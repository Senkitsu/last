// MusicType.java
package com.example.demo.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum MusicType {
    CLASSICAL, POP, HARD_ROCK;

    @JsonCreator
    public static MusicType fromString(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return MusicType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Недопустимый тип музыки: " + value);
        }
    }
}