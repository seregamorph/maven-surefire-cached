package com.github.seregamorph.maven.test.storage;

import com.github.seregamorph.maven.test.common.CacheEntryKey;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;

/**
 * Wrapping cache storage that packs/unpacks entities to save storage space. May be efficient in combination with
 * in-memory storages.
 *
 * @author Sergey Chernov
 */
public class GzipDelegateCacheStorage implements CacheStorage {

    private final CacheStorage delegate;

    public GzipDelegateCacheStorage(CacheStorage delegate) {
        this.delegate = delegate;
    }

    @Nullable
    @Override
    public ReadResult read(CacheEntryKey cacheEntryKey, String fileName) throws CacheStorageException {
        ReadResult readResult = delegate.read(cacheEntryKey, fileName);
        if (readResult == null) {
            return null;
        }
        if (isAlreadyCompressed(fileName)) {
            // .tar.gz files were already compressed, no need to unpack
            return readResult;
        } else {
            try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(readResult.bytes()))) {
                byte[] bytes = IOUtils.toByteArray(in);
                return new ReadResult(bytes);
            } catch (IOException e) {
                throw new CacheStorageException("Error unpacking file " + fileName, e);
            }
        }
    }

    @Override
    public WriteResult write(CacheEntryKey cacheEntryKey, String fileName, byte[] value) throws CacheStorageException {
        byte[] bytesToWrite;
        if (isAlreadyCompressed(fileName)) {
            bytesToWrite = value;
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
                gzip.write(value);
            } catch (IOException e) {
                throw new CacheStorageException("Error while packing", e);
            }
            // note: toByteArray should be called after try-with-resources, not inside
            bytesToWrite = baos.toByteArray();
        }

        return delegate.write(cacheEntryKey, fileName, bytesToWrite);
    }

    private static boolean isAlreadyCompressed(String fileName) {
        return fileName.endsWith(".gz");
    }
}
