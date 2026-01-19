package com.github.seregamorph.maven.test.extension;

import static com.github.seregamorph.maven.test.util.MavenPropertyUtils.isTrue;

import com.github.seregamorph.maven.test.extension.spi.CacheStorageProvider;
import com.github.seregamorph.maven.test.storage.CacheStorage;
import com.github.seregamorph.maven.test.storage.FileCacheStorage;
import com.github.seregamorph.maven.test.storage.HttpCacheStorage;
import com.github.seregamorph.maven.test.storage.HttpCacheStorageConfig;
import com.github.seregamorph.maven.test.util.EnvironmentUtils;
import com.github.seregamorph.maven.test.util.PropertySource;
import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.util.ServiceLoader;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sergey Chernov
 */
class CacheStorageFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheStorageFactory.class);

    private static final String PROP_CACHE_STORAGE_URL = "cacheStorageUrl";
    private static final String PROP_MAX_CACHE_ENTRIES = "maxCacheEntries";

    /**
     * Max number of cache entries per single "$groupId/artifactId" layout
     */
    private static final String DEFAULT_CACHE_ENTRIES = "4";
    private static final String DEFAULT_CACHE_ENTRIES_CI = "16";

    private final PropertySource propertySource;

    CacheStorageFactory(PropertySource propertySource) {
        this.propertySource = propertySource;
    }

    CacheStorage createCacheStorage() {
        String cacheStorageUrl = propertySource.getProperty(PROP_CACHE_STORAGE_URL, null);
        if (cacheStorageUrl == null) {
            String userHome = System.getProperty("user.home");
            if (userHome == null) {
                throw new IllegalStateException("Could not resolve default cacheStorageUrl, user.home is not defined.\n"
                    + "Please provide -D" + PROP_CACHE_STORAGE_URL + "= with directory or HTTP/HTTPS url of the cache");
            }
            cacheStorageUrl = userHome + "/.m2/test-cache";
        }
        //noinspection HttpUrlsUsage
        if (cacheStorageUrl.startsWith("http://") || cacheStorageUrl.startsWith("https://")) {
            return createHttpCacheStorage(cacheStorageUrl);
        }

        // SPI support optional S3CacheStorage from another module
        ServiceLoader<CacheStorageProvider> loader =
            ServiceLoader.load(CacheStorageProvider.class, CacheStorageProvider.class.getClassLoader());
        for (CacheStorageProvider storageProvider : loader) {
            if (storageProvider.supportsCacheStorageUrl(cacheStorageUrl)) {
                return storageProvider.createCacheStorage(cacheStorageUrl, propertySource);
            }
        }

        if (cacheStorageUrl.startsWith("s3:")) {
            String version = getClass().getPackage().getImplementationVersion();
            throw new IllegalArgumentException("The " + PROP_CACHE_STORAGE_URL + " is defined as "
                + cacheStorageUrl + "\n"
                + "Please use\n"
                + "    <extension>\n"
                + "        <groupId>com.github.seregamorph</groupId>\n"
                + "        <artifactId>surefire-cached-extension-s3</artifactId>\n"
                + "        <version>" + version + "</version>\n"
                + "    </extension>\n"
                + "instead of surefire-cached-extension in .mvn/extensions.xml");
        }
        int maxCacheEntries = getMaxCacheEntries();
        LOGGER.debug("maxCacheEntries for FileCacheStorage: {}", maxCacheEntries);
        return new FileCacheStorage(new File(cacheStorageUrl), maxCacheEntries);
    }

    private HttpCacheStorage createHttpCacheStorage(String cacheStorageUrl) {
        boolean checkServerVersion = isTrue(propertySource.getProperty("cacheCheckServerVersion", "false"));
        Duration connectTimeout = Duration.ofSeconds(Integer.parseInt(
            propertySource.getProperty("cacheConnectTimeoutSec", "10")));
        Duration readTimeout = Duration.ofSeconds(Integer.parseInt(
            propertySource.getProperty("cacheReadTimeoutSec", "10")));
        Duration writeTimeout = Duration.ofSeconds(Integer.parseInt(
            propertySource.getProperty("cacheWriteTimeoutSec", "10")));
        @Nullable String cacheHashPrefix = propertySource.getProperty("cacheHashPrefix", null);
        HttpCacheStorageConfig httpCacheStorageConfig = new HttpCacheStorageConfig(
            URI.create(cacheStorageUrl), checkServerVersion,
            connectTimeout, readTimeout, writeTimeout,
            cacheHashPrefix);
        return new HttpCacheStorage(httpCacheStorageConfig);
    }

    private int getMaxCacheEntries() {
        String defaultCacheEntries = EnvironmentUtils.isCi() ? DEFAULT_CACHE_ENTRIES_CI : DEFAULT_CACHE_ENTRIES;
        String maxCacheEntries = propertySource.getProperty(PROP_MAX_CACHE_ENTRIES, defaultCacheEntries);
        return Integer.parseInt(maxCacheEntries);
    }
}
