package com.resumescreening.api.model.dto.response;

import com.resumescreening.api.model.dto.ParsedResumeData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeResponse {

    private Long id;
    private String fileName;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private ParsedResumeData parsedData;
    private LocalDateTime uploadDate;
}