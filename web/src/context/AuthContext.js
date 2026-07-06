import React, { createContext, useState, useEffect, useCallback, useMemo } from 'react';
import PropTypes from 'prop-types';
import { signIn } from '../api/auth';
import { logError } from '../utils/errorHandler';

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      try {
        setUser(JSON.parse(storedUser));
      } catch (e) {
        logError('AuthContext', e);
      }
    }
    setLoading(false);
  }, []);

  const login = useCallback(async (username, role = 'user') => {
    const response = await signIn({ username, role });

    // Carry forward admin_id/profile_id from a prior session for the same
    // username, so re-authenticating doesn't send an already-set-up admin
    // back through the setup wizard (SetupGate gates on user.admin_id).
    let carryForward = {};
    const stored = localStorage.getItem('user');
    if (stored) {
      try {
        const previous = JSON.parse(stored);
        if (previous.username === response.username) {
          carryForward = { admin_id: previous.admin_id, profile_id: previous.profile_id };
        }
      } catch (e) {
        logError('AuthContext', e);
      }
    }

    const newUser = {
      ...carryForward,
      username: response.username,
      role: response.role,
      token: response.token,
      loginTime: response.issued_at,
    };
    setUser(newUser);
    localStorage.setItem('user', JSON.stringify(newUser));
    return newUser;
  }, []);

  const logout = useCallback(() => {
    setUser(null);
    localStorage.removeItem('user');
  }, []);

  const updateUser = useCallback((partial) => {
    setUser((prev) => {
      const next = { ...prev, ...partial };
      localStorage.setItem('user', JSON.stringify(next));
      return next;
    });
  }, []);

  const hasRole = useCallback(
    (requiredRole) => {
      if (!user) return requiredRole === 'public';

      const roleHierarchy = {
        public: 0,
        user: 1,
        admin: 2,
      };

      const userLevel = roleHierarchy[user.role] || 0;
      const requiredLevel = roleHierarchy[requiredRole] || 0;

      return userLevel >= requiredLevel;
    },
    [user]
  );

  const value = useMemo(
    () => ({
      user,
      loading,
      login,
      logout,
      updateUser,
      hasRole,
      isAuthenticated: !!user,
    }),
    [user, loading, login, logout, updateUser, hasRole]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

AuthProvider.propTypes = {
  children: PropTypes.node.isRequired,
};
