package com.example.demo.rest;

public class UnregisteredUserException extends RuntimeException {
    public UnregisteredUserException(String email) {
        super("A user with email " + email + " doesn't exist.");
    }
    public UnregisteredUserException() {
        super("A user with this email doesn't exist.");
    }
}