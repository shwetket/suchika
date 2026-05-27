# Master Business Requirements: Personal Operations System (Suchika)

## 1. Executive Summary
This document defines the overarching business requirements, architecture principles, and maturity milestones for the Suchika personal operations application. The system is designed to consolidate Wealth, Household, and Health data into a single, strictly organized repository. The ultimate long-term goal (v2.0+) is to enable a local Artificial Intelligence to seamlessly read, analyze, and automate personal operations based on highly structured, immutable data.

## 2. Core Principles & Assumptions
The following principles dictate all technical and business decisions regarding this system:

* **Ephemeral Test Data (Pre-v1.0):** Until the v1.0 (Security & Persistence) milestone is reached, all local database records are treated as volatile test data. The system is not required to handle complex data migrations or prevent corruption via edge cases early on, as the database will be wiped and reset frequently during local development.
* **AI Readiness via Data Hygiene:** Data quality is critical. Information must be strictly structured and categorized from Day 1 to ensure seamless ingestion by a future local LLM.
* **Strict Domain Isolation:** Data must be logically separated into distinct domains (Wealth, Household, Health) to prevent cross-contamination and ensure system stability.
* **Calculated Risk & Efficiency:** Core technical capabilities must be proven early with minimal scope (e.g., "Happy Path" in v0.1).
* **Declarative Documentation:** Acceptance Criteria are written as clear, declarative statements optimized for human readability, rather than strict BDD (Given/When/Then) syntax.

## 3. Versioning Approach & Roadmap
The application follows a granular maturity roadmap. Core features are introduced in early stages, with error handling, security, and external integrations strictly deferred to later milestones.

| Version | Stage | Key Focus |
| :--- | :--- | :--- |
| **v0.1** | Prototype | Minimal core capabilities, happy path execution only. |
| **v0.2** | Usable Local App | Introduction of basic logical rules and usable features. |
| **v0.3** | Enhanced Local App | Expansion of features and data parsing capabilities. |
| **v0.4** | Error Handling | Unhappy path, edge cases, and malformed data rejection. |
| **v0.5** | Beta Release | Stable build for controlled local testing. |
| **v0.6** | Testing Foundation | Implementation of automated test coverage. |
| **v1.0** | Security | Authentication, encryption, and transition to persistent, real data. |
| **v1.1** | Multi-User | User accounts and role-based access. |
| **v1.2** | Public Local Release | Stable local release for general users. |
| **v1.3** | Export / Import | Advanced data framework management. |
| **v2.0** | Local AI | Integration of AI-powered features and data synthesis. |
| **v2.1** | Cloud Ready | Architectural preparation for cloud deployment. |
| **v2.2** | Mobile App | Development of a companion mobile application. |
| **v3.0** | GitHub Ready | Open-source collaboration readiness. |
| **v3.1** | Integrations | Google Drive, Calendar, Fitbit, etc. |
| **v3.2** | Plugin Framework | System extensibility. |
| **v3.3** | Marketplace | Development of a plugin/module ecosystem. |
| **v4.0** | Cloud Launch | Full commercial cloud deployment. |
| **v4.1** | Commercial Launch | Licensing, regulatory compliance, and billing. |

## 4. Domain Definitions & Document Hierarchy
Detailed business rules, epics, and version-specific Acceptance Criteria are maintained in domain-specific child documents. These files act as living documents that represent the accumulative state of the system.

* **Wealth & Asset Management:** `documents/REQUIREMENTS_wealth_domain.md`
  * *Focus:* Financial liquidity, transaction ledgers, and physical asset lifecycle compliance.
* **Household Operations:** `documents/REQUIREMENTS_household_domain.md`
  * *Focus:* Scheduling, human logistics, task execution, supply chain (groceries), and home infrastructure automation.
* **Health & Biometrics:** `documents/REQUIREMENTS_health_domain.md`
  * *Focus:* Unstructured time-series biometric tracking and fitness profiles.
* **Cross-Domain Logic:** `documents/REQUIREMENTS_cross_domain.md`
  * *Focus:* Features requiring read-access across multiple isolated domains (e.g., Vacation Planning requiring Calendar, Vehicle, and Finance data).

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
