package com.resumescreening.api.model.dto.response;

import com.resumescreening.api.model.enums.ApplicationStatus;
import com.resumescreening.api.model.enums.ScreeningStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationResponse {
    private Long id;
    private Long jobId;
    private String jobTitle;
    private Long candidateId;
    private String candidateName;
    private String candidateEmail;
    private Long resumeId;
    private String resumeTitle;
    private ApplicationStatus status;
    private String coverLetter;
    private LocalDateTime appliedAt;
    private LocalDateTime screenedAt;
    private ScreeningStatus screeningStatus;
    private String screeningError;
    private LocalDateTime screeningRequestedAt;
    private LocalDateTime screeningCompletedAt;
    private ScreeningResultResponse screeningResult;
}
