package com.github.seregamorph.maven.test.core;

import com.github.seregamorph.maven.test.common.TestTaskOutput;
import javax.annotation.Nullable;

/**
 * @author Sergey Chernov
 */
public enum TaskOutcome {

    SKIPPED_CACHE("serial time", true) {
        @Override
        public String message(TestTaskOutput testTaskOutput) {
            return null;
        }
    },
    SUCCESS("serial time", true) {
        @Override
        public String message(TestTaskOutput testTaskOutput) {
            return "(" + testTaskOutput.getTotalTests() + " tests"
                + ", " + testTaskOutput.getTotalTestTimeSeconds() + "s)";
        }
    },
    FLAKY("serial time", true) {
        @Override
        public String message(TestTaskOutput testTaskOutput) {
            return "(flaky errors " + testTaskOutput.getTestcaseFlakyErrors().size()
                + ", flaky failures " + testTaskOutput.getTestcaseFlakyFailures().size()
                + ", failures " + testTaskOutput.getTestcaseErrors().size() + ")";
        }
    },
    FAILED("serial time", true) {
        @Override
        public String message(TestTaskOutput testTaskOutput) {
            return "(errors " + testTaskOutput.getTotalErrors()
                + ", failures " + testTaskOutput.getTotalFailures() + ")";
        }
    },
    EMPTY("serial time", false) {
        @Override
        public String message(TestTaskOutput testTaskOutput) {
            return null;
        }
    },
    FROM_CACHE("serial time saved", true) {
        @Override
        public String message(TestTaskOutput testTaskOutput) {
            return "(saved " + testTaskOutput.getTotalTimeSeconds() + "s)";
        }
    };

    private final String suffix;
    private final boolean print;

    TaskOutcome(String suffix, boolean print) {
        this.suffix = suffix;
        this.print = print;
    }

    @Nullable
    public String suffix() {
        return suffix;
    }

    /**
     * Should be printed in the text report
     */
    public boolean isPrint() {
        return print;
    }

    @Nullable
    public abstract String message(TestTaskOutput testTaskOutput);
}
