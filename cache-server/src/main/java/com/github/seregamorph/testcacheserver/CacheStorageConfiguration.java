package com.github.seregamorph.testcacheserver;

import com.github.seregamorph.maven.test.storage.CacheStorage;
import com.github.seregamorph.maven.test.storage.FileCacheStorage;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Sergey Chernov
 */
@Configuration
public class CacheStorageConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(CacheStorageConfiguration.class);

    // TODO S3CacheStorage is already in the classpath from surefire-cached-common module
    // Configure it if needed instead of FileCacheStorage

    @Bean
    public CacheStorage cacheStorage() {
        String userHome = System.getProperty("user.home");
        if (userHome == null) {
            throw new IllegalStateException("Could not resolve default cacheStorageUrl, user.home is not defined.\n"
                + "Please provide custom CacheStorageConfiguration.");
        }
        var baseDir = new File(userHome, ".m2/cache-server");
        logger.info("Using cache storage located at {}", baseDir);
        return new FileCacheStorage(baseDir, 16);
    }
}
