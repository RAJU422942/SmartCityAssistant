export interface BackendLiveStatusResponse {
  trainNumber: string;
  trainName: string;
  currentStation: string;
  nextStation: string | null;
  delayMinutes: number;
  runningState: string;
  lastUpdated: string;
  status: string;
  message?: string;
}

export interface BackendPnrResponse {
  pnrNumber: string;
  trainNumber: string;
  trainName: string;
  journeyDate: string;
  from: string;
  to: string;
  bookingStatus: string;
  currentStatus: string;
  coachBerth: string;
  chartingStatus: string;
  status?: string;
  message?: string;
}

export interface BackendAvailabilityResponse {
  trainNumber: string;
  status: string;
  availabilityText: string;
  fare: string | null;
  message?: string;
}

export interface BackendTrainResponse {
  trainNumber: string;
  trainName: string;
  source: string;
  destination: string;
  departureTime: string;
  arrivalTime: string;
  duration: string;
  runningDays: string[];
  status: string;
  classes: string[];
}

export interface BackendAqiResponse {
  status: string;
  aqi: number | null;
  category: string | null;
  dominantPollutant: string | null;
  stationName: string | null;
  stationLatitude: number | null;
  stationLongitude: number | null;
  distanceKm: number | null;
  pm25: number | null;
  pm10: number | null;
  co: number | null;
  no2: number | null;
  o3: number | null;
  so2?: number | null;
  nh3?: number | null;
  timeString: string | null;
  message?: string | null;
  source?: string | null;
}

export interface TransportPoiItem {
  name: string;
  address: string;
  latitude: number;
  longitude: number;
  distanceKm: number;
  status?: string;
  source?: string;
}

export interface TransportPoiResponse {
  status: string;
  results: TransportPoiItem[];
  message?: string;
}

export interface UnavailableResponse {
  status: string;
  message: string;
}
