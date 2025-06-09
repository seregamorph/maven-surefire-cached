package com.github.seregamorph.maven.test.extension;

import com.github.seregamorph.maven.test.storage.CacheStorage;
import com.github.seregamorph.maven.test.storage.FileCacheStorage;
import com.github.seregamorph.maven.test.storage.HttpCacheStorage;
import com.github.seregamorph.maven.test.storage.HttpCacheStorageConfig;
import java.io.File;
import java.net.URI;
import java.time.Duration;
import org.apache.maven.execution.MavenSession;

/**
 * @author Sergey Chernov
 */
class CacheStorageFactory {

    private static final String PROP_CACHE_STORAGE_URL = "cacheStorageUrl";

    private final MavenSession session;

    CacheStorageFactory(MavenSession session) {
        this.session = session;
    }

    CacheStorage createCacheStorage() {
        String cacheStorageUrl = session.getUserProperties().getProperty(PROP_CACHE_STORAGE_URL);
        if (cacheStorageUrl == null) {
            cacheStorageUrl = System.getProperty("user.home") + "/.m2/test-cache";
        }
        //noinspection HttpUrlsUsage
        if (cacheStorageUrl.startsWith("http://") || cacheStorageUrl.startsWith("https://")) {
            // todo support custom configuration, auth, etc.
            var httpCacheStorageConfig = new HttpCacheStorageConfig(URI.create(cacheStorageUrl),
                true, Duration.ofSeconds(5L), Duration.ofSeconds(10L), Duration.ofSeconds(10L));
            return new HttpCacheStorage(httpCacheStorageConfig);
        }

        return new FileCacheStorage(new File(cacheStorageUrl));
    }
}
