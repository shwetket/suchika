# API Reference

## Overview

The unified Quarkus application exposes Finance and Health APIs from a single backend.

Base URL: `http://localhost:8080`
OpenAPI spec: `http://localhost:8080/q/openapi`

## Finance Endpoints

### Accounts
- `GET /api/v1/accounts` — list accounts
- `POST /api/v1/accounts` — create account
- `GET /api/v1/accounts/{account_id}` — get account
- `PATCH /api/v1/accounts/{account_id}` — update account
- `DELETE /api/v1/accounts/{account_id}` — deactivate account

### Transactions
- `POST /api/v1/transactions:uploadCsv` — upload a CSV file
- `GET /api/v1/transactions` — list transactions
- `GET /api/v1/transactions:config` — get dropdown config values

## Health Endpoints

### Health Profiles
- `GET /api/v1/health-profiles` — list health profiles
- `POST /api/v1/health-profiles` — create health profile
- `GET /api/v1/health-profiles/{profile_id}` — get health profile
- `PATCH /api/v1/health-profiles/{profile_id}` — update health profile
- `DELETE /api/v1/health-profiles/{profile_id}` — delete health profile

## Notes
- The frontend syncs with the runtime OpenAPI spec using `npm run generate:api`.
- All endpoints are served from the single Quarkus application in `application/finance`.
