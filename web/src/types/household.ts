/**
 * Household Domain Types
 */

export interface Profile {
  id: string;
  name: string;
  type: 'primary' | 'dependent' | 'child';
  dateOfBirth?: Date;
  relationship?: string;
  createdAt: Date;
  updatedAt: Date;
}

export interface CalendarEvent {
  id: string;
  profileId: string;
  title: string;
  description?: string;
  startTime: Date;
  endTime: Date;
  location?: string;
  category: string;
  createdAt: Date;
  updatedAt: Date;
}

export interface InventoryItem {
  id: string;
  profileId: string;
  name: string;
  category: string;
  quantity: number;
  location: string;
  expiryDate?: Date;
  notes?: string;
  createdAt: Date;
  updatedAt: Date;
}

export interface HouseholdDashboard {
  profiles: Profile[];
  upcomingEvents: CalendarEvent[];
  inventoryItems: InventoryItem[];
  notificationCount: number;
}
