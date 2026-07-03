-- Default admin user (password: admin123, BCrypt-hashed)
-- Change this in production!
INSERT INTO users (username, password_hash, role, created_at)
SELECT 'admin', '$2a$10$/bSs6AlXNxcrcgLbj3ZTEu7Cv/2Uv0ygxCDwr.rPuq774LzI7O3h.', 'ADMIN', now()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');
