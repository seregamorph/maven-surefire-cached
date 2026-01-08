package com.github.seregamorph.maven.test.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * @author Sergey Chernov
 */
@JsonIgnoreProperties(ignoreUnknown = true) // for forward compatibility
public final class TestTaskOutput {

    private final Instant startTime;
    private final Instant endTime;
    /**
     * Time between startTime and endTime of test task
     */
    private final BigDecimal totalTimeSeconds;
    /*
     Time in seconds from TEST-*.xml reports.
     TODO check why it does not match real test time.
     */
    private final BigDecimal totalTestTimeSeconds;
    private final int totalTests;
    private final int totalErrors;
    private final int totalFailures;
    @Nullable
    private final String failureMessage;
    private final List<FlakyFailure> testcaseFlakyErrors;
    private final List<FlakyFailure> testcaseFlakyFailures;
    private final List<FlakyFailure> testcaseErrors;
    // alias -> artifact
    private final Map<String, OutputArtifact> artifacts;

    @JsonCreator
    public TestTaskOutput(
        Instant startTime,
        Instant endTime,
        BigDecimal totalTimeSeconds,
        BigDecimal totalTestTimeSeconds,
        int totalTests,
        int totalErrors,
        int totalFailures,
        @Nullable
        String failureMessage,
        List<FlakyFailure> testcaseFlakyErrors,
        List<FlakyFailure> testcaseFlakyFailures,
        List<FlakyFailure> testcaseErrors,
        Map<String, OutputArtifact> artifacts
    ) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalTimeSeconds = totalTimeSeconds;
        this.totalTestTimeSeconds = totalTestTimeSeconds;
        this.totalTests = totalTests;
        this.totalErrors = totalErrors;
        this.totalFailures = totalFailures;
        this.failureMessage = failureMessage;
        this.testcaseFlakyErrors = testcaseFlakyErrors == null ? Collections.emptyList() : testcaseFlakyErrors;
        this.testcaseFlakyFailures = testcaseFlakyFailures  == null ? Collections.emptyList() : testcaseFlakyFailures;
        this.testcaseErrors = testcaseErrors  == null ? Collections.emptyList() : testcaseErrors;
        this.artifacts = artifacts;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public BigDecimal getTotalTimeSeconds() {
        return totalTimeSeconds;
    }

    public BigDecimal getTotalTestTimeSeconds() {
        return totalTestTimeSeconds;
    }

    public int getTotalTests() {
        return totalTests;
    }

    public int getTotalErrors() {
        return totalErrors;
    }

    public int getTotalFailures() {
        return totalFailures;
    }

    @Nullable
    public String getFailureMessage() {
        return failureMessage;
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

    public Map<String, OutputArtifact> getArtifacts() {
        return artifacts;
    }

    @JsonIgnore
    public boolean hasFailures() {
        return totalErrors > 0 || totalFailures > 0 || failureMessage != null;
    }

    @JsonIgnore
    public boolean hasFlakyFailures() {
        return !testcaseFlakyErrors.isEmpty() || !testcaseFlakyFailures.isEmpty();
    }

    @Override
    public String toString() {
        return "TestTaskOutput{" +
            "startTime=" + startTime +
            ", endTime=" + endTime +
            ", totalTimeSeconds=" + totalTimeSeconds +
            ", totalTestTimeSeconds=" + totalTestTimeSeconds +
            ", totalTests=" + totalTests +
            ", totalErrors=" + totalErrors +
            ", totalFailures=" + totalFailures +
            ", failureMessage='" + failureMessage + '\'' +
            ", testcaseFlakyErrors=" + testcaseFlakyErrors +
            ", testcaseFlakyFailures=" + testcaseFlakyFailures +
            ", testcaseErrors=" + testcaseErrors +
            ", artifacts=" + artifacts +
            '}';
    }
}
