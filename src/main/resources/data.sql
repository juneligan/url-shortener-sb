INSERT INTO db_config (name, type, value, active, deleted, created_at, updated_at)
SELECT 'Password Attempts Label', 'PASSWORD_ATTEMPTS', '{"maxAttempts": 5, "resetMinutes": 2}', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (
    SELECT 1 FROM db_config WHERE type = 'PASSWORD_ATTEMPTS'
);

INSERT INTO db_config (name, type, value, active, deleted, created_at, updated_at)
SELECT 'OTP Attempts Label', 'OTP_ATTEMPTS', '{"maxAttempts": 5, "resetMinutes": 15}', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (
    SELECT 1 FROM db_config WHERE type = 'OTP_ATTEMPTS'
);