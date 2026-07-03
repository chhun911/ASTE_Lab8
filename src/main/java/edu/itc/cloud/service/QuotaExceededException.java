package edu.itc.cloud.service;

/** Thrown when an upload would push a user's usage past their storage quota. */
public class QuotaExceededException extends RuntimeException {

    public QuotaExceededException(long requestedBytes, long freeBytes) {
        super(String.format(
                "Upload rejected: quota exceeded. Need %d MB but only %d MB free.",
                Math.round(requestedBytes / 1_048_576.0),
                Math.round(freeBytes / 1_048_576.0)));
    }
}
