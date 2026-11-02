-- =============================================================================
-- Migration: V2__seed_account_admin.sql
-- Description: Seed initial Admin account
-- Credentials: admin@intellisupport.com / Admin@123
-- =============================================================================

INSERT INTO users (full_name, email, password, role, created_at, updated_at)
VALUES (
    'System Administrator',
    'admin@intellisupport.com',
    '$2a$10$cX3cF2P7LSQSilNcz6dvfuTH5XuhjN4D8GJjTss34coxJi9I.Pshm',
    'ADMIN',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO NOTHING;
