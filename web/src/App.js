import React, { useEffect, useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { SetupGate } from './components/SetupGate';
import { Navigation } from './components/Navigation';

// Public Pages
const Home = React.lazy(() => import('./pages/Public/Home').then((m) => ({ default: m.Home })));
const About = React.lazy(() => import('./pages/Public/About').then((m) => ({ default: m.About })));
const Help = React.lazy(() => import('./pages/Public/Help').then((m) => ({ default: m.Help })));
const SignIn = React.lazy(() =>
  import('./pages/Public/SignIn').then((m) => ({ default: m.SignIn }))
);
const SignUp = React.lazy(() =>
  import('./pages/Public/SignUp').then((m) => ({ default: m.SignUp }))
);

// User Pages
const Dashboard = React.lazy(() =>
  import('./pages/User/Dashboard').then((m) => ({ default: m.Dashboard }))
);
const ActionCenter = React.lazy(() =>
  import('./pages/User/ActionCenter').then((m) => ({ default: m.ActionCenter }))
);

// Admin Pages
const PolicySettings = React.lazy(() =>
  import('./pages/Admin/PolicySettings').then((m) => ({ default: m.PolicySettings }))
);
const AdminSetup = React.lazy(() =>
  import('./pages/Admin/Setup').then((m) => ({ default: m.Setup }))
);
const ApplicationConsole = React.lazy(() =>
  import('./pages/Admin/ApplicationConsole').then((m) => ({ default: m.ApplicationConsole }))
);

const WealthAccounts = React.lazy(() =>
  import('./pages/Wealth').then((m) => ({ default: m.Accounts }))
);
const WealthTransactions = React.lazy(() =>
  import('./pages/Wealth').then((m) => ({ default: m.Transactions }))
);
const WealthReports = React.lazy(() =>
  import('./pages/Wealth').then((m) => ({ default: m.Reports }))
);
const WealthPhysicalAssets = React.lazy(() =>
  import('./pages/Wealth').then((m) => ({ default: m.PhysicalAssets }))
);
const WealthGoalPlans = React.lazy(() =>
  import('./pages/Wealth').then((m) => ({ default: m.GoalPlans }))
);
const WealthInsurancePolicies = React.lazy(() =>
  import('./pages/Wealth').then((m) => ({ default: m.InsurancePolicies }))
);

const HouseholdProfiles = React.lazy(() =>
  import('./pages/Household').then((m) => ({ default: m.Profiles }))
);
const HouseholdCalendar = React.lazy(() =>
  import('./pages/Household').then((m) => ({ default: m.Calendar }))
);
const HouseholdInventory = React.lazy(() =>
  import('./pages/Household').then((m) => ({ default: m.Inventory }))
);
const HouseholdGoals = React.lazy(() =>
  import('./pages/Household').then((m) => ({ default: m.Goals }))
);
const HouseholdVacationPlanner = React.lazy(() =>
  import('./pages/Household').then((m) => ({ default: m.VacationPlanner }))
);

const HealthVitals = React.lazy(() =>
  import('./pages/Health').then((m) => ({ default: m.Vitals }))
);
const HealthDoctorVisits = React.lazy(() =>
  import('./pages/Health').then((m) => ({ default: m.DoctorVisits }))
);

const PageLoader = () => (
  <div className="flex justify-center items-center h-[50vh]">
    <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-600"></div>
  </div>
);

function resolveTheme(theme, prefersDark) {
  if (theme !== 'auto') return theme;
  return prefersDark ? 'dark' : 'light';
}

// Single client for the app's lifetime (ADR-018: React Query for server state).
const queryClient = new QueryClient();

function App() {
  const [theme, setTheme] = useState('auto');

  useEffect(() => {
    const savedTheme = globalThis.localStorage.getItem('theme');
    setTheme(savedTheme === 'light' || savedTheme === 'dark' ? savedTheme : 'auto');
  }, []);

  useEffect(() => {
    const prefersDark = globalThis.matchMedia?.('(prefers-color-scheme: dark)')?.matches ?? false;
    const activeTheme = resolveTheme(theme, prefersDark);
    document.documentElement.classList.toggle('dark', activeTheme === 'dark');
    document.documentElement.classList.toggle('light', activeTheme === 'light');
  }, [theme]);

  const toggleTheme = () => {
    const prefersDark = globalThis.matchMedia?.('(prefers-color-scheme: dark)')?.matches ?? false;
    const currentTheme = resolveTheme(theme, prefersDark);
    const nextTheme = currentTheme === 'dark' ? 'light' : 'dark';
    setTheme(nextTheme);
    globalThis.localStorage.setItem('theme', nextTheme);
  };

  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Navigation theme={theme} onToggleTheme={toggleTheme} />
          <React.Suspense fallback={<PageLoader />}>
            <Routes>
              {/* Public Routes */}
              <Route path="/" element={<Home />} />
              <Route path="/about" element={<About />} />
              <Route path="/help" element={<Help />} />
              <Route path="/help/:docName" element={<Help />} />
              <Route path="/signin" element={<SignIn />} />
              <Route path="/signup" element={<SignUp />} />

              {/* Redirect old stub routes */}
              <Route
                path="/transactions"
                element={<Navigate to="/wealth/transactions" replace />}
              />
              <Route path="/health" element={<Navigate to="/health/vitals" replace />} />

              {/* User Routes - requires 'user' role or higher */}
              <Route
                path="/dashboard"
                element={
                  <ProtectedRoute requiredRole="user">
                    <SetupGate>
                      <Dashboard />
                    </SetupGate>
                  </ProtectedRoute>
                }
              />
              <Route
                path="/action-center"
                element={
                  <ProtectedRoute requiredRole="user">
                    <ActionCenter />
                  </ProtectedRoute>
                }
              />

              {/* Wealth Domain */}
              <Route
                path="/wealth/accounts"
                element={
                  <ProtectedRoute requiredRole="user">
                    <WealthAccounts />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/wealth/transactions"
                element={
                  <ProtectedRoute requiredRole="user">
                    <WealthTransactions />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/wealth/reports"
                element={
                  <ProtectedRoute requiredRole="user">
                    <WealthReports />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/wealth/physical-assets"
                element={
                  <ProtectedRoute requiredRole="user">
                    <WealthPhysicalAssets />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/wealth/goal-plans"
                element={
                  <ProtectedRoute requiredRole="user">
                    <WealthGoalPlans />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/wealth/insurance-policies"
                element={
                  <ProtectedRoute requiredRole="user">
                    <WealthInsurancePolicies />
                  </ProtectedRoute>
                }
              />

              {/* Household Domain */}
              <Route
                path="/household/profiles"
                element={
                  <ProtectedRoute requiredRole="user">
                    <HouseholdProfiles />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/household/calendar"
                element={
                  <ProtectedRoute requiredRole="user">
                    <HouseholdCalendar />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/household/inventory"
                element={
                  <ProtectedRoute requiredRole="user">
                    <HouseholdInventory />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/household/goals"
                element={
                  <ProtectedRoute requiredRole="user">
                    <HouseholdGoals />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/household/vacation-planner"
                element={
                  <ProtectedRoute requiredRole="user">
                    <HouseholdVacationPlanner />
                  </ProtectedRoute>
                }
              />

              {/* Health Domain */}
              <Route
                path="/health/vitals"
                element={
                  <ProtectedRoute requiredRole="user">
                    <HealthVitals />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/health/doctors"
                element={
                  <ProtectedRoute requiredRole="user">
                    <HealthDoctorVisits />
                  </ProtectedRoute>
                }
              />
              {/* Admin Routes */}
              <Route
                path="/admin/setup"
                element={
                  <ProtectedRoute requiredRole="admin">
                    <AdminSetup />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/policy"
                element={
                  <ProtectedRoute requiredRole="admin">
                    <PolicySettings />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/console"
                element={
                  <ProtectedRoute requiredRole="admin">
                    <ApplicationConsole />
                  </ProtectedRoute>
                }
              />

              {/* Fallback */}
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </React.Suspense>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
}

export default App;
