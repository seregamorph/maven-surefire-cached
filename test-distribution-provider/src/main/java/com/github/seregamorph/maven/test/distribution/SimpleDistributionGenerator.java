package com.github.seregamorph.maven.test.distribution;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple distribution generator which splits original list to N groups.
 */
public class SimpleDistributionGenerator implements DistributionGenerator {

    @Override
    public List<List<String>> generate(List<String> testClasses, int numGroups) {
        if (numGroups < 1) {
            throw new IllegalArgumentException("Number of groups must be greater than 0, passed: " + numGroups);
        }

        int groupSize = testClasses.size() / numGroups;
        int mod = testClasses.size() % numGroups;
        if (mod > 0) {
            groupSize++;
        }

        var result = new ArrayList<List<String>>();
        for (int i = 0; i < numGroups; i++) {
            result.add(testClasses.subList(i * groupSize, Math.min((i + 1) * groupSize, testClasses.size())));
        }
        return result;
    }
}
