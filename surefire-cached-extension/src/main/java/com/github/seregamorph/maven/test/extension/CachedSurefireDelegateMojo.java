package com.github.seregamorph.maven.test.extension;

import static com.github.seregamorph.maven.test.util.MavenPropertyUtils.getProperty;
import static com.github.seregamorph.maven.test.util.MavenPropertyUtils.isTrue;
import static com.github.seregamorph.maven.test.util.ReflectionUtils.call;
import static com.github.seregamorph.maven.test.util.ReflectionUtils.callProtected;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.github.seregamorph.maven.test.common.CacheEntryKey;
import com.github.seregamorph.maven.test.common.GroupArtifactId;
import com.github.seregamorph.maven.test.common.OutputArtifact;
import com.github.seregamorph.maven.test.common.PluginName;
import com.github.seregamorph.maven.test.common.TestTaskOutput;
import com.github.seregamorph.maven.test.config.ArtifactsConfig;
import com.github.seregamorph.maven.test.config.TestPluginConfig;
import com.github.seregamorph.maven.test.config.TestPluginConfigLoader;
import com.github.seregamorph.maven.test.core.TaskOutcome;
import com.github.seregamorph.maven.test.core.TestSuiteReport;
import com.github.seregamorph.maven.test.core.TestTaskInput;
import com.github.seregamorph.maven.test.storage.CacheService;
import com.github.seregamorph.maven.test.util.JsonSerializers;
import com.github.seregamorph.maven.test.util.MoreFileUtils;
import com.github.seregamorph.maven.test.util.TimeFormatUtils;
import com.github.seregamorph.maven.test.util.ZipUtils;
import java.io.File;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

/**
 * @author Sergey Chernov
 */
public class CachedSurefireDelegateMojo extends AbstractMojo {

    private final TestTaskCacheHelper testTaskCacheHelper;
    private final CacheService cacheService;
    private final CacheReport cacheReport;
    private final MavenSession session;
    private final MavenProject project;
    private final Mojo delegate;
    private final PluginName pluginName;

    private final Log log;
    private final File projectBuildDirectory;
    private final File reportsDirectory;

    public CachedSurefireDelegateMojo(
        TestTaskCacheHelper testTaskCacheHelper,
        CacheService cacheService,
        CacheReport cacheReport,
        MavenSession session,
        MavenProject project,
        Mojo delegate,
        PluginName pluginName
    ) {
        this.testTaskCacheHelper = testTaskCacheHelper;
        this.cacheService = cacheService;
        this.cacheReport = cacheReport;
        this.session = session;
        this.project = project;
        this.delegate = delegate;
        this.pluginName = pluginName;

        this.log = delegate.getLog();
        // "target" subdir of basedir
        this.projectBuildDirectory = new File(project.getBuild().getDirectory());
        // "target/surefire-reports" or "target/failsafe-reports"
        this.reportsDirectory = call(delegate, File.class, "getReportsDirectory");
    }

    private void reportCachedExecution(TaskOutcome result, TestTaskOutput testTaskOutput) {
        reportCachedExecution(result, testTaskOutput, 0);
    }

    private void reportCachedExecution(TaskOutcome result, TestTaskOutput testTaskOutput, int deletedCacheEntries) {
        cacheReport.addExecutionResult(getGroupArtifactId(project), pluginName,
            result, testTaskOutput.getTotalTimeSeconds(), deletedCacheEntries);

        String message = result.message(testTaskOutput);
        log.info("Cached execution "
            + project.getGroupId() + ":" + project.getArtifactId()
            + " " + result + (message == null ? "" : " " + message));
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (callProtected(this.delegate, Boolean.class, "isSkipExecution")
            || "pom".equals(project.getPackaging())
            || !projectBuildDirectory.exists()) {
            delegate.execute();
            return;
        }

        Instant startTime = Instant.now();

        MoreFileUtils.delete(reportsDirectory);

        boolean skipCache = isTrue(getProperty(session, project, "skipCache"));
        if (skipCache) {
            log.info("Skipping cache for " + project.getGroupId() + ":" + project.getArtifactId());
            delegate.execute();
            TestTaskOutput testTaskOutput = getTaskOutput(startTime, Instant.now());
            reportCachedExecution(TaskOutcome.SKIPPED_CACHE, testTaskOutput);
            return;
        }

        TestPluginConfig testPluginConfig = loadEffectiveTestPluginConfig(pluginName);
        log.debug("Effective test plugin config " + testPluginConfig);

        File taskInputFile = new File(projectBuildDirectory, getTaskInputFileName());
        File taskOutputFile = new File(projectBuildDirectory, getTaskOutputFileName());
        MoreFileUtils.delete(taskInputFile);
        MoreFileUtils.delete(taskOutputFile);

        TestTaskInput testTaskInput = testTaskCacheHelper.getTestTaskInput(session, project, this.delegate, testPluginConfig);
        String hash = testTaskInput.hash();
        testTaskInput.setHash(hash);
        byte[] testTaskInputBytes = JsonSerializers.serialize(testTaskInput);
        log.debug(new String(testTaskInputBytes, UTF_8));
        MoreFileUtils.write(taskInputFile, testTaskInputBytes);
        CacheEntryKey cacheEntryKey = new CacheEntryKey(pluginName, getGroupArtifactId(project), hash);

        // TODO calculate cache operation time (hash, load, store, etc.)
        Instant entryCalculatedTime = Instant.now();
        log.debug("Cache entry calculated in " + Duration.between(startTime, entryCalculatedTime));

        boolean testClassesEmpty = Optional.ofNullable(testTaskInput.getTestClassesHashes())
            .map(Map::isEmpty).orElse(true);
        // try to restore from cache only if there are test classes
        boolean restoredFromCache = !testClassesEmpty && restoreFromCache(cacheEntryKey, taskOutputFile);
        if (restoredFromCache) {
            return;
        }

        if (testClassesEmpty) {
            log.info("Not test classes found");
        } else {
            log.info("Cache miss " + cacheEntryKey);
        }
        boolean success = false;
        try {
            delegate.execute();
            success = true;
        } catch (Error e) {
            // e.g. OutOfMemoryError
            log.error("Test execution failed", e);
            throw e;
        } finally {
            TestTaskOutput testTaskOutput = getTaskOutput(startTime, Instant.now());
            MoreFileUtils.write(taskOutputFile, JsonSerializers.serialize(testTaskOutput));
            // note that failsafe plugin does not throw exceptions on test failures
            if (testTaskOutput.getTotalErrors() > 0 || testTaskOutput.getTotalFailures() > 0) {
                log.warn("Tests failed, not storing to cache. See " + reportsDirectory);
                reportCachedExecution(TaskOutcome.FAILED, testTaskOutput);
            } else if (success) {
                if (testTaskOutput.getTotalTests() == 0) {
                    log.info("No tests found, not storing to cache");
                    reportCachedExecution(TaskOutcome.EMPTY, testTaskOutput);
                } else {
                    log.info("Storing artifacts to cache from " + projectBuildDirectory);
                    int deleted = storeCache(testPluginConfig, cacheEntryKey, testTaskInput, testTaskOutput);
                    reportCachedExecution(TaskOutcome.SUCCESS, testTaskOutput, deleted);
                }
            }
        }
    }

    private boolean restoreFromCache(CacheEntryKey cacheEntryKey, File taskOutputFile) {
        byte[] testTaskOutputBytes = cacheService.read(cacheEntryKey, getTaskOutputFileName());
        if (testTaskOutputBytes == null) {
            return false;
        }

        MoreFileUtils.write(taskOutputFile, testTaskOutputBytes);
        log.info("Cache hit " + cacheEntryKey);
        TestTaskOutput testTaskOutput = JsonSerializers.deserialize(testTaskOutputBytes, TestTaskOutput.class,
            getTaskOutputFileName());
        log.info("Restoring artifacts from cache to " + projectBuildDirectory);

        try {
            restoreCache(cacheEntryKey, testTaskOutput);
            reportCachedExecution(TaskOutcome.FROM_CACHE, testTaskOutput);
            return true;
        } catch (InconsistentCacheException e) {
            // failover to standard execution
            log.warn(e.getMessage());
        }
        return false;
    }

    TestPluginConfig loadEffectiveTestPluginConfig(PluginName pluginName) {
        return TestPluginConfigLoader.loadEffectiveTestPluginConfig(project, pluginName);
    }

    private int storeCache(
        TestPluginConfig testPluginConfig,
        CacheEntryKey cacheEntryKey,
        TestTaskInput testTaskInput,
        TestTaskOutput testTaskOutput
    ) {
        int deleted = cacheService.write(cacheEntryKey, getTaskInputFileName(),
            JsonSerializers.serialize(testTaskInput));
        for (Map.Entry<String, ArtifactsConfig> entry : testPluginConfig.getArtifacts().entrySet()) {
            String alias = entry.getKey();
            ArtifactsConfig artifactsConfig = entry.getValue();
            String fileName = getArtifactPackName(alias);
            File packFile = new File(projectBuildDirectory, fileName);
            MoreFileUtils.delete(packFile);
            List<ZipUtils.PackedFile> packedFiles = ZipUtils.packDirectory(projectBuildDirectory, artifactsConfig.getIncludes(), packFile);
            deleted += cacheService.write(cacheEntryKey, fileName, MoreFileUtils.read(packFile));
            long unpackedSize = packedFiles.stream().mapToLong(ZipUtils.PackedFile::unpackedSize).sum();
            OutputArtifact outputArtifact = new OutputArtifact(fileName, packedFiles.size(),
                unpackedSize, packFile.length());
            testTaskOutput.getArtifacts().put(alias, outputArtifact);
        }
        byte[] testTaskOutputBytes = JsonSerializers.serialize(testTaskOutput);
        deleted += cacheService.write(cacheEntryKey, getTaskOutputFileName(), testTaskOutputBytes);
        return deleted;
    }

    private void restoreCache(CacheEntryKey cacheEntryKey, TestTaskOutput testTaskOutput) throws InconsistentCacheException {
        for (Map.Entry<String, OutputArtifact> entry : testTaskOutput.getArtifacts().entrySet()) {
            String fileName = entry.getValue().getFileName();
            byte[] packedContent = cacheService.read(cacheEntryKey, fileName);
            if (packedContent == null) {
                throw new InconsistentCacheException("Cache file not found " + cacheEntryKey + " " + fileName);
            }

            File packFile = new File(projectBuildDirectory, fileName);
            MoreFileUtils.write(packFile, packedContent);
            ZipUtils.unpackDirectory(packFile, projectBuildDirectory);
            MoreFileUtils.delete(packFile);
        }
    }

    private TestTaskOutput getTaskOutput(
        Instant startTime,
        Instant endTime
    ) {
        File[] testReports = reportsDirectory.listFiles((dir, name) ->
            name.startsWith("TEST-") && name.endsWith(".xml"));

        if (testReports == null) {
            testReports = new File[0];
        }

        int totalClasses = 0;
        BigDecimal totalTestTimeSeconds = BigDecimal.ZERO;
        int totalTests = 0;
        int totalErrors = 0;
        int totalFailures = 0;
        for (File testReport : testReports) {
            TestSuiteReport testSuiteSummary = TestSuiteReport.fromFile(testReport);
            totalClasses++;
            totalTestTimeSeconds = totalTestTimeSeconds.add(testSuiteSummary.timeSeconds());
            totalTests += testSuiteSummary.tests();
            totalErrors += testSuiteSummary.errors();
            totalFailures += testSuiteSummary.failures();
            if (testSuiteSummary.errors() > 0 || testSuiteSummary.failures() > 0) {
                log.warn(testReport + " has errors or failures, skipping cache");
            }
        }

        // artifacts are filled before saving
        Map<String, OutputArtifact> artifacts = new TreeMap<>();
        return new TestTaskOutput(startTime, endTime, getTotalTimeSeconds(startTime, endTime),
            totalClasses, totalTestTimeSeconds, totalTests, totalErrors, totalFailures, artifacts);
    }

    private static String getArtifactPackName(String alias) {
        return alias + ".tar.gz";
    }

    private static BigDecimal getTotalTimeSeconds(Instant startTime, Instant endTime) {
        Duration duration = Duration.between(startTime, endTime);
        long durationMillis = duration.toMillis();
        return TimeFormatUtils.toSeconds(durationMillis);
    }

    private String getTaskInputFileName() {
        return pluginName + "-input.json";
    }

    private String getTaskOutputFileName() {
        return pluginName + "-output.json";
    }

    private static GroupArtifactId getGroupArtifactId(MavenProject project) {
        return new GroupArtifactId(project.getGroupId(), project.getArtifactId());
    }
}
