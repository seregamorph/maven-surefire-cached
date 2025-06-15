package com.github.seregamorph.maven.test.extension;

import static com.github.seregamorph.maven.test.util.ByteSizeFormatUtils.formatByteSize;
import static com.github.seregamorph.maven.test.util.TimeFormatUtils.formatTime;
import static com.github.seregamorph.maven.test.util.TimeFormatUtils.toSeconds;

import com.github.seregamorph.maven.test.common.PluginName;
import com.github.seregamorph.maven.test.core.TaskOutcome;
import com.github.seregamorph.maven.test.storage.CacheServiceMetrics;
import com.github.seregamorph.maven.test.storage.CacheStorage;
import com.github.seregamorph.maven.test.storage.HttpCacheStorage;
import com.github.seregamorph.maven.test.util.JsonSerializers;
import com.github.seregamorph.maven.test.util.MoreFileUtils;
import java.io.File;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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

    private static final class AggResult {

        private static final AggResult EMPTY = new AggResult(0, BigDecimal.ZERO);

        private final int totalModules;
        private final BigDecimal totalTimeSec;

        private AggResult(int totalModules, BigDecimal totalTimeSec) {
            this.totalModules = totalModules;
            this.totalTimeSec = totalTimeSec;
        }

        AggResult add(BigDecimal time) {
            return new AggResult(totalModules + 1, totalTimeSec.add(time));
        }

        public int getTotalModules() {
            return totalModules;
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
    public void afterProjectsRead(MavenSession session) {
        this.testTaskCacheHelper.init(session);
    }

    @Override
    public void afterSessionEnd(MavenSession session) {
        CacheReport cacheReport = testTaskCacheHelper.getCacheReport();
        Map<String, Map<TaskOutcome, AggResult>> pluginResults = new TreeMap<>();
        for (PluginName pluginName : Arrays.asList(PluginName.SUREFIRE_CACHED, PluginName.FAILSAFE_CACHED)) {
            Map<TaskOutcome, AggResult> pluginResult = new TreeMap<>();
            int deleted = 0;
            List<CacheReport.ModuleTestResult> executionResults = cacheReport.getExecutionResults(pluginName);
            for (CacheReport.ModuleTestResult executionResult : executionResults) {
                pluginResult.compute(executionResult.getResult(),
                    (k, v) -> (v == null ? AggResult.EMPTY : v).add(executionResult.getTotalTimeSeconds()));
                deleted += executionResult.getDeletedCacheEntries();
            }
            if (!pluginResult.isEmpty()) {
                logger.info("Total test cached results ({}):", pluginName);
                pluginResult.forEach((k, v) -> {
                    if (k.isPrint()) {
                        String suffix = k.suffix();
                        logger.info("{} ({} modules): {}{}", k, v.totalModules,
                            formatTime(v.totalTimeSec), suffix == null ? "" : " " + suffix);
                    }
                });
                if (deleted > 0) {
                    logger.info("Total deleted cache entries: {}", deleted);
                }
                pluginResults.put(pluginName.name(), pluginResult);
            }
        }
        if (isLogStorageMetrics(testTaskCacheHelper.getCacheStorage())) {
            logStorageMetrics(testTaskCacheHelper.getMetrics());
        }
        saveJsonReport(session, pluginResults);
        this.testTaskCacheHelper.destroy();
    }

    private boolean isLogStorageMetrics(CacheStorage cacheStorage) {
        return cacheStorage instanceof HttpCacheStorage;
    }

    private void saveJsonReport(MavenSession session, Map<String, Map<TaskOutcome, AggResult>> pluginResults) {
        JsonCacheReport jsonCacheReport = new JsonCacheReport(pluginResults, testTaskCacheHelper.getMetrics());
        File dir = new File(session.getExecutionRootDirectory(), "target");
        dir.mkdir();
        MoreFileUtils.write(new File(dir, "surefire-cache-report.json"), JsonSerializers.serialize(jsonCacheReport));
    }

    private static final class JsonCacheReport {

        private final Map<String, Map<TaskOutcome, AggResult>> pluginResults;
        private final CacheServiceMetrics cacheServiceMetrics;

        private JsonCacheReport(
            Map<String, Map<TaskOutcome, AggResult>> pluginResults,
            CacheServiceMetrics cacheServiceMetrics) {
            this.pluginResults = pluginResults;
            this.cacheServiceMetrics = cacheServiceMetrics;
        }

        public Map<String, Map<TaskOutcome, AggResult>> getPluginResults() {
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
