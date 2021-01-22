# --- Initial schema for questionnaire

# --- !Ups

CREATE TABLE Program (
    id   bigserial not null primary key,
    description varchar,
    department varchar,
    contact_email varchar
);

CREATE TABLE ProgramQuestionnaire (
    program_id bigserial REFERENCES Program,
    version bigserial,
    PRIMARY KEY (program_id, version)
);

CREATE TYPE question_type AS ENUM ('textbox', 'radio', 'file', 'slider', 'checkbox');
CREATE TYPE question_response_type AS ENUM ('int', 'float', 'string', 'file');
CREATE TYPE question_category AS ENUM ('demographics', 'income', 'misc'); -- TODO: Add more categories

CREATE TABLE Question (
    id bigserial not null primary key,
    name varchar,
    text_en varchar,
    text_es varchar,
    -- TODO: other languages here
    presentation_type question_type,
    response_type question_response_type,
    -- TODO: validation in our to-be-designed validation language.
    category question_category,
    validation varchar,
    repeated bool
);

CREATE TABLE QuestionnaireQuestions (
    program_id bigint,
    version bigint,
    question_id bigint REFERENCES Question,
    -- the sequence number of the question.  it's permissible for there to be gaps.
    -- question order should be determined in queries by `ORDER BY question_number`
    -- rather than displaying this number to users directly.
    question_number bigint,
    -- TODO: condition in our to-be-designed condition language.
    display_condition varchar,
    FOREIGN KEY (program_id, version) REFERENCES ProgramQuestionnaire,
    UNIQUE (program_id, version, question_number)
);

CREATE TABLE Applicants (
    id bigserial not null primary key
);

CREATE TABLE ApplicantResponse (
    question_id bigint REFERENCES Question,
    applicant_id bigint REFERENCES Applicants,
    -- e.g. {"type": "number", "value": 123}
    -- e.g. {"type": "duration", "value": "5s"}
    -- e.g. {"type": "string", "value": "foo bar"}
    -- e.g. {"type": "file", "value": "sharepoint/directory/uuid-of-uploaded-file"}
    response_value jsonb,
    -- the index of the response if the question is repeated.  should be sequential per applicant.
    response_index int
);

# --- !Downs

drop table if exists Program cascade;
drop table if exists Question cascade;
drop table if exists QuestionnaireQuestions cascade;
drop table if exists ApplicantResponse cascade;
drop table if exists ProgramQuestionnaire cascade;
drop table if exists Applicants cascade;
drop type if exists question_type;
drop type if exists question_response_type;
drop type if exists question_category;
