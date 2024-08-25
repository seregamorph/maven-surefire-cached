package com.github.seregamorph.maven.test.common;

public record CacheEntryKey(String pluginName, GroupArtifactId groupArtifactId, String hash) {

    @Override
    public String toString() {
        return pluginName + '/' + groupArtifactId.groupId() + '/' + groupArtifactId.artifactId() + '/' + hash;
    }
}
