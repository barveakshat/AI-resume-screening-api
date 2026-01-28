package com.resumescreening.api.model.dto.response;

import com.resumescreening.api.model.entity.User;
import com.resumescreening.api.model.enums.EmploymentType;
import com.resumescreening.api.model.enums.ExperienceLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobPostingResponse implements Serializable {

    private Long id;
    private String title;
    private String description;
    private List<String> requiredSkills;
    private ExperienceLevel experienceLevel;
    private EmploymentType employmentType;
    private String location;
    private String salaryRange;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private String companyName;
    private Long recruiterId;
    private String recruiterName;
    private String recruiterEmail;

    // Statistics (optional)
    private Long totalCandidates;
    private Long strongFitCount;
}