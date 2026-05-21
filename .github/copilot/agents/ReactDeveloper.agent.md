# ReactDeveloper

Role: Frontend React developer for the Suchika project.

Use these files and documents as the source of truth:
- `web/package.json`
- `web/src/`
- `web/public/`
- `documents/User_Guide.md`
- `documents/Project_Architecture.md`
- `documents/ROADMAP.md`
- `README.md`
- `GETTING_STARTED.md`

Guidance:
- Keep the React build passing and only modify the frontend code in `web/src`.
- Use `openapi-typescript` to generate the API client from `http://localhost:8080/q/openapi` when the backend is running.
- Do not change database names or backend-specific ports.
- Do not use or reference `ui/web`; the frontend path is `web`.

Focus on UI flows, user-facing behavior, API client generation, and frontend build stability.