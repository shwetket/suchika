# Suchika Web Frontend

React frontend for Suchika personal operations system with role-based access control.

---

## 📋 Quick Start

```bash
# Install dependencies
npm install

# Start development server
npm start
# Opens http://localhost:3000

# Run tests
npm test

# Build for production
npm run build
```

---

## 🛡️ Guardrails & Standards

**All developers must follow [FRONTEND_GUIDELINES.md](../../documents/FRONTEND_GUIDELINES.md)**

### Code Quality Tools (Enforced)
- **ESLint:** Catches bugs, enforces rules
- **Prettier:** Auto-formats code
- **Pre-commit hooks:** Run linting before committing

### Quick Commands

```bash
# Check code for issues
npm run lint

# Auto-fix linting issues
npm run lint:fix

# Auto-format code
npm run format

# Check formatting (no changes)
npm run format:check
```

---

## 🏗️ Project Structure

```
src/
├── App.js              ← Router setup (DO NOT modify core flow)
├── index.js            ← Entry point
├── context/
│   └── AuthContext.js  ← Global auth state (role, user, token)
├── hooks/
│   └── useAuth.js      ← Custom hook for auth
├── components/
│   ├── Navigation.js   ← App navigation bar
│   └── ProtectedRoute.js ← Auth guard for pages
├── pages/
│   ├── Public/         ← No auth required
│   │   ├── Home.js
│   │   ├── About.js
│   │   ├── SignIn.js
│   │   └── SignUp.js
│   ├── User/           ← Requires user+ role
│   │   ├── Dashboard.js
│   │   ├── Transactions.js
│   │   └── Health.js
│   └── Admin/          ← Requires admin role
│       ├── AdminUsers.js
│       ├── AdminSettings.js
│       └── AdminReports.js
└── api/                ← API client (auto-generated)

public/
├── index.html          ← React root element only
├── images/             ← All images (auto-copied from /assets)
└── css/                ← Global CSS only
```

---

## 🔐 Authentication & Authorization

### Roles
- **public:** Unauthenticated users (Home, About, SignIn, SignUp)
- **user:** Regular users (Dashboard, Transactions, Health)
- **admin:** Administrators (User Management, Settings, Reports)

### Sign In Demo
1. Go to `/signin`
2. Enter username: anything (e.g., "john", "admin")
3. Select role: "User" or "Admin"
4. Redirects to `/dashboard`

### Access Control

**Check role in component:**
```jsx
import { useAuth } from '../hooks/useAuth';

export const SomeComponent = () => {
  const { user, hasRole } = useAuth();
  
  // Show admin UI only
  {hasRole('admin') && <AdminLink />}
  
  // Redirect if not authorized
  if (!hasRole('user')) {
    return <Navigate to="/" />;
  }
};
```

**Protect page with router:**
```jsx
<ProtectedRoute requiredRole="admin">
  <AdminUsers />
</ProtectedRoute>
```

---

## 🎨 Styling (Tailwind CSS Only)

- ✅ **Use:** `className="px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-700"`
- ❌ **Don't:** Create `.css` files for components
- ❌ **Don't:** Use inline `style={{}}` props

**Global styles:** Edit `src/index.css` (minimal)

---

## 🔌 API Integration

### Generate API Client
When backend contract changes:
```bash
npm run generate:api
```
Generates typed client at `src/api/generated.ts` from OpenAPI spec.

### Use Generated Client
```jsx
import { getTransactions } from '../api/generated';

export const useTransactions = () => {
  const [data, setData] = useState([]);
  
  useEffect(() => {
    getTransactions().then(setData);
  }, []);
  
  return data;
};
```

---

## 🚀 Build & Deploy

### Development
```bash
npm start
```

### Production Build
```bash
npm run build
# Outputs to: web/build/
```

### Run Checks Before Committing
```bash
npm run lint:fix    # Fix linting issues
npm run format      # Format code
npm test            # Run tests
```

---

## ❌ Common Mistakes

| ❌ | ✅ |
|---|---|
| Storing user object in useState | Use `useAuth()` context |
| Creating component.css files | Use Tailwind classes |
| Using inline `style={{}}` | Use Tailwind only |
| Bypassing `<ProtectedRoute>` | Always protect pages |
| Raw `fetch()` calls | Use generated API client |
| Storing passwords in localStorage | Token only |
| Passing 5+ props to component | Use Context API |
| Not checking `hasRole()` | Always verify permissions |

---

## 📚 Documentation

- **Guidelines:** [FRONTEND_GUIDELINES.md](../../documents/FRONTEND_GUIDELINES.md)
- **Business Rules:** [BUSINESS_REQUIREMENTS.md](../../documents/BUSINESS_REQUIREMENTS.md)
- **Architecture:** [ARCHITECTURE_GUIDELINES.md](../../documents/ARCHITECTURE_GUIDELINES.md)
- **React Router:** https://reactrouter.com/
- **Tailwind CSS:** https://tailwindcss.com/
- **React Hooks:** https://react.dev/reference/react/hooks

---

## 🐛 Troubleshooting

### Port 3000 already in use
```bash
# Kill node processes
Get-Process node | Stop-Process -Force

# Or use different port
PORT=3001 npm start
```

### ESLint errors
```bash
npm run lint:fix
```

### Prettier formatting issues
```bash
npm run format
```

### API generation fails
- Ensure backend is running at `http://localhost:8080`
- Check OpenAPI endpoint: `http://localhost:8080/q/openapi`

---

## ✅ Pre-commit Checklist

Before `git push`:

- [ ] `npm run lint:fix` passes
- [ ] `npm run format` applied
- [ ] `npm test` passes (if applicable)
- [ ] No console.log() in code
- [ ] No unused imports
- [ ] Auth checks on protected pages
- [ ] Tailwind classes only (no CSS files)

---

## 📞 Need Help?

- Check [FRONTEND_GUIDELINES.md](../../documents/FRONTEND_GUIDELINES.md) for standards
- Run `npm run lint:fix` to fix auto-fixable issues
- Ensure backend is running for API calls
