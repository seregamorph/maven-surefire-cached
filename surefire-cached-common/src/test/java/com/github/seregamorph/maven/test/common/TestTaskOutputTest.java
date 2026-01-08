package com.github.seregamorph.maven.test.common;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.seregamorph.maven.test.util.JsonSerializers;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

class TestTaskOutputTest {

    @Test
    public void shouldSerializeAndDeserialize() {
        var testTestOutput = new TestTaskOutput(
            Instant.now().minus(Duration.ofSeconds(10)),
            Instant.now(),
            new BigDecimal("1.1"),
            new BigDecimal("1.0"),
            100,
            0,
            0,
            "failure",
            List.of(),
            List.of(),
            List.of(),
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
        assertEquals(testTestOutput.getTotalTestTimeSeconds(), restored.getTotalTestTimeSeconds());
        assertEquals(testTestOutput.getTotalTests(), restored.getTotalTests());
        assertEquals(testTestOutput.getTotalErrors(), restored.getTotalErrors());
        assertEquals(testTestOutput.getTotalFailures(), restored.getTotalFailures());
        assertEquals(testTestOutput.getFailureMessage(), restored.getFailureMessage());
        assertEquals(testTestOutput.getTestcaseFlakyErrors(), restored.getTestcaseFlakyErrors());
        assertEquals(testTestOutput.getTestcaseFlakyFailures(), restored.getTestcaseFlakyFailures());
        assertEquals(testTestOutput.getTestcaseErrors(), restored.getTestcaseErrors());
        assertEquals(testTestOutput.getArtifacts().get("jacoco").getFileName(),
            restored.getArtifacts().get("jacoco").getFileName());
        assertEquals(testTestOutput.getArtifacts().get("jacoco").getFiles(),
            restored.getArtifacts().get("jacoco").getFiles());
        assertEquals(testTestOutput.getArtifacts().get("jacoco").getPackedSize(),
            restored.getArtifacts().get("jacoco").getPackedSize());
        assertEquals(testTestOutput.getArtifacts().get("jacoco").getUnpackedSize(),
            restored.getArtifacts().get("jacoco").getUnpackedSize());
    }

    @Test
    public void shouldDeserializeBackwardCompatibility() {
        @Language("JSON")
        var content = """
            {
              "startTime" : "2025-06-30T17:10:34.721504Z",
              "endTime" : "2025-06-30T17:10:44.722564Z",
              "totalTimeSeconds" : 1.1,
              "totalTestTimeSeconds" : 1.0,
              "totalTests" : 100,
              "totalErrors" : 0,
              "totalFailures" : 0,
              "artifacts" : {
                "jacoco" : {
                  "fileName" : "jacoco.exec",
                  "files" : 1,
                  "unpackedSize" : 100,
                  "packedSize" : 10
                }
              }
            }
            """;
        var restored = JsonSerializers.deserialize(content.getBytes(UTF_8),
            TestTaskOutput.class, "surefire-cached-output.json");
        assertEquals(Instant.parse("2025-06-30T17:10:34.721504Z"), restored.getStartTime());
        assertEquals(Instant.parse("2025-06-30T17:10:44.722564Z"), restored.getEndTime());
        assertEquals(new BigDecimal("1.1"), restored.getTotalTimeSeconds());
        assertEquals(new BigDecimal("1.0"), restored.getTotalTestTimeSeconds());
        assertEquals(100, restored.getTotalTests());
        assertEquals(0, restored.getTotalErrors());
        assertEquals(0, restored.getTotalFailures());
        assertEquals(List.of(), restored.getTestcaseFlakyErrors());
        assertEquals(List.of(), restored.getTestcaseFlakyFailures());
        assertEquals(List.of(), restored.getTestcaseErrors());
        assertEquals("jacoco.exec",
            restored.getArtifacts().get("jacoco").getFileName());
        assertEquals(1,
            restored.getArtifacts().get("jacoco").getFiles());
        assertEquals(10L,
            restored.getArtifacts().get("jacoco").getPackedSize());
        assertEquals(100L,
            restored.getArtifacts().get("jacoco").getUnpackedSize());
    }

    @Test
    public void shouldDeserializeForwardCompatibility() {
        @Language("JSON")
        var content = """
            {
              "startTime" : "2025-06-30T17:10:34.721504Z",
              "endTime" : "2025-06-30T17:10:44.722564Z",
              "totalTimeSeconds" : 1.1,
              "totalTestTimeSeconds" : 1.0,
              "totalTests" : 100,
              "totalErrors" : 0,
              "totalFailures" : 0,
              "testcaseFlakyErrors": [{
                  "testClassName": "flaky",
                  "testName": "failure"
              }],
              "testcaseErrors": [{
                  "testClassName": "flaky",
                  "testName": "error1"
              }, {
                  "testClassName": "flaky",
                  "testName": "error2"
              }],
              "newField": {
                "sub": "value"
              },
              "artifacts" : {
                "jacoco" : {
                  "fileName" : "jacoco.exec",
                  "files" : 1,
                  "unpackedSize" : 100,
                  "packedSize" : 10
                }
              }
            }
            """;
        var restored = JsonSerializers.deserialize(content.getBytes(UTF_8),
            TestTaskOutput.class, "surefire-cached-output.json");
        assertEquals(Instant.parse("2025-06-30T17:10:34.721504Z"), restored.getStartTime());
        assertEquals(Instant.parse("2025-06-30T17:10:44.722564Z"), restored.getEndTime());
        assertEquals(new BigDecimal("1.1"), restored.getTotalTimeSeconds());
        assertEquals(new BigDecimal("1.0"), restored.getTotalTestTimeSeconds());
        assertEquals(100, restored.getTotalTests());
        assertEquals(0, restored.getTotalErrors());
        assertEquals(0, restored.getTotalFailures());
        assertEquals("[FlakyFailure{testClassName='flaky', testName='failure'}]",
            restored.getTestcaseFlakyErrors().toString());
        assertEquals(List.of(), restored.getTestcaseFlakyFailures());
        assertEquals("[FlakyFailure{testClassName='flaky', testName='error1'}, FlakyFailure{testClassName='flaky', testName='error2'}]",
            restored.getTestcaseErrors().toString());
        assertEquals("jacoco.exec",
            restored.getArtifacts().get("jacoco").getFileName());
        assertEquals(1,
            restored.getArtifacts().get("jacoco").getFiles());
        assertEquals(10L,
            restored.getArtifacts().get("jacoco").getPackedSize());
        assertEquals(100L,
            restored.getArtifacts().get("jacoco").getUnpackedSize());
    }
}
