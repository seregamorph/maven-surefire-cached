package com.github.seregamorph.maven.test.core;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.github.seregamorph.maven.test.common.GroupArtifactId;
import com.github.seregamorph.maven.test.config.ArtifactsConfig;
import com.github.seregamorph.maven.test.util.HashUtils;
import com.github.seregamorph.maven.test.util.JsonSerializers;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.annotation.Nullable;

/**
 * @author Sergey Chernov
 */
@JsonPropertyOrder({
    "hash",
    "version",
    "moduleName",
    "timestamp",
    "argLine",
    "test",
    "ignoredProperties",
    "properties",
    "pluginArtifactHashes",
    "classesHashes",
    "testClassesHashes",
    "moduleArtifactHashes",
    "libraryArtifactHashes",
    "excludeClasspathResources",
    "artifactConfigs",
    "groups",
    "excludedGroups",
    "excludes"
})
public final class TestTaskInput {

    /**
     * Updated on each breaking change
     */
    private static final int VERSION = 2;

    private String hash;

    private final SortedMap<String, String> ignoredProperties = new TreeMap<>();

    private final SortedMap<String, String> properties = new TreeMap<>();

    /**
     * "$groupId:$artifactId[:$classifier]:$version" (optional classifier) -> file hash
     * <p>
     * Note: artifactName is not included in hash, only file hash with classpath sensitivity (ignore timestamp)
     */
    private final SortedMap<String, String> pluginArtifactHashes = new TreeMap<>();

    /**
     * "$groupId:$artifactId" (no version and no classifier) -> file hash
     * <p>
     * Note: artifactName is not included in hash, only file hash with classpath sensitivity (ignore timestamp)
     */
    private final SortedMap<String, String> moduleArtifactHashes = new TreeMap<>();

    /**
     * "$groupId:$artifactId[:$classifier]:$version" (optional classifier) -> file hash
     * <p>
     * Note: artifactName is not included in hash, only file hash with classpath sensitivity (ignore timestamp)
     */
    private final SortedMap<String, String> libraryArtifactHashes = new TreeMap<>();

    // "$groupId:$artifactId", not included in hash
    private String moduleName;
    @Nullable
    private SortedMap<String, String> classesHashes;
    @Nullable
    private SortedMap<String, String> testClassesHashes;
    private List<String> activeProfiles;
    private String argLine;
    private String test;
    // ignore in hash calculation as it's already affecting it
    private List<String> excludeClasspathResources;
    private Map<String, ArtifactsConfig> artifactConfigs;
    private String groups;
    private String excludedGroups;
    private List<String> excludes;

    @JsonIgnore
    public String hash() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        pw.println("# Version");
        pw.println(getVersion());
        pw.println("# Properties");
        properties.forEach((key, value) -> pw.println(key + " -> " + value));
        pw.println("# Plugins");
        pluginArtifactHashes.forEach((key, value) -> pw.println(key + " -> " + value));
        pw.println("# Dependencies");
        Set<Object> artifactHashes = new TreeSet<>();
        artifactHashes.addAll(moduleArtifactHashes.values());
        artifactHashes.addAll(libraryArtifactHashes.values());
        artifactHashes.forEach(hash -> {
            if (!HashUtils.HASH_EMPTY_FILE_COLLECTION.equals(hash)) {
                pw.println(hash);
            }
        });
        if (classesHashes != null) {
            pw.println("# Classes");
            classesHashes.forEach((key, value) -> pw.println(key + " -> " + value));
        }
        if (testClassesHashes != null) {
            pw.println("# Test classes");
            testClassesHashes.forEach((key, value) -> pw.println(key + " -> " + value));
        }
        pw.println("# Profiles");
        pw.println(activeProfiles);
        pw.println("# Arg line");
        pw.println(argLine);
        pw.println("# Test");
        pw.println(test);
        pw.println("# Artifact configs");
        pw.println(JsonSerializers.serializeAsString(artifactConfigs));
        pw.println("# Groups");
        pw.println(groups);
        pw.println("# Excluded groups");
        pw.println(excludedGroups);
        pw.println("# Excludes");
        pw.println(excludes);
        return HashUtils.hashArray(sw.toString().getBytes(UTF_8));
    }

    public void addPluginArtifactHash(
        GroupArtifactId groupArtifactId,
        @Nullable String classifier,
        String version,
        @Nullable String hash
    ) {
        String key = groupArtifactId.toString();
        if (classifier != null && !classifier.isEmpty()) {
            key += ":" + classifier;
        }
        key += ":" + version;
        if (pluginArtifactHashes.containsKey(key)) {
            throw new IllegalStateException("Duplicate plugin classpath entry: " + key);
        }
        pluginArtifactHashes.put(key, hash);
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public void addModuleArtifactHash(String key, String hash) {
        if (moduleArtifactHashes.put(key, hash) != null) {
            throw new IllegalStateException("Duplicate classpath entry: " + key);
        }
    }

    public void addLibraryArtifactHash(
        GroupArtifactId groupArtifactId,
        @Nullable String classifier,
        String version,
        String hash
    ) {
        String key = groupArtifactId.toString();
        if (classifier != null && !classifier.isEmpty()) {
            key += ":" + classifier;
        }
        key += ":" + version;
        if (libraryArtifactHashes.put(key, hash) != null) {
            throw new IllegalStateException("Duplicate classpath entry: " + key);
        }
    }

    public void setClassesHashes(SortedMap<String, String> classesHashes) {
        this.classesHashes = classesHashes;
    }

    public void setTestClassesHashes(SortedMap<String, String> testClassesHashes) {
        this.testClassesHashes = testClassesHashes;
    }

    public void addIgnoredProperty(String key, String value) {
        ignoredProperties.put(key, value);
    }

    public void addProperty(String key, String value) {
        properties.put(key, value);
    }

    public void setActiveProfiles(List<String> activeProfiles) {
        this.activeProfiles = activeProfiles;
    }

    public void setArgLine(String argLine) {
        this.argLine = argLine;
    }

    public void setTest(String test) {
        this.test = test;
    }

    public void setExcludeClasspathResources(List<String> excludeClasspathResources) {
        this.excludeClasspathResources = excludeClasspathResources;
    }

    public void setArtifactConfigs(Map<String, ArtifactsConfig> artifactConfigs) {
        this.artifactConfigs = artifactConfigs;
    }

    public void setGroups(String groups) {
        this.groups = groups;
    }

    public void setExcludedGroups(String excludedGroups) {
        this.excludedGroups = excludedGroups;
    }

    public void setExcludes(List<String> excludes) {
        this.excludes = excludes;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public int getVersion() {
        return VERSION;
    }

    public String getModuleName() {
        return moduleName;
    }

    public Map<String, String> getPluginArtifactHashes() {
        return pluginArtifactHashes;
    }

    public Map<String, String> getModuleArtifactHashes() {
        return Collections.unmodifiableMap(moduleArtifactHashes);
    }

    public Map<String, String> getLibraryArtifactHashes() {
        return Collections.unmodifiableMap(libraryArtifactHashes);
    }

    public SortedMap<String, String> getIgnoredProperties() {
        return ignoredProperties;
    }

    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public SortedMap<String, String> getClassesHashes() {
        return classesHashes;
    }

    public SortedMap<String, String> getTestClassesHashes() {
        return testClassesHashes;
    }

    public List<String> getActiveProfiles() {
        return activeProfiles;
    }

    public String getArgLine() {
        return argLine;
    }

    public String getTest() {
        return test;
    }

    public List<String> getExcludeClasspathResources() {
        return excludeClasspathResources;
    }

    public Map<String, ArtifactsConfig> getArtifactConfigs() {
        return artifactConfigs;
    }

    public String getGroups() {
        return groups;
    }

    public String getExcludedGroups() {
        return excludedGroups;
    }

    public List<String> getExcludes() {
        return excludes;
    }
}
