package com.github.seregamorph.testcacheserver.service;

import com.github.seregamorph.maven.test.common.CacheEntryKey;
import com.github.seregamorph.maven.test.common.TestTaskOutput;
import com.github.seregamorph.maven.test.storage.CacheStorage;
import com.github.seregamorph.maven.test.util.JsonSerializers;
import com.github.seregamorph.maven.test.util.ValidatorUtils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import javax.annotation.Nullable;
import org.springframework.stereotype.Service;

/**
 * @author Sergey Chernov
 */
@Service
public class CacheService {

    private static final List<String> TRACKED_TASK_OUTPUTS = List.of(
        "surefire-cached-output.json",
        "failsafe-cached-output.json"
    );

    private final CacheStorage cacheStorage;
    private final MeterRegistry meterRegistry;

    public CacheService(CacheStorage cacheStorage, MeterRegistry meterRegistry) {
        this.cacheStorage = cacheStorage;
        this.meterRegistry = meterRegistry;
    }

    public void putCache(CacheEntryKey cacheEntryKey, String fileName, byte[] body) {
        ValidatorUtils.validateFileName(fileName);
        cacheStorage.write(cacheEntryKey, fileName, body);
        var pluginName = cacheEntryKey.pluginName().name();

        Counter.builder("put.cache.size")
            .tag("pluginName", pluginName)
            .register(meterRegistry)
            .increment(body.length);

        Counter.builder("put.cache.files")
            .tag("pluginName", pluginName)
            .register(meterRegistry)
            .increment();

        if (TRACKED_TASK_OUTPUTS.contains(fileName)) {
            Counter.builder("put.cache")
                .tag("pluginName", pluginName)
                .register(meterRegistry)
                .increment();

            var testTaskOutput = JsonSerializers.deserialize(body, TestTaskOutput.class, fileName);
            var totalTimeSeconds = testTaskOutput.getTotalTimeSeconds();
            Counter.builder("cache.spent.time.seconds")
                .tag("pluginName", pluginName)
                .register(meterRegistry)
                .increment(totalTimeSeconds.doubleValue());
        }
    }

    @Nullable
    public byte[] getCache(CacheEntryKey cacheEntryKey, String fileName) {
        ValidatorUtils.validateFileName(fileName);
        var readResult = cacheStorage.read(cacheEntryKey, fileName);
        var pluginName = cacheEntryKey.pluginName().name();

        if (readResult != null) {
            Counter.builder("get.cache.size")
                .tag("pluginName", pluginName)
                .register(meterRegistry)
                .increment(readResult.length());
        }

        // this is different from "get_cache_hit" - calculate all returned files
        Counter.builder("get.cache.files")
            .tag("pluginName", pluginName)
            .tag("exist", Boolean.toString(readResult != null))
            .register(meterRegistry)
            .increment();

        if (TRACKED_TASK_OUTPUTS.contains(fileName)) {
            // "get_cache_miss" and "get_cache_hit" calculate once per test execution entity
            if (readResult == null) {
                Counter.builder("get.cache.miss")
                    .tag("pluginName", pluginName)
                    .register(meterRegistry)
                    .increment();
                return null;
            }

            Counter.builder("get.cache.hit")
                .tag("pluginName", pluginName)
                .register(meterRegistry)
                .increment();

            var testTaskOutput = JsonSerializers.deserialize(readResult.bytes(), TestTaskOutput.class, fileName);
            Counter.builder("cache.saved.time.seconds")
                .tag("pluginName", pluginName)
                .register(meterRegistry)
                .increment(testTaskOutput.getTotalTimeSeconds().doubleValue());
        }

        return readResult == null ? null : readResult.bytes();
    }
}
