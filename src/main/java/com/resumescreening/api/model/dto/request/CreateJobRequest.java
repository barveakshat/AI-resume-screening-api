package com.resumescreening.api.model.dto.request;

import com.resumescreening.api.model.enums.EmploymentType;
import com.resumescreening.api.model.enums.ExperienceLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateJobRequest {

    @NotBlank(message = "Job title is required")
    private String title;

    @NotBlank(message = "Job description is required")
    private String description;

    @NotEmpty(message = "At least one skill is required")
    private List<String> requiredSkills;

    @NotNull(message = "Experience level is required")
    private ExperienceLevel experienceLevel;

    @NotNull(message = "Employment type is required")
    private EmploymentType employmentType;

    private String location;
    private String salaryRange;

    @NotBlank(message = "Company name is required")
    private String companyName;
}