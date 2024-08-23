package com.github.seregamorph.maven.test.common;

public record CacheEntryKey(String pluginName, String groupId, String artifactId, String hash) {

    @Override
    public String toString() {
        return pluginName + '/' + groupId + '/' + artifactId + '/' + hash;
    }
}
