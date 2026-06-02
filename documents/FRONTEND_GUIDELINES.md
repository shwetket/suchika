# Frontend Development Guidelines & Guardrails

> Rules and standards for web developers building Suchika frontend.
> Enforce via ESLint, Prettier, and pre-commit hooks.

---

## 1. **React Rules**

### Component Structure
- ✅ **Do:** Use functional components with hooks
- ❌ **Don't:** Use class components (legacy)
- ✅ **Do:** Keep components under 200 lines
- ❌ **Don't:** Pass entire objects as props (destructure)
- ✅ **Do:** Use custom hooks for reusable logic
- ❌ **Don't:** Duplicate logic across components

**Example:**
```jsx
// ✅ GOOD - Destructured props, hook, single responsibility
export const UserCard = ({ username, role }) => {
  const [loading, setLoading] = useState(false);
  
  return <div>{username}</div>;
};

// ❌ BAD - Full object, logic in component
export const UserCard = (props) => {
  // Logic here
  return <div>{props.user.name}</div>;
};
```

---

## 2. **Authentication & Authorization**

### Access Control
- ✅ **Do:** Use `useAuth()` hook for role checks
- ✅ **Do:** Wrap protected pages in `<ProtectedRoute>`
- ✅ **Do:** Check permissions before rendering sensitive UI
- ❌ **Don't:** Store sensitive data in localStorage (tokens only)
- ❌ **Don't:** Bypass auth checks with URL manipulation

**Example:**
```jsx
// ✅ GOOD - Protected route with role enforcement
<ProtectedRoute requiredRole="admin">
  <AdminSettings />
</ProtectedRoute>

// ✅ GOOD - Conditional rendering by role
const { hasRole } = useAuth();
{hasRole('admin') && <AdminLink />}

// ❌ BAD - No auth check
<Route path="/admin" element={<AdminSettings />} />
```

---

## 3. **File & Folder Structure**

Strict organization:

```
src/
├── context/           ← Global state (Auth, UI)
├── hooks/             ← Custom hooks only
├── components/        ← Reusable UI components
│   ├── Navigation.js
│   ├── ProtectedRoute.js
│   └── (keep small, <100 lines)
├── pages/             ← Full-page components
│   ├── Public/        ← No auth required
│   ├── User/          ← User+admin access
│   └── Admin/         ← Admin only
├── api/               ← API calls (generated + custom)
├── App.js             ← Router only (10 lines max)
├── index.js           ← Entry point
└── index.css          ← Global styles

public/
├── index.html         ← React root div only
├── favicon.ico
├── images/            ← All images (auto-copied)
└── css/               ← Global CSS (minimal)
```

### Rules:
- ❌ **Don't:** Create `.css` files for each component (use Tailwind only)
- ✅ **Do:** Keep Tailwind classes in JSX
- ✅ **Do:** Use `src/index.css` for global styles only
- ❌ **Don't:** Mix CSS-in-JS with Tailwind

---

## 4. **Props & State Management**

### Props
- ✅ **Do:** Destructure props in function signature
- ✅ **Do:** Prop types should match context/API
- ❌ **Don't:** Pass object when only 1-2 fields needed
- ✅ **Do:** Keep prop count under 5 (use context if more)

### State
- ✅ **Do:** Use Context API for global state (auth, user, theme)
- ✅ **Do:** Use useState for local component state
- ❌ **Don't:** Store server data in useState (fetch fresh)
- ❌ **Don't:** Use Redux, Zustand, or other libraries (keep it simple)

**Example:**
```jsx
// ✅ GOOD - Destructured, context for global
const { user, hasRole } = useAuth();
const [form, setForm] = useState({ name: '', email: '' });

// ❌ BAD - Full object prop, too many states
<UserForm user={userData} />
const [name, setName] = useState();
const [email, setEmail] = useState();
const [age, setAge] = useState();
```

---

## 5. **API Integration**

### Rules:
- ✅ **Do:** Use generated OpenAPI client (`src/api/generated.ts`)
- ✅ **Do:** Call API from pages/hooks, not components
- ✅ **Do:** Handle loading/error states explicitly
- ❌ **Don't:** Fetch data in component render
- ❌ **Don't:** Use raw `fetch()` (use generated client)
- ✅ **Do:** Cache API responses when appropriate

**Example:**
```jsx
// ✅ GOOD - API call in hook, UI component receives data
export const useTransactions = () => {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    // Call generated API client
    getTransactions().then(setData).finally(() => setLoading(false));
  }, []);
  
  return { data, loading };
};

// ✅ GOOD - Component receives processed data
export const Transactions = () => {
  const { data, loading } = useTransactions();
  return loading ? <Spinner /> : <List items={data} />;
};
```

---

## 6. **Styling Rules (Tailwind Only)**

### Required:
- ✅ **Do:** Use only Tailwind CSS classes
- ✅ **Do:** Follow Tailwind naming (`px-4 py-2 rounded-lg`)
- ✅ **Do:** Use responsive prefixes (`md:flex-col sm:px-2`)
- ✅ **Do:** Define colors in `tailwind.config.js` if custom
- ❌ **Don't:** Add inline styles (`style={{}}`)
- ❌ **Don't:** Create CSS modules or separate `.css` files
- ❌ **Don't:** Use Bootstrap, Material-UI, or other CSS frameworks

**Example:**
```jsx
// ✅ GOOD - Tailwind only
<button className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700">
  Submit
</button>

// ❌ BAD - Inline styles
<button style={{ background: 'blue', color: 'white' }}>Submit</button>

// ❌ BAD - Custom CSS
<button className="custom-btn">Submit</button>  // custom-btn in separate .css
```

---

## 7. **Code Quality & Linting**

### ESLint Rules (Enforced)
- No unused variables
- No console.log in production (warn only)
- No TODO comments without context
- Import order (React first, then libraries, then local)
- Max line length: 100 characters
- Max component size: 200 lines

### Prettier Rules (Auto-formatted)
- 2-space indentation
- Single quotes for strings
- Trailing commas in objects/arrays
- Semicolons required

### Pre-commit Hooks (Husky)
```bash
# Runs before commit:
npm run lint      # ESLint check
npm run format    # Prettier auto-format
```

---

## 8. **Error Handling**

### API Errors
```jsx
// ✅ GOOD - Explicit error handling
try {
  const data = await fetchData();
  setData(data);
} catch (error) {
  setError(error.message);
  console.error('Failed to fetch:', error);
}

// ✅ GOOD - Show error UI
{error && <ErrorBanner message={error} />}
```

### Permission Errors
```jsx
// ✅ GOOD - Redirect on permission denial
if (!hasRole('admin')) {
  return <Navigate to="/" />;
}

// ✅ GOOD - Hide admin UI
{hasRole('admin') && <AdminLink />}
```

---

## 9. **Testing Requirements**

- ✅ **Do:** Test all user-facing flows (happy path)
- ✅ **Do:** Test role-based access (user vs admin)
- ✅ **Do:** Test error states (network fail, 403, etc.)
- ❌ **Don't:** Test component implementation details

**Run tests:**
```bash
npm test
```

---

## 10. **Commit & Review Checklist**

Before pushing code:

- [ ] No console.log() left in code
- [ ] All imports used (no unused variables)
- [ ] ESLint: `npm run lint` passes
- [ ] Prettier: `npm run format` applied
- [ ] Components under 200 lines
- [ ] No inline styles (Tailwind only)
- [ ] Auth checks on protected pages
- [ ] Error states handled
- [ ] Tests pass: `npm test`

---

## 11. **Common Mistakes to Avoid**

| ❌ Mistake | ✅ Fix |
|---|---|
| Storing entire user object in state | Use `useAuth()` context |
| Calling API in component render | Move to useEffect or custom hook |
| Using CSS modules or `.css` files | Use Tailwind classes in JSX |
| Bypassing `ProtectedRoute` | Always use `<ProtectedRoute>` wrapper |
| Multiple role-based conditions | Use `hasRole('admin')` helper |
| Inline `style={{}}` props | Replace with Tailwind classes |
| Unused imports | Run `npm run lint --fix` |
| Props drilling 5+ levels | Use Context API instead |
| Raw `fetch()` calls | Use generated API client |
| Storing sensitive data in localStorage | Token only, never passwords/secrets |

---

## 12. **Getting Help**

- **ESLint/Prettier errors?** → Run `npm run lint --fix`
- **Build failing?** → Check `npm run build` output
- **Type errors?** → Regenerate API: `npm run generate:api`
- **Auth issues?** → Check `useAuth()` hook and `AuthContext`
- **Styling not working?** → Use Tailwind class names (no CSS files)

---

## Setup Commands

```bash
# Install dependencies
npm install

# Check code quality
npm run lint

# Auto-format code
npm run format

# Run tests
npm test

# Build for production
npm run build

# Generate API client from backend
npm run generate:api
```

**All guardrails are automated. Developers must follow before commit.**
