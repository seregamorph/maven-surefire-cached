package com.github.seregamorph.maven.test.storage;

import com.github.seregamorph.maven.test.common.CacheEntryKey;
import com.github.seregamorph.maven.test.common.ServerProtocolVersion;
import com.github.seregamorph.maven.test.util.ResponseBodyUtils;
import java.io.IOException;
import java.net.URI;
import javax.annotation.Nullable;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache storage on a remote HTTP service. Uses HTTP PUT and GET methods.
 *
 * @author Sergey Chernov
 */
public class HttpCacheStorage implements CacheStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpCacheStorage.class);

    private static final MediaType TYPE = MediaType.get("application/octet-stream");

    private final URI baseUrl;
    private final boolean checkServerVersion;
    private final OkHttpClient client;

    public HttpCacheStorage(HttpCacheStorageConfig config) {
        this.baseUrl = config.baseUrl();
        this.checkServerVersion = config.checkServerVersion();
        this.client = createHttpClient(config);
    }

    private static OkHttpClient createHttpClient(HttpCacheStorageConfig config) {
        return new OkHttpClient.Builder()
            .connectTimeout(config.connectTimeout())
            .readTimeout(config.readTimeout())
            .writeTimeout(config.writeTimeout())
            .build();
    }

    @Nullable
    @Override
    public byte[] read(CacheEntryKey cacheEntryKey, String fileName) {
        var url = getEntryUri(cacheEntryKey, fileName);
        try {
            Request request = new Request.Builder()
                .get()
                .url(url)
                .build();
            LOGGER.info("Fetching from cache: {}", url);
            try (Response response = client.newCall(request).execute()) {
                if (checkServerVersion) {
                    checkServerVersion(response);
                }
                if (response.code() == 404) {
                    return null;
                }
                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    throw new IOException("No response body with response code: " + response.code());
                }
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected response code: " + response.code()
                        + "\n" + ResponseBodyUtils.responseBodyForLog(responseBody.string()));
                }
                return responseBody.bytes();
            }
        } catch (IOException e) {
            throw new CacheStorageException("Error while fetching from cache " + url + " " + e, e);
        }
    }

    @Override
    public int write(CacheEntryKey cacheEntryKey, String fileName, byte[] value) {
        var url = getEntryUri(cacheEntryKey, fileName);
        try {
            var requestBody = RequestBody.create(value, TYPE);
            Request request = new Request.Builder()
                .put(requestBody)
                .url(url)
                .build();
            LOGGER.info("Pushing to cache: {}", url);
            try (Response response = client.newCall(request).execute()) {
                if (checkServerVersion) {
                    checkServerVersion(response);
                }
                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    throw new IOException("No response body with response code: " + response.code());
                }
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected response code: " + response.code()
                        + "\n" + ResponseBodyUtils.responseBodyForLog(responseBody.string()));
                }
            }
        } catch (IOException e) {
            throw new CacheStorageException("Error while pushing to cache " + url + " " + e, e);
        }

        return 0;
    }

    private String getEntryUri(CacheEntryKey cacheEntryKey, String fileName) {
        return baseUrl + "/" + cacheEntryKey + "/" + fileName;
    }

    private void checkServerVersion(Response response) {
        var serverVersionStr = response.header(ServerProtocolVersion.HEADER_SERVER_PROTOCOL_VERSION);
        @Nullable var serverVersion = serverVersionStr == null || serverVersionStr.isEmpty() ?
            null : Integer.parseInt(serverVersionStr);
        if (serverVersion == null || serverVersion < ServerProtocolVersion.MIN_SERVER_PROTOCOL_VERSION) {
            // this way we prevent failures caused by a breaking change with the new structure of TestTaskOutput
            throw new MinServerProtocolVersionException("surefire-cached-extension is not compatible with server "
                + "at " + baseUrl + ", "
                + "please update test-cache-server first", serverVersion,
                ServerProtocolVersion.MIN_SERVER_PROTOCOL_VERSION);
        }
    }
}
