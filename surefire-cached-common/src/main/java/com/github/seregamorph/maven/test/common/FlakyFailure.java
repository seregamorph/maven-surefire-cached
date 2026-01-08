package com.github.seregamorph.maven.test.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Sergey Chernov
 */
@JsonIgnoreProperties(ignoreUnknown = true) // for forward compatibility
public class FlakyFailure {

    private final String testClassName;
    private final String testName;

    @JsonCreator
    public FlakyFailure(String testClassName, String testName) {
        this.testClassName = testClassName;
        this.testName = testName;
    }

    public String getTestClassName() {
        return testClassName;
    }

    public String getTestName() {
        return testName;
    }

    @Override
    public String toString() {
        return "FlakyFailure{" +
            "testClassName='" + testClassName + '\'' +
            ", testName='" + testName + '\'' +
            '}';
    }
}
