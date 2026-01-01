package com.resumescreening.api.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts between Java List<String> and PostgreSQL TEXT[] (array).
 *
 * PROBLEM:
 * - JobPosting has: List<String> requiredSkills
 * - PostgreSQL has: TEXT[] column
 * - JPA doesn't know how to convert!
 *
 * SOLUTION:
 * - Store as comma-separated string: "Java,Spring,MySQL"
 * - Convert back to list when reading: ["Java", "Spring", "MySQL"]
 *
 * ALTERNATIVE APPROACH:
 * - Use PostgreSQL ARRAY type directly (more complex)
 * - Use JSON array (we'll do this for complex objects)
 *
 * WHY THIS APPROACH?
 * - Simple and portable
 * - Works with any database (not just PostgreSQL)
 * - Easy to debug: SELECT required_skills FROM job_postings
 *
 * BEST PRACTICE: @Converter(autoApply = false)
 * - We manually specify where to use this with @Convert annotation
 * - More control, less magic
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    /**
     * Convert Java List to Database String.
     *
     * Called when: Saving entity to database
     *
     * Example:
     * Java:     ["Spring Boot", "MySQL", "AWS"]
     * Database: "Spring Boot,MySQL,AWS"
     *
     * EDGE CASES:
     * - null list → null (database allows null)
     * - empty list → "" (empty string, still valid)
     * - skills with commas → URL encode? (current limitation)
     */
    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }

        // Join list with comma delimiter
        // Example: ["Java", "Spring"] → "Java,Spring"
        return String.join(",", attribute);
    }

    /**
     * Convert Database String to Java List.
     *
     * Called when: Loading entity from database
     *
     * Example:
     * Database: "Spring Boot,MySQL,AWS"
     * Java:     ["Spring Boot", "MySQL", "AWS"]
     *
     * EDGE CASES:
     * - null string → empty list (safer than null)
     * - empty string "" → empty list
     * - single skill "Java" → ["Java"]
     */
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