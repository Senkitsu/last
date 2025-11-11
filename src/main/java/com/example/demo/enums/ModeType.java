package com.example.demo.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ModeType {
    AUTO("auto"),
    ECO("eco"),
    COMFORT("comfort");

    private final String value;

    ModeType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ModeType fromString(String value) {
        for (ModeType type : ModeType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ModeType: " + value);
    }
}