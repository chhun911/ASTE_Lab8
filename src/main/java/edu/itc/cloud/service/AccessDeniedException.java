package edu.itc.cloud.service;

/**
 * Thrown when a user tries to touch a resource they do not own. Enforcing this
 * is the heart of per-user isolation.
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
