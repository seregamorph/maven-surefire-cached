package com.github.seregamorph.testcacheserver;

import com.github.seregamorph.maven.test.storage.CacheStorage;
import com.github.seregamorph.maven.test.storage.FileCacheStorage;
import java.io.File;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheStorageConfiguration {

    @Bean
    public CacheStorage cacheStorage() {
        var baseDir = new File(System.getProperty("user.home"), ".m2/test-cache-server");
        return new FileCacheStorage(baseDir, 64);
    }
}
