package com.github.seregamorph.maven.test.core;

import static com.github.seregamorph.maven.test.common.TestTaskOutput.PROP_SUFFIX_TEST_CACHED_RESULT;
import static com.github.seregamorph.maven.test.common.TestTaskOutput.PROP_SUFFIX_TEST_CACHED_TIME;
import static com.github.seregamorph.maven.test.common.TestTaskOutput.PROP_SUFFIX_TEST_DELETED_ENTRIES;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.github.seregamorph.maven.test.common.CacheEntryKey;
import com.github.seregamorph.maven.test.common.GroupArtifactId;
import com.github.seregamorph.maven.test.common.TaskOutcome;
import com.github.seregamorph.maven.test.common.TestTaskOutput;
import com.github.seregamorph.maven.test.storage.CacheStorage;
import com.github.seregamorph.maven.test.storage.FileCacheStorage;
import com.github.seregamorph.maven.test.storage.HttpCacheStorage;
import com.github.seregamorph.maven.test.util.MoreFileUtils;
import com.github.seregamorph.maven.test.util.ZipUtils;
import java.io.File;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.surefire.AbstractSurefireMojo;

public class CachedTestWrapper {

    private final AbstractSurefireMojo self;
    private final TestTaskCacheHelper testTaskCacheHelper;
    private final CacheStorage cacheStorage;
    private final Set<GroupArtifactId> cacheExcludes;
    private final String pluginName;

    public CachedTestWrapper(
        AbstractSurefireMojo self,
        TestTaskCacheHelper testTaskCacheHelper,
        String cacheStorage,
        @Nullable
        String[] cacheExcludes,
        String pluginName
    ) {
        this.self = self;
        this.testTaskCacheHelper = testTaskCacheHelper;
        this.cacheStorage = createCacheStorage(cacheStorage);
        this.cacheExcludes = cacheExcludes == null ? Set.of() : Stream.of(cacheExcludes)
            .map(GroupArtifactId::fromString)
            .collect(Collectors.toSet());
        this.pluginName = pluginName;
    }

    private static CacheStorage createCacheStorage(String cacheStorage) {
        //noinspection HttpUrlsUsage
        if (cacheStorage.startsWith("http://") || cacheStorage.startsWith("https://")) {
            return new HttpCacheStorage(URI.create(cacheStorage));
        }

        return new FileCacheStorage(new File(cacheStorage));
    }

    private Log getLog() {
        return self.getLog();
    }

    private void setCachedExecution(TaskOutcome result, TestTaskOutput testTaskOutput) {
        self.getProject().getProperties().put(pluginName + PROP_SUFFIX_TEST_CACHED_RESULT, result.name());
        self.getProject().getProperties().put(pluginName + PROP_SUFFIX_TEST_CACHED_TIME,
            testTaskOutput.totalTime().toString());

        var message = result.message(testTaskOutput);
        getLog().info("Cached execution "
            + self.getProject().getGroupId() + ":" + self.getProject().getArtifactId()
            + " " + result + (message == null ? "" : " " + message));
    }

    private void setCachedDeletion(int deleted) {
        if (deleted > 0) {
            self.getProject().getProperties()
                .put(pluginName + PROP_SUFFIX_TEST_DELETED_ENTRIES, Integer.toString(deleted));
        }
    }

    public void execute(MojoDelegate delegate) throws MojoExecutionException, MojoFailureException {
        if (self.isSkip() || self.isSkipTests() || self.isSkipExec()
            || "pom".equals(self.getProject().getPackaging())
            || !self.getProjectBuildDirectory().exists()) {
            delegate.execute();
            return;
        }

        var startTime = Instant.now();

        // todo support jacoco coverage file
        var reportsDirectory = self.getReportsDirectory();
        MoreFileUtils.delete(reportsDirectory);

        var skipCache = isEmptyOrTrue(self.getSession().getSystemProperties().getProperty("skipCache"));
        if (skipCache) {
            delegate.execute();
            var testTaskOutput = getTaskOutput(startTime, Instant.now());
            setCachedExecution(TaskOutcome.SKIPPED_CACHE, testTaskOutput);
            return;
        }

        var taskInputFile = new File(self.getProjectBuildDirectory(), getTaskInputFileName());
        var taskOutputFile = new File(self.getProjectBuildDirectory(), getTaskOutputFileName());
        MoreFileUtils.delete(taskInputFile);
        MoreFileUtils.delete(taskOutputFile);

        // todo include surefire/failsafe plugin name
        var testTaskInput = testTaskCacheHelper.getTestTaskInput(self, cacheExcludes);
        var testTaskInputBytes = JsonSerializers.serialize(testTaskInput);
        getLog().debug(new String(testTaskInputBytes, UTF_8));
        MoreFileUtils.write(taskInputFile, testTaskInputBytes);
        var cacheEntryKey = getLayoutKey(testTaskInput);

        var testTaskOutputBytes = cacheStorage.read(cacheEntryKey, getTaskOutputFileName());
        if (testTaskOutputBytes == null) {
            getLog().info("Cache miss " + cacheEntryKey);
            boolean thrown = true;
            try {
                delegate.execute();
                thrown = false;
            } finally {
                var testTaskOutput = getTaskOutput(startTime, Instant.now());
                MoreFileUtils.write(taskOutputFile, JsonSerializers.serialize(testTaskOutput));
                if (testTaskOutput.totalErrors() > 0 || testTaskOutput.totalFailures() > 0) {
                    getLog().warn("Tests failed, not storing to cache. See " + reportsDirectory);
                    setCachedExecution(TaskOutcome.FAILED, testTaskOutput);
                } else if (!thrown) {
                    getLog().info("Storing reports to cache from " + reportsDirectory);
                    var deleted = storeCache(cacheEntryKey, testTaskInput, testTaskOutput);
                    var result = testTaskOutput.totalTests() == 0 ? TaskOutcome.EMPTY : TaskOutcome.SUCCESS;
                    setCachedExecution(result, testTaskOutput);
                    setCachedDeletion(deleted);
                }
            }
        } else {
            MoreFileUtils.write(taskOutputFile, testTaskOutputBytes);
            getLog().info("Cache hit " + cacheEntryKey);
            var testTaskOutput = JsonSerializers.deserialize(testTaskOutputBytes, TestTaskOutput.class);
            getLog().info("Restoring reports from cache to " + reportsDirectory);
            restoreCache(cacheEntryKey, testTaskOutput);
            setCachedExecution(TaskOutcome.FROM_CACHE, testTaskOutput);
        }
    }

    private int storeCache(CacheEntryKey cacheEntryKey, TestTaskInput testTaskInput, TestTaskOutput testTaskOutput) {
        int deleted = cacheStorage.write(cacheEntryKey, getTaskInputFileName(),
            JsonSerializers.serialize(testTaskInput));
        for (Map.Entry<String, String> entry : testTaskOutput.files().entrySet()) {
            var unpackedName = entry.getKey();
            var packedName = entry.getValue();
            var file = new File(self.getProjectBuildDirectory(), unpackedName);
            var zipFile = new File(self.getProjectBuildDirectory(), packedName);
            MoreFileUtils.delete(zipFile);
            ZipUtils.zipDirectory(file, zipFile);
            deleted += cacheStorage.write(cacheEntryKey, packedName, MoreFileUtils.read(zipFile));
        }
        var testTaskOutputBytes = JsonSerializers.serialize(testTaskOutput);
        deleted += cacheStorage.write(cacheEntryKey, getTaskOutputFileName(), testTaskOutputBytes);
        return deleted;
    }

    private void restoreCache(CacheEntryKey cacheEntryKey, TestTaskOutput testTaskOutput) {
        testTaskOutput.files().forEach((unpackedName, packedName) -> {
            var packedContent = cacheStorage.read(cacheEntryKey, packedName);
            if (packedContent == null) {
                throw new IllegalStateException("Cache file not found " + cacheEntryKey + " " + packedName);
            }

            var zipFile = new File(self.getProjectBuildDirectory(), packedName);
            MoreFileUtils.write(zipFile, packedContent);
            var unpackedFile = new File(self.getProjectBuildDirectory(), unpackedName);
            MoreFileUtils.delete(unpackedFile);
            ZipUtils.unzipDirectory(zipFile, unpackedFile);
        });
    }

    private CacheEntryKey getLayoutKey(TestTaskInput testTaskInput) {
        return new CacheEntryKey(
            pluginName,
            new GroupArtifactId(self.getProject().getGroupId(), self.getProject().getArtifactId()),
            testTaskInput.hash());
    }

    private TestTaskOutput getTaskOutput(Instant startTime, Instant endTime) {
        var testReports = self.getReportsDirectory().listFiles((dir, name) ->
            name.startsWith("TEST-") && name.endsWith(".xml"));

        if (testReports == null) {
            return TestTaskOutput.empty();
        }

        int totalClasses = 0;
        BigDecimal totalTime = BigDecimal.ZERO;
        int totalTests = 0;
        int totalErrors = 0;
        int totalFailures = 0;
        for (var testReport : testReports) {
            var testSuiteSummary = TestSuiteReport.fromFile(testReport);
            totalClasses++;
            totalTime = totalTime.add(testSuiteSummary.time());
            totalTests += testSuiteSummary.tests();
            totalErrors += testSuiteSummary.errors();
            totalFailures += testSuiteSummary.failures();
        }

        var files = new TreeMap<String, String>();
        files.put(self.getReportsDirectory().getName(), getReportsZipName());
        // todo add jacoco coverage file

        return new TestTaskOutput(startTime, endTime,
            totalClasses, totalTime, totalTests, totalErrors, totalFailures, files);
    }

    private String getTaskInputFileName() {
        return pluginName + "-input.json";
    }

    private String getTaskOutputFileName() {
        return pluginName + "-output.json";
    }

    private String getReportsZipName() {
        return self.getReportsDirectory().getName() + ".zip";
    }

    private static boolean isEmptyOrTrue(String value) {
        return "".equals(value) || "true".equals(value);
    }

    public interface MojoDelegate {
        void execute() throws MojoExecutionException, MojoFailureException;
    }
}
