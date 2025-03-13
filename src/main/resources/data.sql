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
