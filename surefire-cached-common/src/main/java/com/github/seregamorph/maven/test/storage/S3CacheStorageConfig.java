package com.github.seregamorph.maven.test.storage;

import java.time.Duration;
import java.util.Objects;

/**
 * @author Sergey Chernov
 */
public class S3CacheStorageConfig {

    private final String bucket;
    private final Duration expiration;

    public S3CacheStorageConfig(String bucket, Duration expiration) {
        this.bucket = Objects.requireNonNull(bucket, "bucket");
        this.expiration = Objects.requireNonNull(expiration, "expiration");
    }

    public String getBucket() {
        return bucket;
    }

    public Duration getExpiration() {
        return expiration;
    }

    @Override
    public String toString() {
        return "S3CacheStorageConfig{" +
            "bucket='" + bucket + '\'' +
            ", expiration=" + expiration +
            '}';
    }
}
