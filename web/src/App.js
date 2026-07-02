import React, { useEffect, useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Navigation } from './components/Navigation';

// Public Pages
import { Home } from './pages/Public/Home';
import { About } from './pages/Public/About';
import { SignIn } from './pages/Public/SignIn';
import { SignUp } from './pages/Public/SignUp';

// User Pages
import { Dashboard } from './pages/User/Dashboard';

// Admin Pages
import { PolicySettings } from './pages/Admin/PolicySettings';

import {
  Accounts as WealthAccounts,
  Transactions as WealthTransactions,
  Reports as WealthReports,
  PhysicalAssets as WealthPhysicalAssets,
} from './pages/Wealth';
import {
  Profiles as HouseholdProfiles,
  Calendar as HouseholdCalendar,
  Inventory as HouseholdInventory,
  Goals as HouseholdGoals,
} from './pages/Household';
import {
  Vitals as HealthVitals,
  DoctorVisits as HealthDoctorVisits,
  HealthProfile as HealthProfilePage,
} from './pages/Health';

function resolveTheme(theme, prefersDark) {
  if (theme !== 'auto') return theme;
  return prefersDark ? 'dark' : 'light';
}

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
    <AuthProvider>
      <BrowserRouter>
        <Navigation theme={theme} onToggleTheme={toggleTheme} />
        <Routes>
          {/* Public Routes */}
          <Route path="/" element={<Home />} />
          <Route path="/about" element={<About />} />
          <Route path="/signin" element={<SignIn />} />
          <Route path="/signup" element={<SignUp />} />

          {/* Redirect old stub routes */}
          <Route path="/transactions" element={<Navigate to="/wealth/transactions" replace />} />
          <Route path="/health" element={<Navigate to="/health/vitals" replace />} />

          {/* User Routes - requires 'user' role or higher */}
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute requiredRole="user">
                <Dashboard />
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
          <Route
            path="/health/profile"
            element={
              <ProtectedRoute requiredRole="user">
                <HealthProfilePage />
              </ProtectedRoute>
            }
          />

          {/* Admin Routes */}
          <Route
            path="/admin/policy"
            element={
              <ProtectedRoute requiredRole="admin">
                <PolicySettings />
              </ProtectedRoute>
            }
          />

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
