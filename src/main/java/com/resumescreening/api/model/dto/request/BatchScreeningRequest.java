package com.resumescreening.api.model.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BatchScreeningRequest {

    @NotNull(message = "Job posting ID is required")
    private Long jobPostingId;

    @NotEmpty(message = "Resume IDs list cannot be empty")
    private List<Long> resumeIds;
}