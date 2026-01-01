package com.resumescreening.api.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.resumescreening.api.model.dto.ParsedResumeData;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Type-safe converter specifically for ParsedResumeData.
 *
 * ADVANTAGE over generic JsonConverter:
 * - Knows exact type to deserialize
 * - Better error messages
 * - Type-safe at compile time
 *
 * TRADE-OFF:
 * - Need separate converter for each type
 * - More code, but safer
 */
@Converter
public class ParsedResumeDataConverter implements AttributeConverter<ParsedResumeData, String> {

    private static final Logger logger = LoggerFactory.getLogger(ParsedResumeDataConverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public String convertToDatabaseColumn(ParsedResumeData attribute) {
        if (attribute == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            logger.error("Error converting ParsedResumeData to JSON", e);
            return null;
        }
    }

    @Override
    public ParsedResumeData convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }

        try {
            // Type-safe: Returns ParsedResumeData directly
            return objectMapper.readValue(dbData, ParsedResumeData.class);
        } catch (JsonProcessingException e) {
            logger.error("Error converting JSON to ParsedResumeData", e);
            return null;
        }
    }
}