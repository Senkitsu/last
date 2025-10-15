package com.example.demo.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ModeType {
    ECO, AUTO, COMFORT;

    @JsonCreator
    public static ModeType fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ModeType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Недопустимый режим: " + value + ". Допустимые: ECO, AUTO, COMFORT");
        }
    }
}