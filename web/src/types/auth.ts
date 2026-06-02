/**
 * Authentication and Authorization Types
 */

export type UserRole = 'admin' | 'user' | 'child';

export interface User {
  id: string;
  email: string;
  name: string;
  role: UserRole;
  profileId: string;
  lastLogin?: Date;
}

export interface AuthContextType {
  user: User | null;
  loading: boolean;
  error: string | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  hasRole: (role: UserRole | UserRole[]) => boolean;
  isAuthenticated: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  user: User;
}
