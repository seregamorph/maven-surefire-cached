package com.github.seregamorph.maven.test.common;

/**
 * @author Sergey Chernov
 */
public final class ServerProtocolVersion {

    public static final int SERVER_PROTOCOL_VERSION = 1;
    /**
     * Each time when the server has a breaking change, this should be increased
     */
    public static final int MIN_SERVER_PROTOCOL_VERSION = 1;

    public static final String HEADER_SERVER_PROTOCOL_VERSION =  "Server-Protocol-Version";

    private ServerProtocolVersion() {
    }
}
