package com.github.seregamorph.maven.test.core;

import java.util.List;
import java.util.Map;

/**
 * Configuration entity per plugin (surefire, failsafe or common) for "surefire-cached.json".
 *
 * @author Sergey Chernov
 */
public class TestPluginConfig {

    static final TestPluginConfig DEFAULT_CONFIG = new TestPluginConfig()
        .setInputProperties(List.of("java.specification.version"))
        .setInputIgnoredProperties(List.of())
        .setExcludeModules(List.of())
        // remove MANIFEST.MF and default maven descriptors with version
        // from the hash to unify hash calculation for jar files and classes directories
        .setExcludeClasspathResources(List.of(
            "META-INF/MANIFEST.MF",
            "META-INF/maven/**/pom.properties",
            "META-INF/maven/**/pom.xml"
        ))
        .setArtifacts(Map.of());

    /**
     * Properties that should be included into cache key calculation.
     * <p>
     * The "env." prefix may be used to resolve environment variables.
     * <p>
     * Default is "java.specification.version"
     */
    private List<String> inputProperties;

    /**
     * Properties that should be excluded from cache key calculation, but included to "surefire-cached-input.json" /
     * "failsafe-cached-input.json" files.
     * <p>
     * The "env." prefix may be used to resolve environment variables.
     * <p>
     * The ignored input properties always include "timestamp".
     * <p>
     * E.g. ["java.version", "os.arch", "os.name", "env.CI", "env.GITHUB_BASE_REF", "env.GITHUB_REF",
     * "env.GITHUB_RUN_ID", "env.GITHUB_JOB", "env.GITHUB_SHA"]
     */
    private List<String> inputIgnoredProperties;

    /**
     * List of "$groupId:$artifactId" for modules that should be excluded from cache key calculation
     */
    private List<String> excludeModules;

    /**
     * List of classpath resources that should be ignored in input hash calculation. Ant wildcard expressions are also
     * supported.
     */
    private List<String> excludeClasspathResources;

    private Map<String, SurefireCachedConfig.ArtifactsConfig> artifacts;

    public TestPluginConfig setInputProperties(List<String> inputProperties) {
        this.inputProperties = inputProperties;
        return this;
    }

    public TestPluginConfig setInputIgnoredProperties(List<String> inputIgnoredProperties) {
        this.inputIgnoredProperties = inputIgnoredProperties;
        return this;
    }

    public TestPluginConfig setExcludeModules(List<String> excludeModules) {
        this.excludeModules = excludeModules;
        return this;
    }

    public TestPluginConfig setExcludeClasspathResources(List<String> excludeClasspathResources) {
        this.excludeClasspathResources = excludeClasspathResources;
        return this;
    }

    public TestPluginConfig setArtifacts(Map<String, SurefireCachedConfig.ArtifactsConfig> artifacts) {
        this.artifacts = artifacts;
        return this;
    }

    public List<String> getInputProperties() {
        return inputProperties;
    }

    public List<String> getInputIgnoredProperties() {
        return inputIgnoredProperties;
    }

    public List<String> getExcludeModules() {
        return excludeModules;
    }

    public List<String> getExcludeClasspathResources() {
        return excludeClasspathResources;
    }

    public Map<String, SurefireCachedConfig.ArtifactsConfig> getArtifacts() {
        return artifacts;
    }

    @Override
    public String toString() {
        return "TestPluginConfig{" +
            "inputProperties=" + inputProperties +
            ", inputIgnoredProperties=" + inputIgnoredProperties +
            ", excludeModules=" + excludeModules +
            ", excludeClasspathResources=" + excludeClasspathResources +
            ", artifacts=" + artifacts +
            '}';
    }
}
