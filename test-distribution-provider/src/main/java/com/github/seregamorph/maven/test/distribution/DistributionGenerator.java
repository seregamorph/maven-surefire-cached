package com.github.seregamorph.maven.test.distribution;

import java.util.List;

public interface DistributionGenerator {

    /**
     * Distribute test classes to groups. Each subgroup should preserve original order of test classes.
     *
     * @param testClasses
     * @param numGroups
     * @return list of size numGroups containing subsets of original testClasses
     */
    List<List<String>> generate(List<String> testClasses, int numGroups);
}
