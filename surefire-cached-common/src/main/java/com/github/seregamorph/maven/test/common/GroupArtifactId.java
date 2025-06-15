package com.github.seregamorph.maven.test.common;

import com.github.seregamorph.maven.test.util.ValidatorUtils;
import java.util.Objects;

/**
 * @author Sergey Chernov
 */
public final class GroupArtifactId implements Comparable<GroupArtifactId>{

    private final String groupId;
    private final String artifactId;

    public GroupArtifactId(String groupId, String artifactId) {
        ValidatorUtils.validateFileName(groupId);
        ValidatorUtils.validateFileName(artifactId);
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public String groupId() {
        return groupId;
    }

    public String artifactId() {
        return artifactId;
    }

    public static GroupArtifactId fromString(String str) {
        String[] parts = str.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid format: [" + str + "]");
        }
        return new GroupArtifactId(parts[0], parts[1]);
    }

    @Override
    public int compareTo(GroupArtifactId that) {
        return toString().compareTo(that.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GroupArtifactId that = (GroupArtifactId) o;
        return Objects.equals(groupId, that.groupId)
            && Objects.equals(artifactId, that.artifactId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId);
    }

    @Override
    public String toString() {
        return groupId + ':' + artifactId;
    }
}
