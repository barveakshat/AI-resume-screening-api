package com.resumescreening.api.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScreeningRequest {

    @NotNull(message = "Job posting ID is required")
    private Long jobPostingId;

    @NotNull(message = "Resume ID is required")
    private Long resumeId;
}