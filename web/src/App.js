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
import { Transactions } from './pages/User/Transactions';
import { Health } from './pages/User/Health';

// Admin Pages
import { AdminUsers } from './pages/Admin/AdminUsers';
import { AdminSettings } from './pages/Admin/AdminSettings';
import { AdminReports } from './pages/Admin/AdminReports';

import './App.css';

function App() {
  const [theme, setTheme] = useState('auto');

  useEffect(() => {
    const savedTheme = window.localStorage.getItem('theme');
    setTheme(savedTheme === 'light' || savedTheme === 'dark' ? savedTheme : 'auto');
  }, []);

  useEffect(() => {
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    const activeTheme = theme === 'auto' ? (prefersDark ? 'dark' : 'light') : theme;
    document.documentElement.classList.toggle('dark', activeTheme === 'dark');
    document.documentElement.classList.toggle('light', activeTheme === 'light');
  }, [theme]);

  const toggleTheme = () => {
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    const currentTheme = theme === 'auto' ? (prefersDark ? 'dark' : 'light') : theme;
    const nextTheme = currentTheme === 'dark' ? 'light' : 'dark';
    setTheme(nextTheme);
    window.localStorage.setItem('theme', nextTheme);
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

          {/* User Routes - requires 'user' role or higher */}
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute requiredRole="user">
                <Dashboard />
              </ProtectedRoute>
            }
          />
          <Route
            path="/transactions"
            element={
              <ProtectedRoute requiredRole="user">
                <Transactions />
              </ProtectedRoute>
            }
          />
          <Route
            path="/health"
            element={
              <ProtectedRoute requiredRole="user">
                <Health />
              </ProtectedRoute>
            }
          />

          {/* Admin Routes - requires 'admin' role */}
          <Route
            path="/admin/users"
            element={
              <ProtectedRoute requiredRole="admin">
                <AdminUsers />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/settings"
            element={
              <ProtectedRoute requiredRole="admin">
                <AdminSettings />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/reports"
            element={
              <ProtectedRoute requiredRole="admin">
                <AdminReports />
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
