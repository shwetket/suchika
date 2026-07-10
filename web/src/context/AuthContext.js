import React, { createContext, useState, useEffect, useCallback, useMemo } from 'react';
import PropTypes from 'prop-types';
import { signIn } from '../api/auth';
import { listAdmins } from '../api/admins';
import { listProfiles } from '../api/profiles';
import { logError } from '../utils/errorHandler';

const SELF_RELATION = 'SELF';

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

    const newUser = {
      username: response.username,
      role: response.role,
      token: response.token,
      loginTime: response.issued_at,
    };

    // ADR-021: resolve admin_id/profile_id from the backend's actual admin
    // list on every login, instead of carrying forward whatever happened to
    // be sitting in localStorage for the same username (removed — that
    // heuristic always failed on a fresh browser/session).
    // listAdmins()/listProfiles() return the ListXResponse wrapper shape
    // ({admins: [...], total_size} / {profiles: [...], total_size}), same as
    // every other caller (e.g. Profiles.js) — not a bare array.
    const adminsResponse = await listAdmins();
    const admins = adminsResponse.admins ?? [];

    if (admins.length === 1 && admins[0].is_active === true) {
      const adminId = admins[0].admin_id;
      newUser.admin_id = adminId;

      const profilesResponse = await listProfiles(adminId, true);
      const profiles = profilesResponse.profiles ?? [];
      const selfProfile = profiles.find((profile) => profile.relation_to_admin === SELF_RELATION);
      if (selfProfile && newUser.role === 'admin') {
        newUser.profile_id = selfProfile.profile_id;
      }
    } else if (admins.length > 1) {
      // More than one admin: single-household is a hard assumption, not a
      // stopgap — never guess, never auto-create, never build a picker.
      newUser.household_conflict = true;
    }
    // 0 admins: leave admin_id/profile_id unset — true first-run, SetupGate
    // already routes this to /admin/setup.

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
