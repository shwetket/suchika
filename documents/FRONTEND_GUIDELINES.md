# Frontend Development Guidelines

> Rules and standards for web developers building the Suchika frontend.
> Enforced via ESLint, Prettier, and pre-commit hooks.

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

## 9. Pre-Commit Checklist

- [ ] `npm run lint` passes
- [ ] `npm run format` applied
- [ ] `npm run test:ci` passes
- [ ] No `console.log()` in code
- [ ] No unused imports
- [ ] Auth checks on all protected pages
- [ ] Tailwind classes only — no CSS files or inline styles

---

## 10. Common Mistakes

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

## 11. Troubleshooting

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
