export type AlertCategory =
  | 'EMERGENCY'
  | 'WEATHER'
  | 'DISASTER'
  | 'AIR_QUALITY'
  | 'TRANSPORT'
  | 'TRAFFIC'
  | 'CIVIC'
  | 'GOVERNMENT'
  | 'HEALTH'
  | 'INFORMATION';

export type AlertSeverity = 'CRITICAL' | 'HIGH' | 'MODERATE' | 'LOW' | 'INFO';

export type AlertStatus = 'ACTIVE' | 'EXPIRED' | 'INFORMATION';

export interface CityAlert {
  id: string;
  title: string;
  description: string;
  category: AlertCategory;
  severity: AlertSeverity;
  city?: string;
  district?: string;
  latitude?: number;
  longitude?: number;
  affectedAreas?: string[];
  issuedAt: string;
  expiresAt?: string;
  sourceName: string;
  sourceUrl?: string;
  status: AlertStatus;
  isVerified: boolean;
}

export interface CityAlertsResponse {
  status: string;
  location?: {
    city?: string;
    district?: string;
    latitude?: number;
    longitude?: number;
  };
  alerts: CityAlert[];
}
