package edu.itc.cloud.support;

/** Tiny helpers for building file payloads of a known size in tests. */
public final class TestFiles {

    /** One mebibyte in bytes — the unit the quota is expressed in. */
    public static final long MB = 1_048_576L;

    private TestFiles() {
    }

    /** A {@code byte[]} of exactly {@code sizeBytes} length (filled with zeros). */
    public static byte[] bytesOf(long sizeBytes) {
        if (sizeBytes < 0 || sizeBytes > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("unsupported size: " + sizeBytes);
        }
        return new byte[(int) sizeBytes];
    }
}
