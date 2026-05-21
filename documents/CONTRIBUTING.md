# Contributing

## Commit rules

- Always run `npm install` after any changes to `web/package.json`.
- Always run `npm run generate:api` before committing changes.
- Keep OpenAPI and frontend sync by regenerating the client when the API changes.

## Code style

- Follow the Hexagonal Architecture guidelines in `documents/Project_Architecture.md`.
- Keep domain logic in `com.suchika.finance` and `com.suchika.health` packages.
- Do not introduce messaging or event bus code in this repo unless feature requirements explicitly call for it.

## Development workflow

1. Start the unified backend:
   ```bash
   ./gradlew :application:finance:quarkusDev
   ```
2. Start the frontend:
   ```bash
   cd web
   npm install
   npm start
   ```
3. If API contracts change, regenerate the client:
   ```bash
   npm run generate:api
   ```
