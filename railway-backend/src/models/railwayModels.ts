export interface BackendLiveStatusResponse {
  trainNumber: string;
  trainName: string;
  currentStation: string;
  nextStation: string | null;
  delayMinutes: number;
  runningState: string;
  lastUpdated: string;
  status: string;
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
}

export interface BackendAvailabilityResponse {
  trainNumber: string;
  status: string;
  availabilityText: string;
  fare: string | null;
}

export interface UnavailableResponse {
  status: string;
  message: string;
}
