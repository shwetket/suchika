/**
 * Health Domain Types
 */

export interface HealthProfile {
  id: string;
  profileId: string;
  bloodType?: string;
  allergies: string[];
  chronicConditions: string[];
  medications: string[];
  createdAt: Date;
  updatedAt: Date;
}

export interface VitalReading {
  id: string;
  profileId: string;
  type: 'heart_rate' | 'blood_pressure' | 'temperature' | 'weight' | 'oxygen_saturation';
  value: number;
  unit: string;
  timestamp: Date;
  notes?: string;
  createdAt: Date;
}

export interface DoctorVisit {
  id: string;
  profileId: string;
  doctorName: string;
  specialty: string;
  visitDate: Date;
  reason: string;
  diagnosis?: string;
  prescription?: string;
  notes?: string;
  nextFollowUp?: Date;
  createdAt: Date;
  updatedAt: Date;
}

export interface HealthDashboard {
  profile: HealthProfile;
  recentVitals: VitalReading[];
  doctorVisits: DoctorVisit[];
  healthAlerts: string[];
}

export interface VitalFilter {
  profileId?: string;
  type?: string;
  dateFrom?: Date;
  dateTo?: Date;
  limit?: number;
}
