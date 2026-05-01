CREATE TABLE job_required_skills (
    job_posting_id BIGINT NOT NULL REFERENCES job_postings(id) ON DELETE CASCADE,
    skill_order INTEGER NOT NULL,
    skill VARCHAR(255) NOT NULL,
    PRIMARY KEY (job_posting_id, skill_order)
);

INSERT INTO job_required_skills (job_posting_id, skill_order, skill)
SELECT
    jp.id,
    split_values.ordinality::INTEGER - 1,
    TRIM(split_values.skill)
FROM job_postings jp
CROSS JOIN LATERAL regexp_split_to_table(COALESCE(jp.required_skills, ''), ',')
    WITH ORDINALITY AS split_values(skill, ordinality)
WHERE TRIM(split_values.skill) <> '';

ALTER TABLE job_postings DROP COLUMN required_skills;
