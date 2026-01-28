package com.github.seregamorph.maven.test.storage;

/**
 * @author Sergey Chernov
 */
public final class WriteResult {

    private final int deletedFilesCount;

    public WriteResult(int deletedFilesCount) {
        this.deletedFilesCount = deletedFilesCount;
    }

    public int deletedFilesCount() {
        return deletedFilesCount;
    }

    @Override
    public String toString() {
        return "WriteResult{" +
            "deletedFilesCount=" + deletedFilesCount +
            '}';
    }
}
