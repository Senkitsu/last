package com.example.demo.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SensorType {
    CO2("co2"),
    HUMIDITY("humidity"),
    TEMP("temp"),
    LIGHT("light");

    private final String value;

    SensorType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static SensorType fromString(String value) {
        for (SensorType type : SensorType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown SensorType: " + value);
    }
}