package edu.itc.cloud.service;

/** Thrown when a requested resource does not exist for the current user. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
