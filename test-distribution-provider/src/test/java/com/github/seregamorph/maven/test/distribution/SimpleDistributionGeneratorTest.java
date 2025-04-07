package com.github.seregamorph.maven.test.distribution;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class SimpleDistributionGeneratorTest {

    private static final DistributionGenerator distributionGenerator = new SimpleDistributionGenerator();

    @Test
    public void shouldGenerate() {
        assertEquals(List.of(List.of()), distributionGenerator.generate(List.of(), 1));
        assertEquals(List.of(List.of(), List.of()), distributionGenerator.generate(List.of(), 2));
        assertEquals(List.of(List.of("test1"), List.of()), distributionGenerator.generate(List.of("test1"), 2));
        assertEquals(List.of(List.of("test1"), List.of("test2")), distributionGenerator.generate(List.of("test1", "test2"), 2));
        assertEquals(List.of(List.of("test1", "test2"), List.of("test3")), distributionGenerator.generate(List.of("test1", "test2", "test3"), 2));
        assertEquals(List.of(List.of("test1"), List.of("test2"), List.of("test3")), distributionGenerator.generate(List.of("test1", "test2", "test3"), 3));
        assertEquals(List.of(List.of("test1"), List.of("test2"), List.of("test3"), List.of()), distributionGenerator.generate(List.of("test1", "test2", "test3"), 4));
    }
}
