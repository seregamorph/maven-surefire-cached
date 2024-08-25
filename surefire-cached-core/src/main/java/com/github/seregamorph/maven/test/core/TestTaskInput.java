package com.github.seregamorph.maven.test.core;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.github.seregamorph.maven.test.common.GroupArtifactId;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.annotation.Nullable;

@JsonPropertyOrder({
        "moduleName",
        "timestamp",
        "argLine",
        "test",
        "ignoredProperties",
        "properties",
        "classesHashes",
        "testClassesHashes",
        "artifactHashes"
})
public final class TestTaskInput {

    private final SortedMap<String, String> ignoredProperties = new TreeMap<>();

    private final SortedMap<String, String> properties = new TreeMap<>();

    /**
     * "$groupId:$artifactId[:$classifier]" (no version and optional classifier) -> file hash
     * artifactName is not included in hash, only file hash with classpath sensitivity (ignore timestamp)
     */
    private final SortedMap<String, String> artifactHashes = new TreeMap<>();

    // "$groupId:$artifactId", not included in hash
    private String moduleName;
    @Nullable
    private SortedMap<String, String> classesHashes;
    @Nullable
    private SortedMap<String, String> testClassesHashes;
    private String argLine;
    private String test;
    private List<String> excludes;

    @JsonIgnore
    public String hash() {
        var sw = new StringWriter();
        var pw = new PrintWriter(sw, true);
        properties.forEach((key, value) -> pw.println(key + " -> " + value));
        // repeatable because of sorted map
        artifactHashes.values().forEach(pw::println);
        if (classesHashes != null) {
            classesHashes.forEach((key, value) -> pw.println(key + " -> " + value));
        }
        if (testClassesHashes != null) {
            testClassesHashes.forEach((key, value) -> pw.println(key + " -> " + value));
        }
        pw.println(argLine);
        pw.println(test);
        pw.println(excludes);
        return HashUtils.hashArray(sw.toString().getBytes(UTF_8));
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public void addArtifactHash(GroupArtifactId groupArtifactId, @Nullable String classifier, String hash) {
        var key = groupArtifactId.toString();
        if (classifier != null && !classifier.isEmpty()) {
            key += ":" + classifier;
        }
        if (artifactHashes.put(key, hash) != null) {
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

    public void setArgLine(String argLine) {
        this.argLine = argLine;
    }

    public void setTest(String test) {
        this.test = test;
    }

    public void setExcludes(List<String> excludes) {
        this.excludes = excludes;
    }

    public String getModuleName() {
        return moduleName;
    }

    public Map<String, String> getArtifactHashes() {
        return Collections.unmodifiableMap(artifactHashes);
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

    public String getArgLine() {
        return argLine;
    }

    public String getTest() {
        return test;
    }

    public List<String> getExcludes() {
        return excludes;
    }
}
