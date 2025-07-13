package com.github.seregamorph.maven.test.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class S3CacheStorageTest {

    @Test
    void shouldParseExpires() {
        var expires = S3CacheStorage.parseExpires("Sat, 31 May 2025 01:02:03 GMT");
        assertEquals(ZonedDateTime.parse("2025-05-31T01:02:03Z"), expires);
    }
}
