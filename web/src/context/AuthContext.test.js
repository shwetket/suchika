import React from 'react';
import { renderHook, act } from '@testing-library/react';
import { AuthProvider, AuthContext } from './AuthContext';
import * as authApi from '../api/auth';
import * as adminsApi from '../api/admins';
import * as profilesApi from '../api/profiles';

jest.mock('../api/auth');
jest.mock('../api/admins');
jest.mock('../api/profiles');

function wrapper({ children }) {
  return <AuthProvider>{children}</AuthProvider>;
}

describe('AuthContext', () => {
  beforeEach(() => {
    localStorage.clear();
    jest.clearAllMocks();
    jest.spyOn(console, 'error').mockImplementation(() => {});
    adminsApi.listAdmins.mockResolvedValue({ admins: [], total_size: 0 });
    profilesApi.listProfiles.mockResolvedValue({ profiles: [], total_size: 0 });
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('starts with null user and loading completes', async () => {
    const { result } = renderHook(() => React.useContext(AuthContext), { wrapper });
    // After mount, loading should be false
    await act(async () => {});
    expect(result.current.user).toBeNull();
    expect(result.current.loading).toBe(false);
    expect(result.current.isAuthenticated).toBe(false);
  });

  it('restores user from localStorage on mount', async () => {
    const storedUser = { username: 'alice', role: 'admin', token: 'tok123' };
    localStorage.setItem('user', JSON.stringify(storedUser));
    const { result } = renderHook(() => React.useContext(AuthContext), { wrapper });
    await act(async () => {});
    expect(result.current.user).toEqual(storedUser);
    expect(result.current.isAuthenticated).toBe(true);
  });

  it('login() stores user in state and localStorage', async () => {
    authApi.signIn.mockResolvedValue({
      username: 'bob',
      role: 'user',
      token: 'tok456',
      issued_at: '2024-01-01T00:00:00Z',
    });

    const { result } = renderHook(() => React.useContext(AuthContext), { wrapper });
    await act(async () => {});

    await act(async () => {
      await result.current.login('bob', 'user');
    });

    expect(result.current.user).toMatchObject({ username: 'bob', role: 'user' });
    expect(result.current.isAuthenticated).toBe(true);
    const stored = JSON.parse(localStorage.getItem('user'));
    expect(stored.username).toBe('bob');
  });

  it('logout() clears user from state and localStorage', async () => {
    const storedUser = { username: 'alice', role: 'admin', token: 'tok' };
    localStorage.setItem('user', JSON.stringify(storedUser));
    const { result } = renderHook(() => React.useContext(AuthContext), { wrapper });
    await act(async () => {});

    act(() => {
      result.current.logout();
    });

    expect(result.current.user).toBeNull();
    expect(result.current.isAuthenticated).toBe(false);
    expect(localStorage.getItem('user')).toBeNull();
  });

  it('updateUser() merges partial fields into user state and localStorage', async () => {
    const storedUser = { username: 'alice', role: 'admin', token: 'tok' };
    localStorage.setItem('user', JSON.stringify(storedUser));
    const { result } = renderHook(() => React.useContext(AuthContext), { wrapper });
    await act(async () => {});

    act(() => {
      result.current.updateUser({ admin_id: 'admin-1', profile_id: 'profile-1' });
    });

    expect(result.current.user).toMatchObject({
      username: 'alice',
      role: 'admin',
      admin_id: 'admin-1',
      profile_id: 'profile-1',
    });
    const stored = JSON.parse(localStorage.getItem('user'));
    expect(stored.admin_id).toBe('admin-1');
    expect(stored.profile_id).toBe('profile-1');
  });

  describe('hasRole()', () => {
    it('returns true for public when user is null', async () => {
      const { result } = renderHook(() => React.useContext(AuthContext), { wrapper });
      await act(async () => {});
      expect(result.current.hasRole('public')).toBe(true);
    });

    it('returns false for user role when user is null', async () => {
      const { result } = renderHook(() => React.useContext(AuthContext), { wrapper });
      await act(async () => {});
      expect(result.current.hasRole('user')).toBe(false);
    });

    it('returns true for user role when user has user role', async () => {
      localStorage.setItem('user', JSON.stringify({ username: 'u', role: 'user', token: 't' }));
      const { result } = renderHook(() => React.useContext(AuthContext), { wrapper });
      await act(async () => {});
      expect(result.current.hasRole('user')).toBe(true);
    });

    it('returns false for admin role when user has user role', async () => {
      localStorage.setItem('user', JSON.stringify({ username: 'u', role: 'user', token: 't' }));
      const { result } = renderHook(() => React.useContext(AuthContext), { wrapper });
      await act(async () => {});
      expect(result.current.hasRole('admin')).toBe(false);
    });

    it('returns true for admin role when user has admin role', async () => {
      localStorage.setItem('user', JSON.stringify({ username: 'a', role: 'admin', token: 't' }));
      const { result } = renderHook(() => React.useContext(AuthContext), { wrapper });
      await act(async () => {});
      expect(result.current.hasRole('admin')).toBe(true);
    });

    it('returns true for user role when user has admin role (hierarchy)', async () => {
      localStorage.setItem('user', JSON.stringify({ username: 'a', role: 'admin', token: 't' }));
      const { result } = renderHook(() => React.useContext(AuthContext), { wrapper });
      await act(async () => {});
      expect(result.current.hasRole('user')).toBe(true);
    });
  });

  describe('ADR-021 admin auto-attach', () => {
    const signInResponse = (role) => ({
      username: 'alice',
      role,
      token: 'tok-new',
      issued_at: '2024-02-01T00:00:00Z',
    });

    it('leaves admin_id/profile_id unset when zero admins exist (true first-run)', async () => {
      authApi.signIn.mockResolvedValue(signInResponse('admin'));
      adminsApi.listAdmins.mockResolvedValue({ admins: [], total_size: 0 });

      const { result } = renderHook(() => React.useContext(AuthContext), { wrapper });
      await act(async () => {});

      await act(async () => {
        await result.current.login('alice', 'admin');
      });

      expect(result.current.user.admin_id).toBeUndefined();
      expect(result.current.user.profile_id).toBeUndefined();
      expect(result.current.user.household_conflict).toBeUndefined();
      expect(profilesApi.listProfiles).not.toHaveBeenCalled();
    });

    it('auto-attaches admin_id and the SELF profile_id when exactly one active admin exists', async () => {
      authApi.signIn.mockResolvedValue(signInResponse('admin'));
      adminsApi.listAdmins.mockResolvedValue({
        admins: [{ admin_id: 'admin-1', is_active: true, display_name: 'Ketan' }],
        total_size: 1,
      });
      profilesApi.listProfiles.mockResolvedValue({
        profiles: [
          { profile_id: 'profile-1', relation_to_admin: 'SELF' },
          { profile_id: 'profile-2', relation_to_admin: 'SPOUSE' },
        ],
        total_size: 2,
      });

      const { result } = renderHook(() => React.useContext(AuthContext), { wrapper });
      await act(async () => {});

      await act(async () => {
        await result.current.login('alice', 'admin');
      });

      expect(profilesApi.listProfiles).toHaveBeenCalledWith('admin-1', true);
      expect(result.current.user).toMatchObject({
        admin_id: 'admin-1',
        profile_id: 'profile-1',
      });
    });

    it('does not attach profile_id when the login role is not admin', async () => {
      authApi.signIn.mockResolvedValue(signInResponse('user'));
      adminsApi.listAdmins.mockResolvedValue({
        admins: [{ admin_id: 'admin-1', is_active: true, display_name: 'Ketan' }],
        total_size: 1,
      });
      profilesApi.listProfiles.mockResolvedValue({
        profiles: [{ profile_id: 'profile-1', relation_to_admin: 'SELF' }],
        total_size: 1,
      });

      const { result } = renderHook(() => React.useContext(AuthContext), { wrapper });
      await act(async () => {});

      await act(async () => {
        await result.current.login('alice', 'user');
      });

      expect(result.current.user.admin_id).toBe('admin-1');
      expect(result.current.user.profile_id).toBeUndefined();
    });

    it('does not auto-attach when the sole admin is inactive', async () => {
      authApi.signIn.mockResolvedValue(signInResponse('admin'));
      adminsApi.listAdmins.mockResolvedValue({
        admins: [{ admin_id: 'admin-1', is_active: false, display_name: 'Ketan' }],
        total_size: 1,
      });

      const { result } = renderHook(() => React.useContext(AuthContext), { wrapper });
      await act(async () => {});

      await act(async () => {
        await result.current.login('alice', 'admin');
      });

      expect(result.current.user.admin_id).toBeUndefined();
      expect(result.current.user.household_conflict).toBeUndefined();
    });

    it('auto-attaches to the sole ACTIVE admin when a deactivated admin also exists', async () => {
      // GET /v1/admins ignores ?is_active and always returns every admin (active
      // and deactivated) — a deactivated leftover must not count toward the
      // multi-admin conflict check.
      authApi.signIn.mockResolvedValue(signInResponse('admin'));
      adminsApi.listAdmins.mockResolvedValue({
        admins: [
          { admin_id: 'admin-1', is_active: true, display_name: 'Ketan' },
          { admin_id: 'admin-2', is_active: false, display_name: 'Old Test Admin' },
        ],
        total_size: 2,
      });
      profilesApi.listProfiles.mockResolvedValue({
        profiles: [{ profile_id: 'profile-1', relation_to_admin: 'SELF' }],
        total_size: 1,
      });

      const { result } = renderHook(() => React.useContext(AuthContext), { wrapper });
      await act(async () => {});

      await act(async () => {
        await result.current.login('alice', 'admin');
      });

      expect(result.current.user.admin_id).toBe('admin-1');
      expect(result.current.user.household_conflict).toBeUndefined();
      expect(profilesApi.listProfiles).toHaveBeenCalledWith('admin-1', true);
    });

    it('sets household_conflict and does not guess when more than one admin exists', async () => {
      authApi.signIn.mockResolvedValue(signInResponse('admin'));
      adminsApi.listAdmins.mockResolvedValue({
        admins: [
          { admin_id: 'admin-1', is_active: true, display_name: 'Ketan' },
          { admin_id: 'admin-2', is_active: true, display_name: 'Someone Else' },
        ],
        total_size: 2,
      });

      const { result } = renderHook(() => React.useContext(AuthContext), { wrapper });
      await act(async () => {});

      await act(async () => {
        await result.current.login('alice', 'admin');
      });

      expect(result.current.user.household_conflict).toBe(true);
      expect(result.current.user.admin_id).toBeUndefined();
      expect(profilesApi.listProfiles).not.toHaveBeenCalled();
    });

    it('propagates the error and leaves user unset when listAdmins() throws', async () => {
      authApi.signIn.mockResolvedValue(signInResponse('admin'));
      adminsApi.listAdmins.mockRejectedValue(new Error('network error'));

      const { result } = renderHook(() => React.useContext(AuthContext), { wrapper });
      await act(async () => {});

      await expect(
        act(async () => {
          await result.current.login('alice', 'admin');
        })
      ).rejects.toThrow('network error');

      expect(result.current.user).toBeNull();
      expect(localStorage.getItem('user')).toBeNull();
    });
  });

  it('handles invalid JSON in localStorage gracefully', async () => {
    localStorage.setItem('user', 'not-valid-json');
    const { result } = renderHook(() => React.useContext(AuthContext), { wrapper });
    await act(async () => {});
    expect(result.current.user).toBeNull();
    expect(result.current.loading).toBe(false);
  });
});
