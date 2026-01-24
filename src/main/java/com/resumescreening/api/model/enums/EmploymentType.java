package com.resumescreening.api.model.enums;

import java.io.Serializable;

/**
 * Types of employment offered in job postings.
 */
public enum EmploymentType implements Serializable {
    FULL_TIME,      // Regular 9-5 job
    PART_TIME,      // Flexible hours
    CONTRACT,       // Fixed duration project
    INTERNSHIP      // Training/learning opportunity
}