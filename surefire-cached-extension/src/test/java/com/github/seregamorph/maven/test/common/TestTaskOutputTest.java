package com.github.seregamorph.maven.test.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.seregamorph.maven.test.util.JsonSerializers;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TestTaskOutputTest {

    @Test
    public void shouldDeserialize() {
        var testTestOutput = new TestTaskOutput(
            Instant.now().minus(Duration.ofSeconds(10)),
            Instant.now(),
            new BigDecimal("1.1"),
            10,
            new BigDecimal("1.0"),
            100,
            0,
            0,
            Map.of(
                "jacoco",
                new OutputArtifact(
                    "jacoco.exec",
                    1,
                    100,
                    10
                )
            )
        );
        var content = JsonSerializers.serialize(testTestOutput);
        var restored = JsonSerializers.deserialize(content, TestTaskOutput.class, "surefire-cached-output.json");
        assertEquals(testTestOutput.getStartTime(), restored.getStartTime());
        assertEquals(testTestOutput.getEndTime(), restored.getEndTime());
        assertEquals(testTestOutput.getTotalTimeSeconds(), restored.getTotalTimeSeconds());
        assertEquals(testTestOutput.getTotalClasses(), restored.getTotalClasses());
        assertEquals(testTestOutput.getTotalTestTimeSeconds(), restored.getTotalTestTimeSeconds());
        assertEquals(testTestOutput.getTotalTests(), restored.getTotalTests());
        assertEquals(testTestOutput.getTotalErrors(), restored.getTotalErrors());
        assertEquals(testTestOutput.getTotalFailures(), restored.getTotalFailures());
        assertEquals(testTestOutput.getArtifacts().get("jacoco").getFileName(),
            restored.getArtifacts().get("jacoco").getFileName());
        assertEquals(testTestOutput.getArtifacts().get("jacoco").getFiles(),
            restored.getArtifacts().get("jacoco").getFiles());
        assertEquals(testTestOutput.getArtifacts().get("jacoco").getPackedSize(),
            restored.getArtifacts().get("jacoco").getPackedSize());
        assertEquals(testTestOutput.getArtifacts().get("jacoco").getUnpackedSize(),
            restored.getArtifacts().get("jacoco").getUnpackedSize());
    }
}
