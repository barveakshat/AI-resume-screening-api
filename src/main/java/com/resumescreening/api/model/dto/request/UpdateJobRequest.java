package com.resumescreening.api.model.dto.request;

import com.resumescreening.api.model.enums.EmploymentType;
import com.resumescreening.api.model.enums.ExperienceLevel;
import lombok.Data;

import java.util.List;

@Data
public class UpdateJobRequest {

    private String title;
    private String description;
    private List<String> requiredSkills;
    private ExperienceLevel experienceLevel;
    private EmploymentType employmentType;
    private String location;
    private String salaryRange;
}