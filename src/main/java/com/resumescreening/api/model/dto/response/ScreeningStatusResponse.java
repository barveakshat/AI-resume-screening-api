package com.resumescreening.api.model.dto.response;

import com.resumescreening.api.model.enums.ScreeningStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreeningStatusResponse {
    private Long applicationId;
    private Long jobPostingId;
    private Long screeningResultId;
    private ScreeningStatus screeningStatus;
    private String message;
    private String screeningError;
    private LocalDateTime screeningRequestedAt;
    private LocalDateTime screeningCompletedAt;
}
