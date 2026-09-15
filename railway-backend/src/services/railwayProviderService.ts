import {
  BackendLiveStatusResponse,
  BackendPnrResponse,
  BackendAvailabilityResponse,
  BackendTrainResponse,
  BackendAqiResponse,
  UnavailableResponse
} from '../models/railwayModels';

function haversineKm(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371; // Earth radius in km
  const dLat = (lat2 - lat1) * (Math.PI / 180);
  const dLon = (lon2 - lon1) * (Math.PI / 180);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * (Math.PI / 180)) * Math.cos(lat2 * (Math.PI / 180)) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

const MAX_AQI_STATION_DISTANCE_KM = 100;

export class RailwayProviderService {
  private getApiKey(): string | undefined {
    const rawKey = process.env.RAILKIT_API_KEY;
    if (!rawKey) return undefined;
    return rawKey.trim().replace(/^["']|["']$/g, '');
  }

  private getAqiKey(): string | undefined {
    const rawKey = process.env.AQICN_API_KEY;
    if (!rawKey) return undefined;
    return rawKey.trim().replace(/^["']|["']$/g, '');
  }

  private getBaseUrl(): string {
    return process.env.RAILKIT_API_BASE_URL || 'https://api.railkit.io/v1';
  }

  private isConfigured(): boolean {
    const key = this.getApiKey();
    return !!key && key !== 'your_railkit_api_key_here';
  }

  private async fetchWithTimeout(url: string, options: RequestInit = {}, timeoutMs = 8000): Promise<Response> {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), timeoutMs);
    try {
      const response = await fetch(url, {
        ...options,
        signal: controller.signal,
        headers: {
          'Authorization': `Bearer ${this.getApiKey()}`,
          'x-api-key': this.getApiKey() || '',
          'Content-Type': 'application/json',
          ...(options.headers || {})
        }
      });
      clearTimeout(timeoutId);
      return response;
    } catch (error: any) {
      clearTimeout(timeoutId);
      if (error.name === 'AbortError') {
        throw new Error('Provider request timed out');
      }
      throw error;
    }
  }

  async getLiveStatus(trainNumber: string, date: string): Promise<BackendLiveStatusResponse | UnavailableResponse> {
    if (!this.isConfigured()) {
      return {
        status: 'UNAVAILABLE',
        message: 'Railway service is temporarily unavailable.'
      };
    }

    try {
      const url = `${this.getBaseUrl()}/trains/${trainNumber}/live?date=${encodeURIComponent(date)}`;
      const res = await this.fetchWithTimeout(url);

      if (!res.ok) {
        return {
          status: 'UNAVAILABLE',
          message: 'Railway service is temporarily unavailable.'
        };
      }

      const data: any = await res.json();
      return {
        trainNumber: data.trainNumber || trainNumber,
        trainName: data.trainName || 'Unknown Train',
        currentStation: data.currentStation || 'Unknown',
        nextStation: data.nextStation || null,
        delayMinutes: typeof data.delayMinutes === 'number' ? data.delayMinutes : 0,
        runningState: data.runningState || 'Running',
        lastUpdated: data.lastUpdated || new Date().toISOString(),
        status: 'OK'
      };
    } catch (error: any) {
      return {
        status: 'UNAVAILABLE',
        message: 'Railway service is temporarily unavailable.'
      };
    }
  }

  async getPnrStatus(pnr: string): Promise<BackendPnrResponse | UnavailableResponse> {
    if (!this.isConfigured()) {
      return {
        status: 'UNAVAILABLE',
        message: 'Railway service is temporarily unavailable.'
      };
    }

    try {
      const url = `${this.getBaseUrl()}/pnr/${pnr}`;
      const res = await this.fetchWithTimeout(url);

      if (res.status === 401 || res.status === 403) {
        return {
          pnrNumber: pnr,
          trainNumber: 'N/A',
          trainName: 'N/A',
          journeyDate: 'N/A',
          from: 'N/A',
          to: 'N/A',
          bookingStatus: 'N/A',
          currentStatus: 'AUTH_ERROR',
          coachBerth: 'N/A',
          chartingStatus: 'N/A',
          message: 'PNR service authentication unavailable'
        };
      }
      if (res.status === 429) {
        return {
          pnrNumber: pnr,
          trainNumber: 'N/A',
          trainName: 'N/A',
          journeyDate: 'N/A',
          from: 'N/A',
          to: 'N/A',
          bookingStatus: 'N/A',
          currentStatus: 'RATE_LIMIT',
          coachBerth: 'N/A',
          chartingStatus: 'N/A',
          message: 'PNR service is temporarily busy. Try again later.'
        };
      }
      if (res.status === 404) {
        return {
          pnrNumber: pnr,
          trainNumber: 'N/A',
          trainName: 'N/A',
          journeyDate: 'N/A',
          from: 'N/A',
          to: 'N/A',
          bookingStatus: 'N/A',
          currentStatus: 'NOT_FOUND',
          coachBerth: 'N/A',
          chartingStatus: 'N/A',
          message: 'PNR record not found.'
        };
      }
      if (!res.ok) {
        return {
          pnrNumber: pnr,
          trainNumber: 'N/A',
          trainName: 'N/A',
          journeyDate: 'N/A',
          from: 'N/A',
          to: 'N/A',
          bookingStatus: 'N/A',
          currentStatus: 'UNAVAILABLE',
          coachBerth: 'N/A',
          chartingStatus: 'N/A',
          message: 'Railway service is temporarily unavailable.'
        };
      }

      const data: any = await res.json();
      return {
        pnrNumber: data.pnrNumber || data.pnr || pnr,
        trainNumber: data.trainNumber || data.train_number || 'N/A',
        trainName: data.trainName || data.train_name || 'N/A',
        journeyDate: data.journeyDate || data.journey_date || 'N/A',
        from: data.from || data.source_station || 'N/A',
        to: data.to || data.destination_station || 'N/A',
        bookingStatus: data.bookingStatus || data.booking_status || 'N/A',
        currentStatus: data.currentStatus || data.current_status || 'CONFIRMED',
        coachBerth: data.coachBerth || data.coach_position || 'N/A',
        chartingStatus: data.chartingStatus || data.charting_status || 'N/A'
      };
    } catch (error: any) {
      return {
        pnrNumber: pnr,
        trainNumber: 'N/A',
        trainName: 'N/A',
        journeyDate: 'N/A',
        from: 'N/A',
        to: 'N/A',
        bookingStatus: 'N/A',
        currentStatus: 'UNAVAILABLE',
        coachBerth: 'N/A',
        chartingStatus: 'N/A',
        message: 'Railway service is temporarily unavailable.'
      };
    }
  }

  async getSeatAvailability(
    train: string,
    from: string,
    to: string,
    date: string,
    trainClass: string,
    quota: string
  ): Promise<BackendAvailabilityResponse | UnavailableResponse> {
    if (!this.isConfigured()) {
      return {
        status: 'UNAVAILABLE',
        message: 'Railway service is temporarily unavailable.'
      };
    }

    try {
      const params = new URLSearchParams({
        train,
        from,
        to,
        date,
        class: trainClass,
        quota
      });
      const url = `${this.getBaseUrl()}/availability?${params.toString()}`;
      const res = await this.fetchWithTimeout(url);

      if (!res.ok) {
        return {
          status: 'UNAVAILABLE',
          message: 'Railway service is temporarily unavailable.'
        };
      }

      const data: any = await res.json();
      return {
        trainNumber: data.trainNumber || train,
        status: data.status || 'UNKNOWN',
        availabilityText: data.availabilityText || 'Data unavailable',
        fare: data.fare || null
      };
    } catch (error: any) {
      return {
        status: 'UNAVAILABLE',
        message: 'Railway service is temporarily unavailable.'
      };
    }
  }

  async getTrainsBetween(from: string, to: string, date: string): Promise<BackendTrainResponse[] | UnavailableResponse> {
    if (!this.isConfigured()) {
      return {
        status: 'UNAVAILABLE',
        message: 'Railway service is temporarily unavailable.'
      };
    }

    try {
      const url = `${this.getBaseUrl()}/trains/between/${from}/${to}?date=${encodeURIComponent(date)}`;
      const res = await this.fetchWithTimeout(url);

      if (!res.ok) {
        return {
          status: 'UNAVAILABLE',
          message: 'Railway service is temporarily unavailable.'
        };
      }

      const data: any = await res.json();
      if (Array.isArray(data)) {
        return data.map((t: any) => ({
          trainNumber: t.trainNumber || t.train_number || 'N/A',
          trainName: t.trainName || t.train_name || 'Express Service',
          source: t.source || t.from || from,
          destination: t.destination || t.to || to,
          departureTime: t.departureTime || t.departure || '08:00 AM',
          arrivalTime: t.arrivalTime || t.arrival || '04:00 PM',
          duration: t.duration || '8h 00m',
          runningDays: t.runningDays || t.running_days || ['Daily'],
          status: t.status || 'Scheduled',
          classes: t.classes || ['3A', 'SL']
        }));
      }
      return [];
    } catch (error: any) {
      return {
        status: 'UNAVAILABLE',
        message: 'Railway service is temporarily unavailable.'
      };
    }
  }

  async getTrainInfo(trainNumber: string): Promise<any> {
    if (!this.isConfigured()) {
      return { status: 'UNAVAILABLE', message: 'Railway service is temporarily unavailable.' };
    }
    try {
      const res = await this.fetchWithTimeout(`${this.getBaseUrl()}/trains/${trainNumber}`);
      if (!res.ok) return { status: 'UNAVAILABLE', message: 'Train info unavailable.' };
      return await res.json();
    } catch (e) {
      return { status: 'UNAVAILABLE', message: 'Train info unavailable.' };
    }
  }

  async searchStations(name: string): Promise<any> {
    if (!this.isConfigured()) {
      return { status: 'UNAVAILABLE', message: 'Railway service is temporarily unavailable.' };
    }
    try {
      const res = await this.fetchWithTimeout(`${this.getBaseUrl()}/stations/search?name=${encodeURIComponent(name)}`);
      if (!res.ok) return { status: 'UNAVAILABLE', message: 'Station search unavailable.' };
      return await res.json();
    } catch (e) {
      return { status: 'UNAVAILABLE', message: 'Station search unavailable.' };
    }
  }

  async searchTrainsByName(name: string): Promise<any> {
    if (!this.isConfigured()) {
      return { status: 'UNAVAILABLE', message: 'Railway service is temporarily unavailable.' };
    }
    try {
      const res = await this.fetchWithTimeout(`${this.getBaseUrl()}/trains/search?name=${encodeURIComponent(name)}`);
      if (!res.ok) return { status: 'UNAVAILABLE', message: 'Train search unavailable.' };
      return await res.json();
    } catch (e) {
      return { status: 'UNAVAILABLE', message: 'Train search unavailable.' };
    }
  }

  async getAqi(lat: string, lon: string): Promise<BackendAqiResponse | UnavailableResponse> {
    const aqiKey = this.getAqiKey();
    const userLat = parseFloat(lat);
    const userLon = parseFloat(lon);
    console.log(`[AQI Diagnostic]: AQICN_API_KEY exists: ${!!aqiKey}, requested lat: ${lat}, lon: ${lon}`);

    if (!aqiKey) {
      return {
        status: 'UNAVAILABLE',
        message: 'AQI provider is not configured',
        aqi: null,
        category: null,
        dominantPollutant: null,
        stationName: null,
        stationLatitude: null,
        stationLongitude: null,
        distanceKm: null,
        pm25: null,
        pm10: null,
        co: null,
        no2: null,
        o3: null,
        timeString: null
      };
    }

    try {
      const url = `https://api.waqi.info/feed/geo:${lat};${lon}/?token=${aqiKey}`;
      const res = await this.fetchWithTimeout(url);

      if (!res.ok) {
        return {
          status: 'UNAVAILABLE',
          message: 'Air quality data is not available for your current location.',
          aqi: null,
          category: null,
          dominantPollutant: null,
          stationName: null,
          stationLatitude: null,
          stationLongitude: null,
          distanceKm: null,
          pm25: null,
          pm10: null,
          co: null,
          no2: null,
          o3: null,
          timeString: null
        };
      }

      const json = await res.json();

      if (json?.status === 'error' && json?.data === 'Invalid key') {
        return {
          status: 'UNAVAILABLE',
          message: 'AQI provider authentication failed',
          aqi: null,
          category: null,
          dominantPollutant: null,
          stationName: null,
          stationLatitude: null,
          stationLongitude: null,
          distanceKm: null,
          pm25: null,
          pm10: null,
          co: null,
          no2: null,
          o3: null,
          timeString: null
        };
      }

      if (json?.status !== 'ok' || !json?.data) {
        return {
          status: 'NO_NEARBY_AQI_STATION',
          aqi: null,
          category: null,
          dominantPollutant: null,
          stationName: null,
          stationLatitude: null,
          stationLongitude: null,
          distanceKm: null,
          pm25: null,
          pm10: null,
          co: null,
          no2: null,
          o3: null,
          timeString: null,
          message: 'No nearby air quality monitoring station was found for your current location.'
        };
      }

      const d = json.data;
      const stationGeo = d.city?.geo;
      if (!Array.isArray(stationGeo) || stationGeo.length < 2 || typeof stationGeo[0] !== 'number' || typeof stationGeo[1] !== 'number') {
        return {
          status: 'UNAVAILABLE',
          message: 'Air quality station location could not be verified.',
          aqi: null,
          category: null,
          dominantPollutant: null,
          stationName: null,
          stationLatitude: null,
          stationLongitude: null,
          distanceKm: null,
          pm25: null,
          pm10: null,
          co: null,
          no2: null,
          o3: null,
          timeString: null
        };
      }

      const stationLat = stationGeo[0];
      const stationLon = stationGeo[1];
      const distanceKm = haversineKm(userLat, userLon, stationLat, stationLon);
      const roundedDistance = Math.round(distanceKm * 10) / 10;

      console.log(`[AQI Distance Check]: user=(${userLat}, ${userLon}), station=(${stationLat}, ${stationLon}), distance=${roundedDistance} km, threshold=${MAX_AQI_STATION_DISTANCE_KM} km`);

      if (roundedDistance > MAX_AQI_STATION_DISTANCE_KM) {
        return {
          status: 'NO_NEARBY_AQI_STATION',
          aqi: null,
          category: null,
          dominantPollutant: null,
          stationName: null,
          stationLatitude: stationLat,
          stationLongitude: stationLon,
          distanceKm: roundedDistance,
          pm25: null,
          pm10: null,
          co: null,
          no2: null,
          o3: null,
          timeString: null,
          message: `No nearby air quality monitoring station was found for your current location (~${roundedDistance} km away).`
        };
      }

      const aqiVal = typeof d.aqi === 'number' ? d.aqi : 0;
      const domPol = d.dominentpol || null;
      const station = d.city?.name || null;
      const iaqi = d.iaqi || {};

      let cat = 'GOOD';
      if (aqiVal <= 50) cat = 'GOOD';
      else if (aqiVal <= 100) cat = 'MODERATE';
      else if (aqiVal <= 150) cat = 'UNHEALTHY FOR SENSITIVE GROUPS';
      else if (aqiVal <= 200) cat = 'UNHEALTHY';
      else if (aqiVal <= 300) cat = 'VERY UNHEALTHY';
      else cat = 'HAZARDOUS';

      return {
        status: 'OK',
        aqi: aqiVal,
        category: cat,
        dominantPollutant: domPol?.toUpperCase() || null,
        stationName: station,
        stationLatitude: stationLat,
        stationLongitude: stationLon,
        distanceKm: roundedDistance,
        pm25: iaqi.pm25?.v ?? null,
        pm10: iaqi.pm10?.v ?? null,
        co: iaqi.co?.v ?? null,
        no2: iaqi.no2?.v ?? null,
        o3: iaqi.o3?.v ?? null,
        timeString: d.time?.s || null
      };
    } catch (e: any) {
      console.error('[AQI Service Error]:', e.message);
      return {
        status: 'ERROR',
        message: e.message || 'AQI service error',
        aqi: null,
        category: null,
        dominantPollutant: null,
        stationName: null,
        stationLatitude: null,
        stationLongitude: null,
        distanceKm: null,
        pm25: null,
        pm10: null,
        co: null,
        no2: null,
        o3: null,
        timeString: null
      };
    }
  }
}

export const railwayProviderService = new RailwayProviderService();
