package com.github.seregamorph.maven.test;

import com.github.seregamorph.maven.test.extension.SurefireCachedMojoTest;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public final class TestFileUtils {

    public static File getResourceFile(String name) {
        return new File(getResourceURI(name));
    }

    private static URI getResourceURI(String name) {
        URL resource = SurefireCachedMojoTest.class.getClassLoader().getResource(name);
        if (resource == null) {
            throw new RuntimeException("Resource not found: " + name);
        }
        try {
            return resource.toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private TestFileUtils() {
    }
}
