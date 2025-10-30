package com.github.seregamorph.maven.test.extension;

import com.github.seregamorph.maven.test.common.PluginName;
import com.github.seregamorph.maven.test.common.TestTaskOutput;
import com.github.seregamorph.maven.test.core.FailsafeSummaryReport;
import com.github.seregamorph.maven.test.util.ReflectionUtils;
import java.io.File;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.TreeMap;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.project.MavenProject;

/**
 * Delegates to org.apache.maven.plugin.failsafe.IntegrationTestMojo (failsafe:integration-test)
 *
 * @author Sergey Chernov
 */
class IntegrationTestCachedMojo extends AbstractCachedSurefireMojo {

    private final File summaryFile;

    IntegrationTestCachedMojo(
        TestTaskCacheHelper testTaskCacheHelper,
        MavenSession session,
        MavenProject project,
        Mojo delegate
    ) {
        super(testTaskCacheHelper, session, project, delegate, PluginName.FAILSAFE_CACHED);
        this.summaryFile = ReflectionUtils.call(delegate, File.class, "getSummaryFile");
    }

    @Override
    TestTaskOutput getTaskOutput(Instant startTime, Instant endTime) {
        BigDecimal totalTimeSeconds = getTotalTimeSeconds(startTime, endTime);
        if (!summaryFile.exists()) {
            return new TestTaskOutput(startTime, endTime, totalTimeSeconds,
                BigDecimal.ZERO, 0, 0, 0, null,
                0, 0, 0,
                new TreeMap<>());
        }

        FailsafeSummaryReport report = FailsafeSummaryReport.fromFile(summaryFile);
        return new TestTaskOutput(startTime, endTime, totalTimeSeconds,
            totalTimeSeconds, report.getCompleted(), report.getErrors(), report.getFailures(), report.getFailureMessage(),
            report.getFlakes(), 0, 0, new TreeMap<>());
    }
}
