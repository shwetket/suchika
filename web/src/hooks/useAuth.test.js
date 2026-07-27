import React from 'react';
import { renderHook } from '@testing-library/react';
import { useAuth } from './useAuth';
import { AuthContext } from '../context/AuthContext';

describe('useAuth', () => {
  it('returns the context value when used within AuthProvider', () => {
    const mockValue = { user: { username: 'alice' }, isAuthenticated: true };
    const wrapper = ({ children }) => (
      <AuthContext.Provider value={mockValue}>{children}</AuthContext.Provider>
    );
    const { result } = renderHook(() => useAuth(), { wrapper });
    expect(result.current).toBe(mockValue);
  });

  it('throws when used outside of AuthProvider', () => {
    const { result } = renderHook(() => {
      try {
        return useAuth();
      } catch (err) {
        return err;
      }
    });
    expect(result.current).toBeInstanceOf(Error);
    expect(result.current.message).toBe('useAuth must be used within AuthProvider');
  });
});
