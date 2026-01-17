package com.resumescreening.api.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }

        // Join list with comma delimiter
        // Example: ["Java", "Spring"] → "Java,Spring"
        return String.join(",", attribute);
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return List.of(); // Return empty immutable list
        }

        // Split by comma and trim whitespace
        // Example: "Java, Spring " → ["Java", "Spring"]
        return Arrays.stream(dbData.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }
}