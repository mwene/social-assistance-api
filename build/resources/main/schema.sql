
-- Lookup table for configurable items
CREATE TABLE parameters (
    id SERIAL PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    value VARCHAR(50) NOT NULL,
    UNIQUE (category, value)
);

-- Counties table
CREATE TABLE counties (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- Sub-counties table
CREATE TABLE sub_counties (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    county_id INTEGER NOT NULL REFERENCES counties(id),
    UNIQUE (name, county_id)
);

-- Locations table
CREATE TABLE locations (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    sub_county_id INTEGER NOT NULL REFERENCES sub_counties(id),
    UNIQUE (name, sub_county_id)
);

-- Sub-locations table
CREATE TABLE sub_locations (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    location_id INTEGER NOT NULL REFERENCES locations(id),
    UNIQUE (name, location_id)
);

-- Villages table
CREATE TABLE villages (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    sub_location_id INTEGER NOT NULL REFERENCES sub_locations(id),
    UNIQUE (name, sub_location_id)
);

-- Applicants table
CREATE TABLE applicants (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    middle_name VARCHAR(50),
    last_name VARCHAR(50) NOT NULL,
    sex_id INTEGER NOT NULL REFERENCES parameters(id),
    age INTEGER CHECK (age >= 0),
    marital_status_id INTEGER NOT NULL REFERENCES parameters(id),
    id_number VARCHAR(20) NOT NULL UNIQUE,
    village_id INTEGER NOT NULL REFERENCES villages(id),
    postal_address VARCHAR(255),
    physical_address VARCHAR(255),
    telephone VARCHAR(20),
    verification_status VARCHAR(20) NOT NULL DEFAULT 'Pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Programmes table
CREATE TABLE programmes (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    verification_status VARCHAR(20) NOT NULL DEFAULT 'Active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Applications table
CREATE TABLE applications (
    id SERIAL PRIMARY KEY,
    applicant_id INTEGER NOT NULL REFERENCES applicants(id),
    programme_id INTEGER NOT NULL REFERENCES programmes(id),
    application_date DATE NOT NULL DEFAULT CURRENT_DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'Pending',
    maker_id INTEGER REFERENCES users(id),
    checker_id INTEGER REFERENCES users(id),
    approved BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (applicant_id, programme_id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Users table (updated with role clarification)
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('ROLE_ADMIN', 'ROLE_APPLICANT', 'ROLE_VERIFIER', 'ROLE_APPROVER', 'ROLE_DATA_COLLECTOR', 'ROLE_USER')),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Maker-Checker Logs
CREATE TABLE maker_checker_logs (
    id SERIAL PRIMARY KEY,
    entity_type VARCHAR(20) NOT NULL,
    entity_id INTEGER NOT NULL,
    action VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    maker_id INTEGER NOT NULL REFERENCES users(id),
    checker_id INTEGER REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_applications_applicant_id ON applications(applicant_id);
CREATE INDEX idx_applications_status ON applications(status);
CREATE INDEX idx_applicants_last_name ON applicants(last_name);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_maker_checker_logs_entity ON maker_checker_logs(entity_type, entity_id);

-- Stored Procedures

CREATE OR REPLACE PROCEDURE insert_application(
    p_first_name VARCHAR(50),
    p_middle_name VARCHAR(50),
    p_last_name VARCHAR(50),
    p_sex_id INTEGER,
    p_age INTEGER,
    p_marital_status_id INTEGER,
    p_id_number VARCHAR(20),
    p_village_id INTEGER,
    p_postal_address VARCHAR(255),
    p_physical_address VARCHAR(255),
    p_telephone VARCHAR(20),
    p_programme_id INTEGER
)
LANGUAGE plpgsql AS $$
DECLARE
    v_applicant_id INTEGER;
BEGIN
    INSERT INTO applicants (
        first_name, middle_name, last_name, sex_id, age, marital_status_id,
        id_number, village_id, postal_address, physical_address, telephone
    ) VALUES (
        p_first_name, p_middle_name, p_last_name, p_sex_id, p_age, p_marital_status_id,
        p_id_number, p_village_id, p_postal_address, p_physical_address, p_telephone
    ) RETURNING id INTO v_applicant_id;

    INSERT INTO applications (applicant_id, programme_id)
    VALUES (v_applicant_id, p_programme_id);
END;
$$;

CREATE OR REPLACE PROCEDURE verify_applicant(
    p_applicant_id INTEGER,
    p_user_id INTEGER,
    p_use_maker_checker BOOLEAN DEFAULT FALSE
)
LANGUAGE plpgsql AS $$
BEGIN
    IF p_use_maker_checker THEN
        UPDATE applicants
        SET verification_status = 'Proposed'
        WHERE id = p_applicant_id;

        INSERT INTO maker_checker_logs (entity_type, entity_id, action, status, maker_id)
        VALUES ('Applicant', p_applicant_id, 'Verify', 'Proposed', p_user_id);
    ELSE
        UPDATE applicants
        SET verification_status = 'Verified'
        WHERE id = p_applicant_id;
    END IF;
END;
$$;

CREATE OR REPLACE PROCEDURE approve_application(
    p_application_id INTEGER,
    p_user_id INTEGER,
    p_use_maker_checker BOOLEAN DEFAULT FALSE
)
LANGUAGE plpgsql AS $$
BEGIN
    IF p_use_maker_checker THEN
        UPDATE applications
        SET status = 'Proposed',
            maker_id = p_user_id
        WHERE id = p_application_id;

        INSERT INTO maker_checker_logs (entity_type, entity_id, action, status, maker_id)
        VALUES ('Application', p_application_id, 'Approve', 'Proposed', p_user_id);
    ELSE
        UPDATE applications
        SET status = 'Approved'
        WHERE id = p_application_id;
    END IF;
END;
$$;

CREATE OR REPLACE PROCEDURE confirm_maker_checker(
    p_log_id INTEGER,
    p_checker_id INTEGER,
    p_approve BOOLEAN
)
LANGUAGE plpgsql AS $$
DECLARE
    v_entity_type VARCHAR(20);
    v_entity_id INTEGER;
    v_action VARCHAR(20);
BEGIN
    SELECT entity_type, entity_id, action
    INTO v_entity_type, v_entity_id, v_action
    FROM maker_checker_logs
    WHERE id = p_log_id;

    IF v_entity_type = 'Applicant' AND v_action = 'Verify' THEN
        UPDATE applicants
        SET verification_status = CASE WHEN p_approve THEN 'Verified' ELSE 'Rejected' END
        WHERE id = v_entity_id;
    ELSIF v_entity_type = 'Application' AND v_action = 'Approve' THEN
        UPDATE applications
        SET status = CASE WHEN p_approve THEN 'Approved' ELSE 'Rejected' END,
            checker_id = p_checker_id
        WHERE id = v_entity_id;
    END IF;

    UPDATE maker_checker_logs
    SET status = CASE WHEN p_approve THEN 'Approved' ELSE 'Rejected' END,
        checker_id = p_checker_id,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_log_id;
END;
$$;

CREATE OR REPLACE PROCEDURE register_user(
    p_username VARCHAR(50),
    p_password VARCHAR(255),
    p_name VARCHAR(100),
    p_role VARCHAR(50)
)
LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO users (username, password, name, role)
    VALUES (p_username, p_password, p_name, p_role);
END;
$$;

CREATE OR REPLACE PROCEDURE change_user_password(
    p_user_id INTEGER,
    p_new_password VARCHAR(255)
)
LANGUAGE plpgsql AS $$
BEGIN
    UPDATE users
    SET password = p_new_password,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_user_id;
END;
$$;

-- Views

CREATE VIEW vw_applicant_details AS
SELECT
    a.id AS applicant_id,
    a.first_name,
    a.middle_name,
    a.last_name,
    p1.value AS sex,
    a.age,
    p2.value AS marital_status,
    a.id_number,
    v.name AS village,
    sl.name AS sub_location,
    l.name AS location,
    sc.name AS sub_county,
    c.name AS county,
    a.verification_status,
    app.id AS application_id,
    prog.name AS programme,
    app.application_date,
    app.status
FROM applicants a
JOIN parameters p1 ON a.sex_id = p1.id
JOIN parameters p2 ON a.marital_status_id = p2.id
JOIN villages v ON a.village_id = v.id
JOIN sub_locations sl ON v.sub_location_id = sl.id
JOIN locations l ON sl.location_id = l.id
JOIN sub_counties sc ON l.sub_county_id = sc.id
JOIN counties c ON sc.county_id = c.id
JOIN applications app ON a.id = app.applicant_id
JOIN programmes prog ON app.programme_id = prog.id;

-- Seed initial data
INSERT INTO parameters (category, value) VALUES
    ('Sex', 'Male'),
    ('Sex', 'Female'),
    ('MaritalStatus', 'Single'),
    ('MaritalStatus', 'Married'),
    ('MaritalStatus', 'Divorced'),
    ('MaritalStatus', 'Widowed');

INSERT INTO programmes (name) VALUES
    ('Orphans and vulnerable children'),
    ('Poor elderly persons'),
    ('Persons with disability'),
    ('Persons in extreme poverty'),
    ('Any other');

-- Seed users with different roles (password: "password123" hashed with BCrypt)
INSERT INTO users (username, password, name, role) VALUES
    ('admin', '$2a$10$XURPShQNCsLjp1ESc2laoObo9QZDhxz73hJPaEv7/cBha4pk0AgP.', 'Admin User', 'ROLE_ADMIN'),
    ('applicant1', '$2a$10$XURPShQNCsLjp1ESc2laoObo9QZDhxz73hJPaEv7/cBha4pk0AgP.', 'Applicant One', 'ROLE_APPLICANT'),
    ('verifier1', '$2a$10$XURPShQNCsLjp1ESc2laoObo9QZDhxz73hJPaEv7/cBha4pk0AgP.', 'Verifier One', 'ROLE_VERIFIER'),
    ('approver1', '$2a$10$XURPShQNCsLjp1ESc2laoObo9QZDhxz73hJPaEv7/cBha4pk0AgP.', 'Approver One', 'ROLE_APPROVER'),
    ('datacollector1', '$2a$10$XURPShQNCsLjp1ESc2laoObo9QZDhxz73hJPaEv7/cBha4pk0AgP.', 'Data Collector One', 'ROLE_DATA_COLLECTOR'),
    ('user1', '$2a$10$XURPShQNCsLjp1ESc2laoObo9QZDhxz73hJPaEv7/cBha4pk0AgP.', 'Basic User One', 'ROLE_USER');
