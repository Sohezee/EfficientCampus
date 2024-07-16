package com.example.demo.rest;

public class EmailExistsException extends RuntimeException {
    public EmailExistsException(String email) {
        super("A user with email " + email + " already exists.");
    }
}