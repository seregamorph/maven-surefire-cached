package com.github.seregamorph.maven.test.extension;

import com.github.seregamorph.maven.test.common.FlakyFailure;
import com.github.seregamorph.maven.test.common.GroupArtifactId;
import com.github.seregamorph.maven.test.core.TaskOutcome;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author Sergey Chernov
 */
public final class ModuleTestResult {

    private final GroupArtifactId groupArtifactId;
    private final TaskOutcome result;
    private final BigDecimal totalTimeSeconds;
    private final int deletedCacheEntries;
    private final List<FlakyFailure> testcaseFlakyErrors;
    private final List<FlakyFailure> testcaseFlakyFailures;
    private final List<FlakyFailure> testcaseErrors;

    public ModuleTestResult(
        GroupArtifactId groupArtifactId,
        TaskOutcome result,
        BigDecimal totalTimeSeconds,
        int deletedCacheEntries,
        List<FlakyFailure> testcaseFlakyErrors,
        List<FlakyFailure> testcaseFlakyFailures,
        List<FlakyFailure> testcaseErrors
    ) {
        this.groupArtifactId = groupArtifactId;
        this.result = result;
        this.totalTimeSeconds = totalTimeSeconds;
        this.deletedCacheEntries = deletedCacheEntries;
        this.testcaseFlakyErrors = testcaseFlakyErrors;
        this.testcaseFlakyFailures = testcaseFlakyFailures;
        this.testcaseErrors = testcaseErrors;
    }

    public GroupArtifactId getGroupArtifactId() {
        return groupArtifactId;
    }

    public TaskOutcome getResult() {
        return result;
    }

    public BigDecimal getTotalTimeSeconds() {
        return totalTimeSeconds;
    }

    public int getDeletedCacheEntries() {
        return deletedCacheEntries;
    }

    public List<FlakyFailure> getTestcaseFlakyErrors() {
        return testcaseFlakyErrors;
    }

    public List<FlakyFailure> getTestcaseFlakyFailures() {
        return testcaseFlakyFailures;
    }

    public List<FlakyFailure> getTestcaseErrors() {
        return testcaseErrors;
    }

    @Override
    public String toString() {
        return "ModuleTestResult{" +
            "groupArtifactId=" + groupArtifactId +
            ", result=" + result +
            ", totalTimeSeconds=" + totalTimeSeconds +
            ", deletedCacheEntries=" + deletedCacheEntries +
            ", testcaseFlakyErrors=" + testcaseFlakyErrors +
            ", testcaseFlakyFailures=" + testcaseFlakyFailures +
            ", testcaseErrors=" + testcaseErrors +
            '}';
    }
}
