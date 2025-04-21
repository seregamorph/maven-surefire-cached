package com.github.seregamorph.maven.test.extension.local;

import org.apache.maven.artifact.Artifact;

/**
 * @author Sergey Chernov
 */
public record GroupArtifactId(String groupId, String artifactId) {

    public static GroupArtifactId of(Artifact artifact) {
        return new GroupArtifactId(artifact.getGroupId(), artifact.getArtifactId());
    }

    @Override
    public String toString() {
        return groupId + ':' + artifactId;
    }
}
