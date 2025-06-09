package com.github.seregamorph.maven.test.storage;

import java.net.URI;
import java.time.Duration;

/**
 * @author Sergey Chernov
 */
public record HttpCacheStorageConfig(
    URI baseUrl,
    boolean checkServerVersion,
    Duration connectTimeout,
    Duration readTimeout,
    Duration writeTimeout
) {
}
