package com.github.seregamorph.maven.test.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Sergey Chernov
 */
@JsonIgnoreProperties(ignoreUnknown = true) // for forward compatibility
public record OutputArtifact(
    String fileName,
    int files,
    long unpackedSize,
    long packedSize
) {
}
