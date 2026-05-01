CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    company_name VARCHAR(255),
    designation VARCHAR(255),
    phone_number VARCHAR(255),
    is_email_verified BOOLEAN,
    is_active BOOLEAN,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE resumes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    file_type VARCHAR(255),
    file_size BIGINT,
    parsed_data JSONB,
    resume_title VARCHAR(255),
    extracted_text TEXT,
    uploaded_at TIMESTAMP NOT NULL,
    is_primary BOOLEAN
);

CREATE TABLE job_postings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    required_skills TEXT,
    experience_level VARCHAR(50),
    employment_type VARCHAR(50),
    location VARCHAR(255),
    salary_range VARCHAR(100),
    company_name VARCHAR(255),
    is_active BOOLEAN,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE applications (
    id BIGSERIAL PRIMARY KEY,
    job_posting_id BIGINT NOT NULL REFERENCES job_postings(id),
    candidate_id BIGINT NOT NULL REFERENCES users(id),
    resume_id BIGINT NOT NULL REFERENCES resumes(id),
    status VARCHAR(255) NOT NULL,
    cover_letter TEXT,
    applied_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    screened_at TIMESTAMP,
    CONSTRAINT uk_applications_job_candidate UNIQUE (job_posting_id, candidate_id)
);

CREATE TABLE screening_results (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL UNIQUE REFERENCES applications(id),
    job_posting_id BIGINT NOT NULL REFERENCES job_postings(id),
    overall_score INTEGER NOT NULL,
    recommendation VARCHAR(255) NOT NULL,
    skill_match_score INTEGER,
    experience_match_score INTEGER,
    education_match_score INTEGER,
    strengths TEXT,
    weaknesses TEXT,
    ai_analysis TEXT,
    created_at TIMESTAMP NOT NULL,
    processing_time_ms BIGINT
);

CREATE TABLE matched_skills (
    screening_result_id BIGINT NOT NULL REFERENCES screening_results(id),
    skill VARCHAR(255)
);

CREATE TABLE missing_skills (
    screening_result_id BIGINT NOT NULL REFERENCES screening_results(id),
    skill VARCHAR(255)
);

CREATE TABLE api_usage (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    endpoint VARCHAR(255) NOT NULL,
    request_count INTEGER,
    last_reset TIMESTAMP NOT NULL,
    CONSTRAINT unique_user_endpoint UNIQUE (user_id, endpoint)
);

CREATE INDEX idx_resumes_user_id ON resumes(user_id);
CREATE INDEX idx_job_postings_user_id ON job_postings(user_id);
CREATE INDEX idx_applications_candidate_id ON applications(candidate_id);
CREATE INDEX idx_applications_job_posting_id ON applications(job_posting_id);
CREATE INDEX idx_screening_results_job_posting_id ON screening_results(job_posting_id);
