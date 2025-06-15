package com.github.seregamorph.maven.test.storage;

import java.net.URI;
import java.time.Duration;

/**
 * @author Sergey Chernov
 */
public final class HttpCacheStorageConfig {

    private final URI baseUrl;
    private final boolean checkServerVersion;
    private final Duration connectTimeout;
    private final Duration readTimeout;
    private final Duration writeTimeout;

    public HttpCacheStorageConfig(
        URI baseUrl,
        boolean checkServerVersion,
        Duration connectTimeout,
        Duration readTimeout,
        Duration writeTimeout
    ) {
        this.baseUrl = baseUrl;
        this.checkServerVersion = checkServerVersion;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.writeTimeout = writeTimeout;
    }

    public URI baseUrl() {
        return baseUrl;
    }

    public boolean checkServerVersion() {
        return checkServerVersion;
    }

    public Duration connectTimeout() {
        return connectTimeout;
    }

    public Duration readTimeout() {
        return readTimeout;
    }

    public Duration writeTimeout() {
        return writeTimeout;
    }

    @Override
    public String toString() {
        return "HttpCacheStorageConfig[" +
            "baseUrl=" + baseUrl + ", " +
            "checkServerVersion=" + checkServerVersion + ", " +
            "connectTimeout=" + connectTimeout + ", " +
            "readTimeout=" + readTimeout + ", " +
            "writeTimeout=" + writeTimeout + ']';
    }
}
