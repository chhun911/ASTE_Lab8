package edu.itc.cloud.service;

/** Thrown when registering with an email address that is already taken. */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Email already registered: " + email);
    }
}
