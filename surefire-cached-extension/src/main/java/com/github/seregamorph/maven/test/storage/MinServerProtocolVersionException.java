package com.github.seregamorph.maven.test.storage;

import javax.annotation.Nullable;

/**
 * @author Sergey Chernov
 */
public class MinServerProtocolVersionException extends RuntimeException {

    public MinServerProtocolVersionException(String message, @Nullable Integer serverVersion, int minServerVersion) {
        super(message + ", serverVersion: " + (serverVersion == null ? "<empty>" : serverVersion.toString()) + ", "
            + "minServerVersion: " + minServerVersion);
    }
}
