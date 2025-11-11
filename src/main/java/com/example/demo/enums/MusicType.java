package com.example.demo.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MusicType {
    CLASSICAL("classical"),
    POP("pop"),
    HARD_ROCK("hard_rock");

    private final String value;

    MusicType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static MusicType fromString(String value) {
        for (MusicType type : MusicType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown MusicType: " + value);
    }
}