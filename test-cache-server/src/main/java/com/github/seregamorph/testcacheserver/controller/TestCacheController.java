package com.github.seregamorph.testcacheserver.controller;

import com.github.seregamorph.maven.test.common.CacheEntryKey;
import com.github.seregamorph.maven.test.common.GroupArtifactId;
import com.github.seregamorph.testcacheserver.service.TestCacheService;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/cache")
public class TestCacheController {

    private final TestCacheService testCacheService;

    public TestCacheController(TestCacheService testCacheService) {
        this.testCacheService = testCacheService;
    }

    @Timed(value = "putCache")
    @Counted(value = "putCache")
    @PutMapping("/{pluginName}/{groupId}/{artifactId}/{hash}/{file}")
    public ResponseEntity<?> putCache(
        @PathVariable("pluginName") String pluginName,
        @PathVariable("groupId") String groupId,
        @PathVariable("artifactId") String artifactId,
        @PathVariable("hash") String hash,
        @PathVariable("file") String fileName,
        @RequestBody byte[] body
    ) {
        var cacheEntryKey = new CacheEntryKey(pluginName, new GroupArtifactId(groupId, artifactId), hash);
        testCacheService.putCache(cacheEntryKey, fileName, body);
        return ResponseEntity.ok().build();
    }

    @Timed(value = "getCache")
    @Counted(value = "getCache")
    @GetMapping("/{pluginName}/{groupId}/{artifactId}/{hash}/{file}")
    public ResponseEntity<byte[]> getCache(
        @PathVariable("pluginName") String pluginName,
        @PathVariable("groupId") String groupId,
        @PathVariable("artifactId") String artifactId,
        @PathVariable("hash") String hash,
        @PathVariable("file") String fileName
    ) {
        var cacheEntryKey = new CacheEntryKey(pluginName, new GroupArtifactId(groupId, artifactId), hash);
        var body = testCacheService.getCache(cacheEntryKey, fileName);
        if (body == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(body);
    }
}
