# CICD

## Pipeline purpose

This file documents the automation jobs and technical pipeline rules used by the project.

## Backend pipeline

- Compile Java backend:
  ```bash
  ./gradlew :application:finance:compileJava
  ```
- Run backend tests when available.
- Verify Flyway migrations do not fail on startup.

## Frontend pipeline

- Install dependencies:
  ```bash
  cd web
  npm install
  ```
- Generate the OpenAPI client after backend or contract changes:
  ```bash
  npm run generate:api
  ```
- Build the production frontend:
  ```bash
  npm run build
  ```

## API contract synchronization

- Keep frontend and backend OpenAPI contracts in sync.
- When OpenAPI changes, regenerate the client in `web/`.
- The generated client is stored in `web/src/api/generated/`.

## Commit and CI rules

- Always run `npm install` after changing `web/package.json`.
- Always run `npm run generate:api` after changing the backend API contract or OpenAPI file.
- Keep OpenAPI and frontend sync by regenerating the client whenever the API changes.
- Run `./gradlew clean` if Gradle sync or compile fails.

## Recommended CI jobs

- `backend-compile`: compile the backend Java code.
- `frontend-build`: install npm deps, generate API client, build the React app.
- `docs-check`: verify that documentation files are present and the README tree is updated.
