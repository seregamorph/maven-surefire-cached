package com.github.seregamorph.maven.test.storage;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.seregamorph.maven.test.common.CacheEntryKey;
import com.github.seregamorph.maven.test.common.GroupArtifactId;
import com.github.seregamorph.maven.test.common.PluginName;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

class GzipDelegateCacheStorageTest {

    @Language("JSON")
    private static final String JSON_CONTENT = """
        {
          "startTime" : "2025-05-10T18:55:14.282376Z",
          "endTime" : "2025-05-10T18:55:24.093039Z",
          "totalTimeSeconds" : 9.810,
          "totalTestTimeSeconds" : 0.954,
          "totalTests" : 70,
          "totalErrors" : 0,
          "totalFailures" : 0,
          "files" : {
            "jacoco" : "jacoco.tar.gz",
            "surefire-reports" : "surefire-reports.tar.gz"
          }
        }
        """;

    @Test
    public void testGzipDelegateCacheStorage() {
        var storage = new GzipDelegateCacheStorage(new SoftReferenceMemoryStorage(4));
        var cacheEntryKey = new CacheEntryKey(PluginName.SUREFIRE_CACHED,
            new GroupArtifactId("com.acme", "utils"), "01234567890abcdef");
        var fileName = "content.json";

        var writeResult = storage.write(cacheEntryKey, fileName, JSON_CONTENT.getBytes(UTF_8));
        assertEquals(0, writeResult.deletedFilesCount());

        var restoredReadResult = storage.read(cacheEntryKey, fileName);
        assertNotNull(restoredReadResult);
        assertEquals(JSON_CONTENT, new String(restoredReadResult.bytes(), UTF_8));
    }
}
