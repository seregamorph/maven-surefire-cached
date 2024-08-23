package com.github.seregamorph.maven.test.common;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

public record TestTaskOutput(
    Instant beginTime,
    Instant endTime,
    int totalClasses,
    BigDecimal totalTime,
    int totalTests,
    int totalErrors,
    int totalFailures,
    // unpacked source name -> packed target name
    Map<String, String> files
) {
    public static TestTaskOutput empty() {
        return new TestTaskOutput(
            Instant.now(), Instant.now(),
            0, BigDecimal.ZERO,
            0, 0, 0, Collections.emptyMap());
    }

    public static final String PROP_SUFFIX_TEST_CACHED_RESULT = "_test-cached-result";
    public static final String PROP_SUFFIX_TEST_CACHED_TIME = "_test-cached-time";
    public static final String PROP_SUFFIX_TEST_DELETED_ENTRIES = "_test-deleted-entries";

    public static final String PLUGIN_SUREFIRE_CACHED = "surefire-cached";
    public static final String PLUGIN_FAILSAFE_CACHED = "failsafe-cached";
}
