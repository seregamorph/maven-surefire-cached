package com.github.seregamorph.maven.test.extension;

import static com.github.seregamorph.maven.test.util.MavenPropertyUtils.getProperty;

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
        String cacheStorageUrl = getProperty(session, PROP_CACHE_STORAGE_URL);
        if (cacheStorageUrl == null) {
            String userHome = System.getProperty("user.home");
            if (userHome == null) {
                throw new IllegalStateException("Could not resolve default cacheStorageUrl, user.home is not defined.\n"
                    + "Please provide -DcacheStorageUrl= with directory or HTTP/HTTPS url of cache");
            }
            cacheStorageUrl = userHome + "/.m2/test-cache";
        }
        //noinspection HttpUrlsUsage
        if (cacheStorageUrl.startsWith("http://") || cacheStorageUrl.startsWith("https://")) {
            // todo support custom configuration, auth, etc.
            HttpCacheStorageConfig httpCacheStorageConfig = new HttpCacheStorageConfig(URI.create(cacheStorageUrl),
                true, Duration.ofSeconds(5L), Duration.ofSeconds(10L), Duration.ofSeconds(10L));
            return new HttpCacheStorage(httpCacheStorageConfig);
        }

        return new FileCacheStorage(new File(cacheStorageUrl));
    }
}
