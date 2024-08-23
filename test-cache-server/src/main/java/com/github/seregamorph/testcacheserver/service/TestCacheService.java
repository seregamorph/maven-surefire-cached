package com.github.seregamorph.testcacheserver.service;

import com.github.seregamorph.maven.test.common.CacheEntryKey;
import com.github.seregamorph.maven.test.common.TestTaskOutput;
import com.github.seregamorph.maven.test.core.JsonSerializers;
import com.github.seregamorph.maven.test.storage.CacheStorage;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import javax.annotation.Nullable;
import org.springframework.stereotype.Service;

@Service
public class TestCacheService {

    private static final List<String> TRACKED_TASK_OUTPUTS = List.of(
        "surefire-cached-output.json",
        "failsafe-cached-output.json"
    );

    private final CacheStorage cacheStorage;
    private final MeterRegistry meterRegistry;

    public TestCacheService(CacheStorage cacheStorage, MeterRegistry meterRegistry) {
        this.cacheStorage = cacheStorage;
        this.meterRegistry = meterRegistry;
    }

    public void putCache(CacheEntryKey cacheEntryKey, String fileName, byte[] body) {
        cacheStorage.write(cacheEntryKey, fileName, body);

        Counter.builder("put_cache")
            .tag("pluginName", cacheEntryKey.pluginName())
            .register(meterRegistry)
            .increment();

        Counter.builder("put_cache_size")
            .tag("pluginName", cacheEntryKey.pluginName())
            .register(meterRegistry)
            .increment(body.length);
    }

    @Nullable
    public byte[] getCache(CacheEntryKey cacheEntryKey, String fileName) {
        var body = cacheStorage.read(cacheEntryKey, fileName);
        if (body == null) {
            Counter.builder("get_cache_miss")
                .tag("pluginName", cacheEntryKey.pluginName())
                .register(meterRegistry)
                .increment();
            return null;
        }

        Counter.builder("get_cache_hit")
            .tag("pluginName", cacheEntryKey.pluginName())
            .register(meterRegistry)
            .increment();

        Counter.builder("get_cache_size")
            .tag("pluginName", cacheEntryKey.pluginName())
            .register(meterRegistry)
            .increment(body.length);

        if (TRACKED_TASK_OUTPUTS.contains(fileName)) {
            var testTaskOutput = JsonSerializers.deserialize(body, TestTaskOutput.class);
            Counter.builder("cache_saved_time_seconds")
                .tag("pluginName", cacheEntryKey.pluginName())
                .register(meterRegistry)
                .increment(testTaskOutput.totalTime().doubleValue());
        }

        return body;
    }
}
