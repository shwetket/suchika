-- ==============================================================================
-- 00_bootstrap.sql
-- Pre-Flyway one-time infrastructure setup.
-- Run ONCE as the postgres superuser BEFORE Flyway runs for the first time.
--
-- USAGE:
--   psql -U postgres -f 00_bootstrap.sql
--
-- After this script succeeds, configure Flyway to connect as:
--   URL      : jdbc:postgresql://localhost:5432/wealth_db
--   User     : flyway_admin
--   Password : <value set in STEP 1 — change before running>
--
-- ROLES CREATED:
--   flyway_admin     — DDL owner. Runs all Flyway migrations. Never used at runtime.
--   wealth_app_user  — Application runtime. Least-privilege DML only.
--                      Full per-table grants are applied in V4__.
-- ==============================================================================


-- ==============================================================================
-- STEP 1: Create roles
-- Idempotent: safe to re-run if script fails partway through.
-- ⚠  CHANGE BOTH PASSWORDS BEFORE RUNNING.
--    Do NOT commit real passwords to version control.
-- ==============================================================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'flyway_admin') THEN
        CREATE ROLE flyway_admin WITH
            LOGIN
            NOINHERIT
            NOSUPERUSER
            NOCREATEDB
            NOCREATEROLE
            PASSWORD 'changeme_flyway';
        RAISE NOTICE '[bootstrap] Role flyway_admin created.';
    ELSE
        RAISE NOTICE '[bootstrap] Role flyway_admin already exists — skipping.';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'wealth_app_user') THEN
        CREATE ROLE wealth_app_user WITH
            LOGIN
            NOINHERIT
            NOSUPERUSER
            NOCREATEDB
            NOCREATEROLE
            CONNECTION LIMIT 10
            PASSWORD 'changeme_app';
        RAISE NOTICE '[bootstrap] Role wealth_app_user created.';
    ELSE
        RAISE NOTICE '[bootstrap] Role wealth_app_user already exists — skipping.';
    END IF;
END $$;


-- ==============================================================================
-- STEP 2: Create the application database
-- If this fails because the DB already exists, drop it first:
--   DROP DATABASE IF EXISTS wealth_db;
-- ==============================================================================

CREATE DATABASE wealth_db
    WITH ENCODING = 'UTF8'
         TEMPLATE = template0;


-- ==============================================================================
-- STEP 3: Database-level access grants
-- Revoke the default PUBLIC connect permission before granting selectively.
-- ==============================================================================

REVOKE ALL ON DATABASE wealth_db FROM PUBLIC;

-- flyway_admin: needs CONNECT to run migrations.
-- Schema-level CREATE is granted in STEP 4 after connecting to wealth_db.
GRANT CONNECT ON DATABASE wealth_db TO flyway_admin;

-- wealth_app_user: CONNECT only at DB level.
-- Schema USAGE + table DML are granted in V4__ (after the tables exist).
GRANT CONNECT ON DATABASE wealth_db TO wealth_app_user;


-- ==============================================================================
-- STEP 4: Schema-level setup
-- Must be run while connected to wealth_db.
--
-- psql: the \connect directive below switches the active connection automatically.
-- Other clients: disconnect, reconnect to wealth_db as postgres, then run
--                the remaining statements manually.
-- ==============================================================================

\connect wealth_db

-- Remove PostgreSQL's default grant of CREATE on the public schema to all users.
-- Only flyway_admin should be able to create or modify objects.
REVOKE ALL ON SCHEMA public FROM PUBLIC;

-- flyway_admin needs full DDL rights on the schema to run migrations.
GRANT USAGE, CREATE ON SCHEMA public TO flyway_admin;

-- wealth_app_user: schema visibility only.
-- Object-level DML (SELECT/INSERT/UPDATE/DELETE) is granted in V4__.
GRANT USAGE ON SCHEMA public TO wealth_app_user;

-- ==============================================================================
-- STEP 5: Default privileges
-- Ensures any table flyway_admin creates in a FUTURE migration automatically
-- grants SELECT + INSERT to wealth_app_user.
-- Restrictive operations (UPDATE, DELETE on specific tables) are still applied
-- explicitly and intentionally in V4__.
-- ==============================================================================

ALTER DEFAULT PRIVILEGES FOR ROLE flyway_admin IN SCHEMA public
    GRANT SELECT, INSERT ON TABLES TO wealth_app_user;

ALTER DEFAULT PRIVILEGES FOR ROLE flyway_admin IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO wealth_app_user;


-- ==============================================================================
-- Done. You can now point Flyway at wealth_db and run the migrations:
--   flyway -url=jdbc:postgresql://localhost:5432/wealth_db \
--          -user=flyway_admin \
--          -password=changeme_flyway \
--          migrate
-- ==============================================================================
