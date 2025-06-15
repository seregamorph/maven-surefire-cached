package com.github.seregamorph.maven.test.common;

import com.github.seregamorph.maven.test.util.ValidatorUtils;
import java.util.Objects;

/**
 * @author Sergey Chernov
 */
public final class CacheEntryKey {

    private final PluginName pluginName;
    private final GroupArtifactId groupArtifactId;
    private final String hash;

    public CacheEntryKey(PluginName pluginName, GroupArtifactId groupArtifactId, String hash) {
        ValidatorUtils.validateFileName(hash);
        this.pluginName = pluginName;
        this.groupArtifactId = groupArtifactId;
        this.hash = hash;
    }

    public PluginName pluginName() {
        return pluginName;
    }

    public GroupArtifactId groupArtifactId() {
        return groupArtifactId;
    }

    public String hash() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        CacheEntryKey that = (CacheEntryKey) obj;
        return Objects.equals(this.pluginName, that.pluginName)
            && Objects.equals(this.groupArtifactId, that.groupArtifactId)
            && Objects.equals(this.hash, that.hash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pluginName, groupArtifactId, hash);
    }

    @Override
    public String toString() {
        return pluginName + "/" + groupArtifactId.groupId() + '/' + groupArtifactId.artifactId() + '/' + hash;
    }
}
