package com.github.seregamorph.maven.test.extension;

import static com.github.seregamorph.maven.test.util.ByteSizeFormatUtils.formatByteSize;
import static com.github.seregamorph.maven.test.util.TimeFormatUtils.formatTime;
import static com.github.seregamorph.maven.test.util.TimeFormatUtils.toSeconds;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.seregamorph.maven.test.common.FlakyFailure;
import com.github.seregamorph.maven.test.common.GroupArtifactId;
import com.github.seregamorph.maven.test.common.PluginName;
import com.github.seregamorph.maven.test.core.TaskOutcome;
import com.github.seregamorph.maven.test.storage.CacheServiceMetrics;
import com.github.seregamorph.maven.test.storage.CacheStorage;
import com.github.seregamorph.maven.test.storage.HttpCacheStorage;
import com.github.seregamorph.maven.test.storage.S3CacheStorage;
import com.github.seregamorph.maven.test.util.JsonSerializers;
import com.github.seregamorph.maven.test.util.MoreFileUtils;
import java.io.File;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.SessionScoped;
import org.apache.maven.execution.MavenSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sergey Chernov
 */
@SessionScoped
@Named
public class CachedTestLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    private static final Logger logger = LoggerFactory.getLogger(CachedTestLifecycleParticipant.class);

    private static class ModuleResult {

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private final SortedSet<String> flakyTests = new TreeSet<>();

        public SortedSet<String> getFlakyTests() {
            return flakyTests;
        }
    }

    private static class AggResult {

        private final SortedMap<GroupArtifactId, ModuleResult> modules = new TreeMap<>();

        private BigDecimal totalTimeSec = BigDecimal.ZERO;

        void add(GroupArtifactId groupArtifactId, ModuleTestResult moduleTestResult) {
            totalTimeSec = totalTimeSec.add(moduleTestResult.getTotalTimeSeconds());
            ModuleResult moduleResult = new ModuleResult();
            moduleResult.flakyTests.addAll(formatFlakyFailures(moduleTestResult.getTestcaseFlakyErrors()));
            moduleResult.flakyTests.addAll(formatFlakyFailures(moduleTestResult.getTestcaseFlakyFailures()));
            moduleResult.flakyTests.addAll(formatFlakyFailures(moduleTestResult.getTestcaseErrors()));
            modules.put(groupArtifactId, moduleResult);
        }

        private static List<String> formatFlakyFailures(List<FlakyFailure> flakyFailures) {
            return flakyFailures.stream()
                .map(ff -> ff.getTestClassName() + "#" + ff.getTestName())
                .collect(Collectors.toList());
        }

        public SortedMap<GroupArtifactId, ModuleResult> getModules() {
            return modules;
        }

        public int getTotalModules() {
            return modules.size();
        }

        public BigDecimal getTotalTimeSec() {
            return totalTimeSec;
        }
    }

    private final TestTaskCacheHelper testTaskCacheHelper;

    @Inject
    public CachedTestLifecycleParticipant(TestTaskCacheHelper testTaskCacheHelper) {
        this.testTaskCacheHelper = testTaskCacheHelper;
    }

    @Override
    public void afterSessionStart(MavenSession session) {
        // Implementation notice: this method is called once in CLI execution, once in Maven execution via IDEA
        // (specifies "idea.version" property) and once in IDEA import (IDEA also specifies "idea.version" and
        // "idea.maven.embedder.version" properties)
        this.testTaskCacheHelper.initSession(session);
    }

    @Override
    public void afterProjectsRead(MavenSession session) {
        // Implementation notice: this method is called once in CLI execution with the whole set of modules,
        // same for run Maven from IDEA (specifies "idea.version" property), but
        // runs once PER EACH module during the IDEA import
        this.testTaskCacheHelper.initProjects(session);
    }

    @Override
    public void afterSessionEnd(MavenSession session) {
        // Implementation notice: this method is called once in CLI execution, same while running Maven from IDEA
        // and never during the IDEA reimport
        if (testTaskCacheHelper.wasPluginManagerInstantiated()) {
            CacheReport cacheReport = testTaskCacheHelper.getCacheReport();
            Map<PluginName, Map<TaskOutcome, AggResult>> pluginResults = handleReport(cacheReport);
            if (isLogStorageMetrics(testTaskCacheHelper.getCacheStorage())) {
                logStorageMetrics(testTaskCacheHelper.getMetrics());
            }
            saveJsonReport(session, pluginResults);
        } else {
            // See issue #23 for more details
            File extensionXmlFile = new File(session.getExecutionRootDirectory(), ".mvn/extensions.xml");
            logger.warn("The surefire-cached-extension is not properly set up (hence not working).\n"
                + "Please add extension to {} (but not the pom.xml)", extensionXmlFile.getAbsolutePath());
        }
        this.testTaskCacheHelper.destroy();
    }

    private Map<PluginName, Map<TaskOutcome, AggResult>> handleReport(CacheReport cacheReport) {
        Map<PluginName, Map<TaskOutcome, AggResult>> pluginResults = new TreeMap<>();
        for (PluginName pluginName : Arrays.asList(PluginName.SUREFIRE_CACHED, PluginName.FAILSAFE_CACHED)) {
            Map<TaskOutcome, AggResult> pluginResult = new TreeMap<>();
            int deleted = 0;
            Map<GroupArtifactId, List<ModuleTestResult>> pluginExecutionResults =
                cacheReport.getExecutionResults(pluginName);
            for (Map.Entry<GroupArtifactId, List<ModuleTestResult>> entry : pluginExecutionResults.entrySet()) {
                GroupArtifactId groupArtifactId = entry.getKey();
                List<ModuleTestResult> moduleTestResults = entry.getValue();
                for (ModuleTestResult executionResult : moduleTestResults) {
                    pluginResult.computeIfAbsent(executionResult.getResult(), $ -> new AggResult())
                        .add(groupArtifactId, executionResult);
                    deleted += executionResult.getDeletedCacheEntries();
                }
            }
            if (!pluginResult.isEmpty()) {
                AtomicBoolean headerPrinted = new AtomicBoolean(false);
                pluginResult.forEach((k, v) -> {
                    if (k.isPrint()) {
                        String suffix = k.suffix();
                        if (headerPrinted.compareAndSet(false, true)) {
                            // print only if needed, but not more than once
                            logger.info("Total test cached results ({}):", pluginName);
                        }
                        logger.info("{} ({} modules): {}{}", k, v.getTotalModules(),
                            formatTime(v.totalTimeSec), suffix == null ? "" : " " + suffix);
                    }
                });
                if (deleted > 0) {
                    logger.info("Total deleted cache entries: {}", deleted);
                }
                pluginResults.put(pluginName, pluginResult);
            }
        }
        return pluginResults;
    }

    private boolean isLogStorageMetrics(CacheStorage cacheStorage) {
        return cacheStorage instanceof HttpCacheStorage
            || cacheStorage instanceof S3CacheStorage;
    }

    private void saveJsonReport(MavenSession session, Map<PluginName, Map<TaskOutcome, AggResult>> pluginResults) {
        JsonCacheReport jsonCacheReport = new JsonCacheReport(pluginResults, testTaskCacheHelper.getMetrics());
        File dir = new File(session.getExecutionRootDirectory(), "target");
        dir.mkdir();
        MoreFileUtils.write(new File(dir, "surefire-cached-report.json"), JsonSerializers.serialize(jsonCacheReport));
    }

    private static final class JsonCacheReport {

        private final Map<PluginName, Map<TaskOutcome, AggResult>> pluginResults;
        private final CacheServiceMetrics cacheServiceMetrics;

        private JsonCacheReport(
            Map<PluginName, Map<TaskOutcome, AggResult>> pluginResults,
            CacheServiceMetrics cacheServiceMetrics) {
            this.pluginResults = pluginResults;
            this.cacheServiceMetrics = cacheServiceMetrics;
        }

        public Map<PluginName, Map<TaskOutcome, AggResult>> getPluginResults() {
            return pluginResults;
        }

        public CacheServiceMetrics getCacheServiceMetrics() {
            return cacheServiceMetrics;
        }

        @Override
        public String toString() {
            return "JsonCacheReport[" +
                "pluginResults=" + pluginResults + ", " +
                "cacheServiceMetrics=" + cacheServiceMetrics + ']';
        }
    }

    private static void logStorageMetrics(CacheServiceMetrics metrics) {
        int readHitOps = metrics.getReadHitOperations();
        long readHitMillis = metrics.getReadHitMillis();
        long readHitBytes = metrics.getReadHitBytes();
        if (readHitOps > 0) {
            logger.info("Cache hit read operations: {}, time: {}, size: {}",
                readHitOps, formatTime(toSeconds(readHitMillis)), formatByteSize(readHitBytes));
        }

        int readMissOps = metrics.getReadMissOperations();
        long readMissMillis = metrics.getReadMissMillis();
        if (readMissOps > 0) {
            logger.info("Cache miss read operations: {}, time: {}",
                readMissOps, formatTime(toSeconds(readMissMillis)));
        }

        int writeOps = metrics.getWriteOperations();
        long writeMillis = metrics.getWriteMillis();
        long writeBytes = metrics.getWriteBytes();
        if (writeOps > 0) {
            logger.info("Cache write operations: {}, time: {}, size: {}",
                writeOps, formatTime(toSeconds(writeMillis)), formatByteSize(writeBytes));
        }

        int readFailures = metrics.getReadFailures();
        if (readFailures != 0) {
            logger.warn("Read failures: {}, then skipped {} operations", readFailures, metrics.getReadSkipped());
        }
        int writeFailures = metrics.getWriteFailures();
        if (writeFailures != 0) {
            logger.warn("Write failures: {}, then skipped {} operations", writeFailures, metrics.getWriteSkipped());
        }
    }
}
