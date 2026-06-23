# Frontend Development Guidelines

| | |
|---|---|
| **Type** | Guideline |
| **Audience** | Frontend developers |
| **Status** | Active |
| **Last updated** | 2026-06-23 |

## Objective

Define the rules, patterns, and standards for all frontend work in `web/`. Rules here are enforced by ESLint, Prettier, and pre-commit hooks — a violation blocks the commit or CI build.

## Use Cases

- Before writing a new React component or hook — check structure rules in sections 1–4
- Before calling an API endpoint — see section 5 (generated client only, no raw `fetch`)
- Before adding CSS — see section 6 (Tailwind only)
- Before opening a PR — run through the pre-commit checklist in section 10

---

## 1. React Rules

### Component Structure
- Use functional components with hooks — no class components.
- Keep components under 200 lines.
- Destructure props in function signatures — don't pass entire objects.
- Use custom hooks for reusable logic — don't duplicate it across components.

```jsx
// Good
export const UserCard = ({ username, role }) => {
  const [loading, setLoading] = useState(false);
  return <div>{username}</div>;
};

// Bad — full object prop, business logic in component
export const UserCard = (props) => {
  return <div>{props.user.name}</div>;
};
```

---

## 2. Authentication & Authorization

- Use `useAuth()` hook for role checks.
- Wrap protected pages in `<ProtectedRoute>`.
- Check permissions before rendering sensitive UI.
- Never store sensitive data in localStorage — tokens only.

```jsx
// Protect a route
<ProtectedRoute requiredRole="admin">
  <AdminSettings />
</ProtectedRoute>

// Conditional rendering by role
const { hasRole } = useAuth();
{hasRole('admin') && <AdminLink />}
```

---

## 3. File & Folder Structure

```
src/
├── context/           ← Global state (Auth)
├── hooks/             ← Custom hooks only
├── components/        ← Reusable UI components (<100 lines each)
├── pages/
│   ├── Public/        ← No auth required
│   ├── User/          ← user + admin access
│   └── Admin/         ← admin only
├── api/               ← generated.ts (auto-generated) + custom clients
├── types/             ← TypeScript type definitions
├── utils/             ← Constants, formatters, validators
├── App.js             ← Router only
└── index.js           ← Entry point

public/
├── index.html
├── images/            ← All images (auto-copied from /assets at build time)
└── css/               ← Global CSS only (minimal)
```

---

## 4. State Management

- Use Context API for global state (auth, user).
- Use `useState` for local component state.
- Keep prop count under 5; use Context if more is needed.
- Do not use Redux, Zustand, or other state libraries (see PROP-005 for the open decision).

---

## 5. API Integration

- Use only the generated OpenAPI client (`src/api/generated.ts`) — no raw `fetch()` calls.
- Call API from pages or custom hooks — not from UI components.
- Handle loading and error states explicitly.

```jsx
export const useTransactions = () => {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getTransactions().then(setData).finally(() => setLoading(false));
  }, []);

  return { data, loading };
};
```

The frontend API base URL is configured via `REACT_APP_API_BASE_URL` (defaults to `http://localhost:8080` — the web-gateway BFF). Do not hardcode domain service ports (8081–8084) in frontend code.

When the backend contract changes:
```bash
cd web && npm run generate:api
```
Never hand-edit `src/api/generated.ts`.

---

## 6. Styling (Tailwind Only)

- Tailwind CSS classes only — no CSS modules, no inline `style={{}}`, no other CSS frameworks.
- Global styles go in `src/index.css` only.
- Responsive prefixes: `md:flex-col sm:px-2`.

```jsx
// Good
<button className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700">
  Submit
</button>

// Bad — inline style
<button style={{ background: 'blue' }}>Submit</button>
```

---

## 7. Code Quality

### ESLint (enforced)
- No unused variables or imports.
- No `console.log` in committed code.
- Import order: React first, then libraries, then local.
- Max line length: 100 characters.

### Prettier (auto-applied)
- 2-space indentation.
- Single quotes.
- Trailing commas in objects and arrays.
- Semicolons required.

### Commands
```bash
npm run lint          # Check for ESLint errors
npm run lint:fix      # Auto-fix ESLint errors
npm run format        # Auto-format with Prettier (run this before committing)
npm run format:check  # Check only (what CI runs)
```

---

## 8. Testing

- Test all user-facing flows (happy path).
- Test role-based access (user vs admin).
- Test error states (network fail, 403, etc.).
- Do not test component implementation details.

```bash
npm run test:ci   # Single run, no watch (matches CI)
npm test          # Watch mode for local development
```

---

## 9. E2E Testing (Playwright)

Use **Playwright** for end-to-end tests. Do not use Cypress or Selenium.

### Spec file location

All spec files live in `web/e2e/`. Config is `web/playwright.config.js` (Chromium only, headless).

### Running

```bash
cd web && npm run test:e2e          # headless — requires dev server on :3000
cd web && npm run test:e2e:headed   # visible browser for debugging
cd web && npm run test:e2e:report   # open HTML report from last run
```

The dev server (`npm start`) must be running before executing any E2E test. The gateway (`:8080`) is optional — page-load and navigation tests pass without a backend.

### Locator rules

- Use role-based locators: `getByRole`, `getByLabel`, `getByText` — **no CSS selectors**.
- Scope nav locators to the navigation landmark to avoid collisions with page content that shares the same label:

```js
// Good
const nav = page.getByRole('navigation');
await nav.getByRole('link', { name: 'Wealth' }).click();

// Bad — may match heading or breadcrumb with the same text
await page.getByRole('link', { name: 'Wealth' }).click();
```

### Test design rules

- Tests must pass when **only the dev server is running** (auth spec uses a demo fallback — no live auth backend required).
- Do not assert on specific data values from the database (row counts, amounts). Test page structure and navigation instead.
- Each spec file covers one domain area; keep specs independent — no shared state between test files.

See [E2E_TESTING](./E2E_TESTING.md) for the full test inventory and startup order for data tests.

---

## 10. Pre-Commit Checklist

- [ ] `npm run lint` passes
- [ ] `npm run format` applied
- [ ] `npm run test:ci` passes
- [ ] No `console.log()` in code
- [ ] No unused imports
- [ ] Auth checks on all protected pages
- [ ] Tailwind classes only — no CSS files or inline styles

---

## 11. Common Mistakes

| Bad | Fix |
|---|---|
| `fetch('/api/...')` | Use generated client from `src/api/generated.ts` |
| `style={{ color: 'red' }}` | Use Tailwind class `text-red-500` |
| `import './Component.css'` | Use Tailwind classes in JSX |
| `<Route path="/admin" element={<Admin />} />` | Wrap in `<ProtectedRoute requiredRole="admin">` |
| Storing user object in `useState` | Use `useAuth()` context |
| Calling API in component render | Call in `useEffect` or custom hook |
| Props drilling 5+ levels | Use Context API |
| Storing passwords in localStorage | Token only |

---

## 12. Troubleshooting

**Port 3000 already in use:**
```bash
# PowerShell
Get-Process node | Stop-Process -Force
# Or start on a different port
$env:PORT=3001; npm start
```

**ESLint errors:**
```bash
npm run lint:fix
```

**Prettier formatting issues:**
```bash
npm run format
```

**API generation fails — "gateway.yaml not found":**
Check that `application/contract/gateway.yaml` exists (relative to `web/`). The generate command reads `../application/contract/gateway.yaml`.

**Generated client type errors after contract change:**
```bash
cd web && npm run generate:api
```
