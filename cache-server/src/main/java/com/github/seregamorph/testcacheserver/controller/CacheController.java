package com.github.seregamorph.testcacheserver.controller;

import com.github.seregamorph.maven.test.common.CacheEntryKey;
import com.github.seregamorph.maven.test.common.GroupArtifactId;
import com.github.seregamorph.maven.test.common.PluginName;
import com.github.seregamorph.testcacheserver.service.CacheService;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Sergey Chernov
 */
@Controller
@RequestMapping("/cache")
public class CacheController {

    private final CacheService cacheService;

    public CacheController(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Timed(value = "putCache")
    @Counted(value = "putCache")
    @PutMapping("/{pluginName}/{groupId}/{artifactId}/{hash}/{fileName}")
    public ResponseEntity<?> putCache(
        @PathVariable("pluginName") PluginName pluginName,
        @PathVariable("groupId") String groupId,
        @PathVariable("artifactId") String artifactId,
        @PathVariable("hash") String hash,
        @PathVariable("fileName") String fileName,
        @RequestBody byte[] body
    ) {
        var cacheEntryKey = new CacheEntryKey(pluginName, new GroupArtifactId(groupId, artifactId), hash);
        cacheService.putCache(cacheEntryKey, fileName, body);
        return ResponseEntity.ok().build();
    }

    @Timed(value = "getCache")
    @Counted(value = "getCache")
    @GetMapping("/{pluginName}/{groupId}/{artifactId}/{hash}/{fileName}")
    public ResponseEntity<byte[]> getCache(
        @PathVariable("pluginName") PluginName pluginName,
        @PathVariable("groupId") String groupId,
        @PathVariable("artifactId") String artifactId,
        @PathVariable("hash") String hash,
        @PathVariable("fileName") String fileName
    ) {
        var cacheEntryKey = new CacheEntryKey(pluginName, new GroupArtifactId(groupId, artifactId), hash);
        var body = cacheService.getCache(cacheEntryKey, fileName);
        if (body == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
            .contentType(getContentType(fileName))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
            .body(body);
    }

    private static MediaType getContentType(String fileName) {
        return fileName.endsWith(".json") ? MediaType.APPLICATION_JSON : MediaType.APPLICATION_OCTET_STREAM;
    }
}
