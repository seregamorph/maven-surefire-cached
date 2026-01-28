package com.github.seregamorph.maven.test.storage;

/**
 * @author Sergey Chernov
 */
public final class ReadResult {

    private final byte[] bytes;

    public ReadResult(byte[] bytes) {
        this.bytes = bytes;
    }

    public byte[] bytes() {
        return bytes;
    }

    public int length() {
        return bytes.length;
    }

    @Override
    public String toString() {
        return "ReadResult{" +
            "bytes=" + length() + " bytes" +
            '}';
    }
}
