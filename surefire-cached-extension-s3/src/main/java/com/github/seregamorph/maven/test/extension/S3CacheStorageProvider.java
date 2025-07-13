package com.github.seregamorph.maven.test.extension;

import com.github.seregamorph.maven.test.extension.spi.CacheStorageProvider;
import com.github.seregamorph.maven.test.storage.CacheStorage;
import com.github.seregamorph.maven.test.storage.S3CacheStorage;
import com.github.seregamorph.maven.test.storage.S3CacheStorageConfig;
import com.github.seregamorph.maven.test.util.PropertySource;
import java.net.URI;
import java.time.Duration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 * @author Sergey Chernov
 */
public class S3CacheStorageProvider implements CacheStorageProvider {

    private static final String S3_PREFIX = "s3://";

    @Override
    public boolean supportsCacheStorageUrl(String cacheStorageUrl) {
        return cacheStorageUrl.startsWith(S3_PREFIX);
    }

    @Override
    public CacheStorage createCacheStorage(String cacheStorageUrl, PropertySource propertySource) {
        S3ClientBuilder s3ClientBuilder = S3Client.builder();
        String s3EndpointOverride = propertySource.getProperty("s3EndpointOverride", null);
        if (s3EndpointOverride != null) {
            // for localstack compatibility
            s3ClientBuilder.serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build());
            s3ClientBuilder.endpointOverride(URI.create(s3EndpointOverride));
        }
        S3Client s3Client = s3ClientBuilder.build();

        String bucket = cacheStorageUrl.substring(S3_PREFIX.length());
        if (bucket.isEmpty()) {
            throw new IllegalArgumentException("S3 bucket is empty: " + cacheStorageUrl);
        }
        if (bucket.endsWith("/")) {
            throw new IllegalArgumentException("S3 bucket should not end with '/': " + cacheStorageUrl);
        }
        Duration expiration = Duration.ofHours(Integer.parseInt(
            propertySource.getProperty("cacheExpirationHours", "6")));
        S3CacheStorageConfig s3CacheStorageConfig = new S3CacheStorageConfig(bucket, expiration);
        return new S3CacheStorage(s3Client, s3CacheStorageConfig);
    }
}
