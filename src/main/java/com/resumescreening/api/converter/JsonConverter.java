package com.resumescreening.api.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts between Java Objects and PostgreSQL JSONB.
 *
 * PROBLEM:
 * - Resume has: ParsedResumeData parsedData (complex object)
 * - PostgreSQL has: JSONB column
 * - JPA doesn't know how to convert!
 *
 * SOLUTION:
 * - Use Jackson (JSON library included with Spring Boot)
 * - Serialize object to JSON string
 * - Deserialize JSON string back to object
 *
 * WHY JSONB over JSON?
 * - JSONB is binary, faster queries
 * - Can index JSONB fields: CREATE INDEX ON resumes USING GIN(parsed_data)
 * - Can query inside: SELECT * WHERE parsed_data->>'email' = 'test@example.com'
 *
 * USAGE:
 * @Convert(converter = JsonConverter.class)
 * @Column(columnDefinition = "jsonb")
 * private ParsedResumeData parsedData;
 */
@Converter
public class JsonConverter implements AttributeConverter<Object, String> {

    private static final Logger logger = LoggerFactory.getLogger(JsonConverter.class);

    /**
     * ObjectMapper: Jackson's main class for JSON operations.
     *
     * BEST PRACTICE: Static + configured once
     * - Creating ObjectMapper is expensive
     * - Reuse same instance for all conversions
     *
     * JavaTimeModule:
     * - Handles LocalDateTime, LocalDate, etc.
     * - Without it: Error converting dates to JSON
     */
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule()); // Support Java 8 Date/Time

    /**
     * Convert Java Object to JSON String.
     *
     * Example:
     * Java Object:
     * ParsedResumeData {
     *   fullName: "Akshat Barve",
     *   email: "test@example.com",
     *   skills: ["Java", "Spring"]
     * }
     *
     * Database (JSONB):
     * {
     *   "fullName": "Akshat Barve",
     *   "email": "test@example.com",
     *   "skills": ["Java", "Spring"]
     * }
     *
     * ERROR HANDLING:
     * - If serialization fails → log error, return null
     * - Alternative: throw exception (fail-fast approach)
     */
    @Override
    public String convertToDatabaseColumn(Object attribute) {
        if (attribute == null) {
            return null;
        }

        try {
            // Convert object to JSON string
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            logger.error("Error converting object to JSON", e);
            // DESIGN DECISION: Return null instead of throwing
            // - Allows save to proceed (data partially lost)
            // - Alternative: throw RuntimeException (fail-fast)
            return null;
        }
    }

    /**
     * Convert JSON String to Java Object.
     *
     * CHALLENGE: We don't know the target type!
     * - Could be ParsedResumeData
     * - Could be List<String>
     * - Could be any object
     *
     * SOLUTION: Return generic Object
     * - JPA will cast to correct type
     * - If wrong type → ClassCastException (caught early)
     *
     * ERROR HANDLING:
     * - Invalid JSON → log error, return null
     * - Entity will have null field (graceful degradation)
     */
    @Override
    public Object convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }

        try {
            // Parse JSON string to Object
            // Returns LinkedHashMap for objects, ArrayList for arrays
            return objectMapper.readValue(dbData, Object.class);
        } catch (JsonProcessingException e) {
            logger.error("Error converting JSON to object", e);
            return null;
        }
    }
}