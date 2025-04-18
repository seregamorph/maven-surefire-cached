package com.github.seregamorph.maven.test.storage;

import com.github.seregamorph.maven.test.common.CacheEntryKey;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import javax.annotation.Nullable;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sergey Chernov
 */
public class HttpCacheStorage implements CacheStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpCacheStorage.class);

    private static final MediaType TYPE = MediaType.get("application/octet-stream");

    private final OkHttpClient client = new OkHttpClient();

    private final URI baseUrl;

    public HttpCacheStorage(URI baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Nullable
    @Override
    public byte[] read(CacheEntryKey cacheEntryKey, String fileName) {
        var url = getEntryUri(cacheEntryKey, fileName);
        try {
            Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
            LOGGER.info("Fetching from cache: {}", url);
            try (Response response = client.newCall(request).execute()) {
                if (response.code() == 404) {
                    return null;
                }
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected response code: " + response.code());
                }
                return response.body().bytes();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Error while fetching from cache " + url, e);
        }
    }

    @Override
    public int write(CacheEntryKey cacheEntryKey, String fileName, byte[] value) {
        var url = getEntryUri(cacheEntryKey, fileName);
        try {
            var body = RequestBody.create(value, TYPE);
            Request request = new Request.Builder()
                .url(url)
                .put(body)
                .build();
            LOGGER.info("Pushing to cache: {}", url);
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected response code: " + response.code());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Error while pushing to cache " + url, e);
        }

        return 0;
    }

    private String getEntryUri(CacheEntryKey cacheEntryKey, String fileName) {
        return baseUrl + "/" + cacheEntryKey + "/" + fileName;
    }
}
