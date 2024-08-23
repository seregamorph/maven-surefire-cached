package com.github.seregamorph.maven.test.extension;

import static com.github.seregamorph.maven.test.common.TestTaskOutput.PLUGIN_FAILSAFE_CACHED;
import static com.github.seregamorph.maven.test.common.TestTaskOutput.PLUGIN_SUREFIRE_CACHED;
import static com.github.seregamorph.maven.test.common.TestTaskOutput.PROP_SUFFIX_TEST_CACHED_RESULT;
import static com.github.seregamorph.maven.test.common.TestTaskOutput.PROP_SUFFIX_TEST_CACHED_TIME;
import static com.github.seregamorph.maven.test.common.TestTaskOutput.PROP_SUFFIX_TEST_DELETED_ENTRIES;

import com.github.seregamorph.maven.test.common.TaskOutcome;
import com.github.seregamorph.maven.test.util.TimeFormatUtils;
import java.math.BigDecimal;
import java.util.List;
import java.util.TreeMap;
import javax.inject.Named;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.SessionScoped;
import org.apache.maven.execution.MavenSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
Hint: monitor values Dashboard
* Stored entries
* Evicted entries
* Cache hits
* Cache misses
* Cache hit rate
* Data Received
* Data Sent
 */
@SessionScoped
@Named
public class CachedTestLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    private static final Logger logger = LoggerFactory.getLogger(CachedTestLifecycleParticipant.class);

    private record AggResult(int total, BigDecimal totalTime) {
        private static final AggResult EMPTY = new AggResult(0, BigDecimal.ZERO);

        AggResult add(BigDecimal time) {
            return new AggResult(total + 1, totalTime.add(time));
        }
    }

    @Override
    public void afterSessionEnd(MavenSession session) {
        for (var pluginName : List.of(PLUGIN_SUREFIRE_CACHED, PLUGIN_FAILSAFE_CACHED)) {
            var results = new TreeMap<TaskOutcome, AggResult>();
            int deleted = 0;
            for (var project : session.getProjects()) {
                var cachedResult = project.getProperties().getProperty(pluginName + PROP_SUFFIX_TEST_CACHED_RESULT);
                if (cachedResult != null) {
                    var cachedTime = new BigDecimal(project.getProperties().getProperty(pluginName + PROP_SUFFIX_TEST_CACHED_TIME));
                    results.compute(TaskOutcome.valueOf(cachedResult),
                        (k, v) -> (v == null ? AggResult.EMPTY : v).add(cachedTime));
                    var deletedStr = project.getProperties().getProperty(pluginName + PROP_SUFFIX_TEST_DELETED_ENTRIES);
                    if (deletedStr != null) {
                        deleted += Integer.parseInt(deletedStr);
                    }
                }
            }
            if (!results.isEmpty()) {
                logger.info("Total test cached results ({}):", pluginName);
                results.forEach((k, v) -> {
                    var suffix = k.suffix();
                    logger.info("{} ({} modules): {}{}", k, v.total,
                        TimeFormatUtils.formatTime(v.totalTime), suffix == null ? "" : " " + suffix);
                });
                if (deleted > 0) {
                    logger.info("Total deleted cache entries: {}", deleted);
                }
                logger.info("");
            }
        }
    }
}
