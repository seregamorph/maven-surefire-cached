package com.github.seregamorph.maven.test.storage;

import com.github.seregamorph.maven.test.common.CacheEntryKey;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.InvalidObjectStateException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Cache storage for AWS S3.
 * Can be used directly from Maven extension or in the cache web server.
 *
 * @author Sergey Chernov
 */
public class S3CacheStorage implements CacheStorage {

    private static final Logger logger = LoggerFactory.getLogger(S3CacheStorage.class);

    private final S3Client s3Client;
    private final S3CacheStorageConfig config;

    public S3CacheStorage(S3Client s3Client, S3CacheStorageConfig config) {
        this.s3Client = s3Client;
        this.config = config;
    }

    @Nullable
    @Override
    public byte[] read(CacheEntryKey cacheEntryKey, String fileName) throws CacheStorageException {
        String awsKey = cacheEntryKey + "/" + fileName;
        try {
            try (ResponseInputStream<GetObjectResponse> object = s3Client.getObject(b -> b.bucket(config.getBucket()).key(awsKey))) {
                byte[] bytes = IOUtils.toByteArray(object);
                String expiresString = object.response().expiresString();
                ZonedDateTime expires = parseExpires(expiresString);
                if (isExpired(expires)) {
                    logger.debug("Skipping cache entry {} expired at {}", awsKey, expires);
                    return null;
                }
                return bytes;
            }
        } catch (NoSuchKeyException | InvalidObjectStateException e) {
            return null;
        } catch (IOException e) {
            logger.warn("Error while reading from S3 {}", awsKey, e);
            return null;
        }
    }

    protected boolean isExpired(@Nullable ZonedDateTime expires) {
        return expires != null && expires.isBefore(ZonedDateTime.now());
    }

    @Override
    public int write(CacheEntryKey cacheEntryKey, String fileName, byte[] value) throws CacheStorageException {
        String awsKey = cacheEntryKey + "/" + fileName;
        Instant expires = Instant.now().plus(config.getExpiration());
        s3Client.putObject(b -> b.bucket(config.getBucket()).key(awsKey).expires(expires),
            RequestBody.fromBytes(value));
        return 0;
    }

    @Nullable
    static ZonedDateTime parseExpires(@Nullable String expiresString) {
        if (expiresString == null || expiresString.isEmpty()) {
            return null;
        }
        return ZonedDateTime.parse(expiresString, RFC_1123_DATE_TIME);
    }
}
