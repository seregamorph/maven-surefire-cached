package com.github.seregamorph.maven.test.extension;

import static com.github.seregamorph.maven.test.extension.SurefireCachedMojo.formatFailures;

import com.github.seregamorph.maven.test.common.FlakyFailure;
import com.github.seregamorph.maven.test.common.PluginName;
import com.github.seregamorph.maven.test.common.TestTaskOutput;
import com.github.seregamorph.maven.test.core.FailsafeSummaryReport;
import com.github.seregamorph.maven.test.core.TestSuiteReport;
import com.github.seregamorph.maven.test.util.ReflectionUtils;
import java.io.File;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                new TreeMap<>());
        }

        FailsafeSummaryReport report = FailsafeSummaryReport.fromFile(summaryFile);
        Map<File, TestSuiteReport> testReports = TestSuiteReport.fromDirectory(reportsDirectory);
        List<FlakyFailure> testcaseFlakyErrors = new ArrayList<>();
        List<FlakyFailure> testcaseFlakyFailures = new ArrayList<>();
        List<FlakyFailure> testcaseErrors = new ArrayList<>();
        for (Map.Entry<File, TestSuiteReport> entry : testReports.entrySet()) {
            TestSuiteReport testSuiteSummary = entry.getValue();
            testcaseFlakyErrors.addAll(formatFailures(testSuiteSummary.testcaseFlakyErrors()));
            testcaseFlakyFailures.addAll(formatFailures(testSuiteSummary.testcaseFlakyFailures()));
            testcaseErrors.addAll(formatFailures(testSuiteSummary.testcaseErrors()));
        }

        if (report.getFlakes() > 0 && testcaseFlakyErrors.isEmpty()) {
            // this can happen e.g. if TEST-*.xml reports are not enabled
            testcaseFlakyErrors.add(new FlakyFailure("failsafe-summary.xml", "flakes"));
        }

        return new TestTaskOutput(startTime, endTime, totalTimeSeconds,
            totalTimeSeconds, report.getCompleted(),
            report.getErrors(), report.getFailures(), report.getFailureMessage(),
            testcaseFlakyErrors, testcaseFlakyFailures, testcaseErrors, new TreeMap<>());
    }
}
