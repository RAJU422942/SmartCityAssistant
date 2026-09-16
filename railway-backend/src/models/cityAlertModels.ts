export type AlertCategory = 'WEATHER' | 'DISASTER' | 'AIR_QUALITY';

export type AlertSeverity = 'INFO' | 'LOW' | 'MODERATE' | 'HIGH' | 'SEVERE' | 'CRITICAL';

export type AlertStatus = 'ACTIVE' | 'UPCOMING' | 'EXPIRED';

export interface CityAlert {
  id: string;
  type?: string;
  title: string;
  description: string;
  category: AlertCategory;
  severity: AlertSeverity;
  status: AlertStatus;
  startTime?: string;
  endTime?: string;
  lastUpdated?: string;
  locationName?: string;
  city?: string;
  district?: string;
  latitude?: number;
  longitude?: number;
  sourceName: string;
  sourceUrl?: string;
  isVerified: boolean;
}

export interface CityAlertsResponse {
  status: string;
  location?: {
    latitude: number;
    longitude: number;
    city?: string;
    district?: string;
  };
  alerts: CityAlert[];
  updatedAt: string;
}
