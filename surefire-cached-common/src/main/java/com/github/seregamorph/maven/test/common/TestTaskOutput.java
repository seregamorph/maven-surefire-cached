package com.github.seregamorph.maven.test.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

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
    /**
     * Total test classes (counted by number of TEST-*.xml reports)
     */
    private final int totalClasses;
    /*
     Time in seconds from TEST-*.xml reports.
     TODO check why it does not match real test time.
     */
    private final BigDecimal totalTestTimeSeconds;
    private final int totalTests;
    private final int totalErrors;
    private final int totalFailures;
    // alias -> artifact
    private final Map<String, OutputArtifact> artifacts;

    @JsonCreator
    public TestTaskOutput(
        Instant startTime,
        Instant endTime,
        BigDecimal totalTimeSeconds,
        int totalClasses,
        BigDecimal totalTestTimeSeconds,
        int totalTests,
        int totalErrors,
        int totalFailures,
        Map<String, OutputArtifact> artifacts
    ) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalTimeSeconds = totalTimeSeconds;
        this.totalClasses = totalClasses;
        this.totalTestTimeSeconds = totalTestTimeSeconds;
        this.totalTests = totalTests;
        this.totalErrors = totalErrors;
        this.totalFailures = totalFailures;
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

    public int getTotalClasses() {
        return totalClasses;
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

    public Map<String, OutputArtifact> getArtifacts() {
        return artifacts;
    }

    @Override
    public String toString() {
        return "TestTaskOutput[" +
            "startTime=" + startTime + ", " +
            "endTime=" + endTime + ", " +
            "totalTimeSeconds=" + totalTimeSeconds + ", " +
            "totalClasses=" + totalClasses + ", " +
            "totalTestTimeSeconds=" + totalTestTimeSeconds + ", " +
            "totalTests=" + totalTests + ", " +
            "totalErrors=" + totalErrors + ", " +
            "totalFailures=" + totalFailures + ", " +
            "artifacts=" + artifacts + ']';
    }
}
