package com.resumescreening.api.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedResumeData {

    private String fullName;
    private String email;
    private String phone;
    private List<String> skills = new ArrayList<>();
    private Integer totalExperienceYears;
    private List<Experience> experience = new ArrayList<>();
    private List<Education> education = new ArrayList<>();
    private String summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Experience {
        private String title;
        private String company;
        private String duration;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Education {
        private String degree;
        private String institution;
        private String year;
        private String field;
    }
}