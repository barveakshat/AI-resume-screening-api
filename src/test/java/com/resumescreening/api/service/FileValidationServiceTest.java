package com.resumescreening.api.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileValidationServiceTest {

    private final FileValidationService validationService = new FileValidationService();

    @Test
    void rejectsLegacyDocFilesBecauseExtractorSupportsOnlyPdfAndDocx() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.doc",
                "application/msword",
                "content".getBytes()
        );

        assertThatThrownBy(() -> validationService.validateFile(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not allowed");
    }
}
