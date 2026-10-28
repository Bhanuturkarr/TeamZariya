CREATE TABLE topic_prerequisites (
    id UUID PRIMARY KEY,
    topic_id UUID NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    prerequisite_topic_id UUID NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    source VARCHAR(32) NOT NULL CHECK (source IN ('AI_EXTRACTED', 'SOURCE_ORDER_FALLBACK')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_topic_prerequisite UNIQUE (topic_id, prerequisite_topic_id),
    CONSTRAINT ck_topic_prerequisite_not_self CHECK (topic_id <> prerequisite_topic_id)
);

CREATE INDEX idx_topic_prerequisites_topic ON topic_prerequisites (topic_id);

CREATE TABLE learning_plans (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES learning_projects(id) ON DELETE CASCADE,
    material_id UUID NOT NULL REFERENCES materials(id) ON DELETE CASCADE,
    assessment_id UUID NOT NULL REFERENCES assessments(id) ON DELETE CASCADE,
    assessment_attempt_id UUID NOT NULL REFERENCES assessment_attempts(id) ON DELETE CASCADE,
    available_study_minutes INTEGER NOT NULL CHECK (available_study_minutes BETWEEN 10 AND 600),
    learning_preference VARCHAR(500),
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'COMPLETED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_learning_plans_project_created ON learning_plans (project_id, created_at DESC);

CREATE TABLE learning_plan_items (
    id UUID PRIMARY KEY,
    plan_id UUID NOT NULL REFERENCES learning_plans(id) ON DELETE CASCADE,
    topic_id UUID REFERENCES topics(id) ON DELETE SET NULL,
    topic_name VARCHAR(160) NOT NULL,
    sequence_number INTEGER NOT NULL CHECK (sequence_number > 0),
    estimated_minutes INTEGER NOT NULL CHECK (estimated_minutes > 0),
    rationale TEXT NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'COMPLETED')),
    CONSTRAINT uq_learning_plan_item_sequence UNIQUE (plan_id, sequence_number)
);

CREATE INDEX idx_learning_plan_items_plan_sequence ON learning_plan_items (plan_id, sequence_number);

CREATE TABLE lessons (
    id UUID PRIMARY KEY,
    plan_item_id UUID NOT NULL REFERENCES learning_plan_items(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    objective TEXT NOT NULL,
    explanation TEXT NOT NULL,
    worked_example TEXT NOT NULL,
    source_page INTEGER NOT NULL CHECK (source_page > 0),
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_lessons_plan_item ON lessons (plan_item_id);

CREATE TABLE exercises (
    id UUID PRIMARY KEY,
    lesson_id UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    question_text VARCHAR(1000) NOT NULL,
    option_a TEXT NOT NULL,
    option_b TEXT NOT NULL,
    option_c TEXT NOT NULL,
    option_d TEXT NOT NULL,
    correct_option_index INTEGER NOT NULL CHECK (correct_option_index BETWEEN 0 AND 3),
    explanation TEXT NOT NULL,
    difficulty VARCHAR(8) NOT NULL CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    source_page INTEGER NOT NULL CHECK (source_page > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_exercises_lesson_created ON exercises (lesson_id, created_at);

CREATE TABLE topic_mastery (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES learning_projects(id) ON DELETE CASCADE,
    topic_id UUID REFERENCES topics(id) ON DELETE SET NULL,
    topic_name VARCHAR(160) NOT NULL,
    diagnostic_attempt_id UUID REFERENCES assessment_attempts(id) ON DELETE SET NULL,
    diagnostic_attempts INTEGER NOT NULL DEFAULT 0 CHECK (diagnostic_attempts >= 0),
    diagnostic_correct INTEGER NOT NULL DEFAULT 0 CHECK (diagnostic_correct BETWEEN 0 AND diagnostic_attempts),
    practice_attempts INTEGER NOT NULL DEFAULT 0 CHECK (practice_attempts >= 0),
    practice_correct INTEGER NOT NULL DEFAULT 0 CHECK (practice_correct BETWEEN 0 AND practice_attempts),
    mastery_score INTEGER NOT NULL CHECK (mastery_score BETWEEN 0 AND 100),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_topic_mastery_project_name UNIQUE (project_id, topic_name)
);

CREATE INDEX idx_topic_mastery_project_score ON topic_mastery (project_id, mastery_score);

CREATE TABLE exercise_attempts (
    id UUID PRIMARY KEY,
    exercise_id UUID NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    selected_option_index INTEGER NOT NULL CHECK (selected_option_index BETWEEN 0 AND 3),
    is_correct BOOLEAN NOT NULL,
    mastery_before INTEGER NOT NULL CHECK (mastery_before BETWEEN 0 AND 100),
    mastery_after INTEGER NOT NULL CHECK (mastery_after BETWEEN 0 AND 100),
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_exercise_attempts_exercise_submitted ON exercise_attempts (exercise_id, submitted_at DESC);
