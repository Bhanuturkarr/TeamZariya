CREATE TABLE assessments (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES learning_projects(id) ON DELETE CASCADE,
    material_id UUID NOT NULL REFERENCES materials(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_assessments_project_created_at
    ON assessments (project_id, created_at DESC);

CREATE INDEX idx_assessments_material
    ON assessments (material_id);

CREATE TABLE assessment_questions (
    id UUID PRIMARY KEY,
    assessment_id UUID NOT NULL REFERENCES assessments(id) ON DELETE CASCADE,
    topic_id UUID REFERENCES topics(id) ON DELETE SET NULL,
    topic_name VARCHAR(160) NOT NULL,
    question_order INTEGER NOT NULL CHECK (question_order > 0),
    question_text VARCHAR(1000) NOT NULL,
    option_a TEXT NOT NULL,
    option_b TEXT NOT NULL,
    option_c TEXT NOT NULL,
    option_d TEXT NOT NULL,
    correct_option_index INTEGER NOT NULL CHECK (correct_option_index BETWEEN 0 AND 3),
    explanation TEXT NOT NULL,
    difficulty VARCHAR(8) NOT NULL CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    source_page INTEGER NOT NULL CHECK (source_page > 0),
    CONSTRAINT uq_assessment_question_order UNIQUE (assessment_id, question_order)
);

CREATE INDEX idx_assessment_questions_assessment
    ON assessment_questions (assessment_id, question_order);

CREATE TABLE assessment_attempts (
    id UUID PRIMARY KEY,
    assessment_id UUID NOT NULL REFERENCES assessments(id) ON DELETE CASCADE,
    attempt_number INTEGER NOT NULL CHECK (attempt_number > 0),
    score_percent INTEGER NOT NULL CHECK (score_percent BETWEEN 0 AND 100),
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_assessment_attempt_number UNIQUE (assessment_id, attempt_number)
);

CREATE INDEX idx_assessment_attempts_assessment_submitted
    ON assessment_attempts (assessment_id, submitted_at DESC);

CREATE TABLE assessment_answers (
    id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL REFERENCES assessment_attempts(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES assessment_questions(id) ON DELETE CASCADE,
    selected_option_index INTEGER NOT NULL CHECK (selected_option_index BETWEEN 0 AND 3),
    is_correct BOOLEAN NOT NULL,
    answered_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_assessment_answer_question UNIQUE (attempt_id, question_id)
);

CREATE INDEX idx_assessment_answers_attempt
    ON assessment_answers (attempt_id);
