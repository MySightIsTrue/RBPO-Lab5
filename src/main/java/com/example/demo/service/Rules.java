package com.example.demo.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class Rules {
    private Rules() {}

    static void require(boolean condition, String message) {
        if (!condition) throw new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    static void text(String value, String field) {
        if (value == null || value.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must not be blank");
    }

    static void initialStatus(String actual, String expected) {
        require(actual == null || expected.equals(actual), "Use business operations to change status");
    }

    static ResponseStatusException missing(String entity, Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, entity + " not found: " + id);
    }
}
