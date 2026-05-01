package com.resumescreening.api.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchScreeningResponse {
    private Long jobPostingId;
    private int totalApplications;
    private int queuedCount;
    private int skippedCount;
    private int failedCount;
    private List<ScreeningStatusResponse> applications;
}
