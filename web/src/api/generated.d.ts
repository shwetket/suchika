export interface AuthCredentials {
  username: string;
  password?: string;
  role?: 'user' | 'admin' | 'public';
}

export interface AuthResponse {
  username: string;
  role: 'user' | 'admin' | 'public';
  token: string;
  refresh_token?: string;
  issued_at: string;
}

export interface UserSession {
  username: string;
  role: 'user' | 'admin' | 'public';
  token: string;
  loginTime: string;
}

export interface HealthProfile {
  name: string;
  profile_id: string;
  display_name: string;
  relationship: string;
  date_of_birth: string | null;
  blood_group: string | null;
  created_at: string;
}

export interface CreateHealthProfileRequest {
  display_name: string;
  relationship: string;
  date_of_birth?: string | null;
  blood_group?: string | null;
}

export interface ListHealthProfilesResponse {
  health_profiles: HealthProfile[];
}
