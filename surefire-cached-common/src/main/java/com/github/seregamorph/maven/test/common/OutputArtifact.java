package com.github.seregamorph.maven.test.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Sergey Chernov
 */
@JsonIgnoreProperties(ignoreUnknown = true) // for forward compatibility
public final class OutputArtifact {

    private final String fileName;
    private final int files;
    private final long unpackedSize;
    private final long packedSize;

    @JsonCreator
    public OutputArtifact(
        String fileName,
        int files,
        long unpackedSize,
        long packedSize
    ) {
        this.fileName = fileName;
        this.files = files;
        this.unpackedSize = unpackedSize;
        this.packedSize = packedSize;
    }

    public String getFileName() {
        return fileName;
    }

    public int getFiles() {
        return files;
    }

    public long getUnpackedSize() {
        return unpackedSize;
    }

    public long getPackedSize() {
        return packedSize;
    }

    @Override
    public String toString() {
        return "OutputArtifact[" +
            "fileName=" + fileName + ", " +
            "files=" + files + ", " +
            "unpackedSize=" + unpackedSize + ", " +
            "packedSize=" + packedSize + ']';
    }
}
