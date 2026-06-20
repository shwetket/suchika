-- ==============================================================================
-- 00_bootstrap.sql
-- One-time manual setup. Run as a PostgreSQL superuser BEFORE starting any
-- Quarkus module. Flyway does NOT run this file — it is executed manually.
--
-- USAGE:
--   psql -U postgres -d postgres -f application/flyway/00_bootstrap.sql
--
-- WHAT THIS CREATES:
--   1. The application database (app_db)
--   2. A single application user (app_user) — used by all modules in dev
--   3. Five domain schemas: profile, wealth, household, health, projections
--   4. All grants needed for Flyway to run DDL and the app to run DML
--
-- PRODUCTION NOTE:
--   In production, replace the single app_user with per-domain roles
--   (profile_app_user, wealth_app_user, etc.) with least-privilege grants.
--   That is a v1.0 Security concern and intentionally out of scope here.
-- ==============================================================================


-- ==============================================================================
-- STEP 1: Create the database (run connected to postgres)
-- ==============================================================================

-- Run this block connected to the 'postgres' default database:
--   psql -U postgres -c "CREATE DATABASE app_db;"
-- Then connect to app_db before running the rest of this file.
--
-- If you prefer, uncomment and run directly:
-- CREATE DATABASE app_db;
-- \c app_db


-- ==============================================================================
-- STEP 2: Create application user
-- ==============================================================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'app_user') THEN
        CREATE ROLE app_user LOGIN PASSWORD 'local_dev_only';
    END IF;
END
$$;

-- Grant database connection
GRANT CONNECT ON DATABASE app_db TO app_user;


-- ==============================================================================
-- STEP 3: Create domain schemas
-- ==============================================================================

CREATE SCHEMA IF NOT EXISTS profile;
CREATE SCHEMA IF NOT EXISTS wealth;
CREATE SCHEMA IF NOT EXISTS household;
CREATE SCHEMA IF NOT EXISTS health;
CREATE SCHEMA IF NOT EXISTS projections;


-- ==============================================================================
-- STEP 4: Schema-level grants
-- app_user needs:
--   USAGE — to reference objects in the schema
--   CREATE — so Flyway can create tables and the flyway_schema_history table
-- ==============================================================================

GRANT USAGE, CREATE ON SCHEMA profile      TO app_user;
GRANT USAGE, CREATE ON SCHEMA wealth       TO app_user;
GRANT USAGE, CREATE ON SCHEMA household    TO app_user;
GRANT USAGE, CREATE ON SCHEMA health       TO app_user;
GRANT USAGE, CREATE ON SCHEMA projections  TO app_user;


-- ==============================================================================
-- STEP 5: Default privileges
-- Ensures that any table created by app_user (via Flyway migrations) is
-- automatically readable/writable by app_user going forward.
-- ==============================================================================

ALTER DEFAULT PRIVILEGES FOR ROLE app_user IN SCHEMA profile
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_user;

ALTER DEFAULT PRIVILEGES FOR ROLE app_user IN SCHEMA wealth
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_user;

ALTER DEFAULT PRIVILEGES FOR ROLE app_user IN SCHEMA household
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_user;

ALTER DEFAULT PRIVILEGES FOR ROLE app_user IN SCHEMA health
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_user;

ALTER DEFAULT PRIVILEGES FOR ROLE app_user IN SCHEMA projections
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_user;


-- ==============================================================================
-- STEP 6: Lock down public schema
-- Prevents accidental table creation in the default public schema.
-- ==============================================================================

REVOKE CREATE ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM PUBLIC;


-- ==============================================================================
-- STEP 7: Timezone — all timestamps are stored and displayed in IST (UTC+5:30).
-- This setting is overridden at the application level too (Hibernate jdbc.timezone)
-- so it stays IST regardless of the OS timezone of the server.
-- ==============================================================================

ALTER DATABASE app_db SET timezone = 'Asia/Kolkata';


-- ==============================================================================
-- VERIFICATION
-- After running this script, verify with:
--
--   \dn         → lists schemas (should show profile, wealth, household, health, projections)
--   \du         → lists roles (should show app_user)
-- ==============================================================================
