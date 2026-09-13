import {
  BackendLiveStatusResponse,
  BackendPnrResponse,
  BackendAvailabilityResponse,
  BackendTrainResponse,
  BackendAqiResponse,
  UnavailableResponse
} from '../models/railwayModels';

interface FallbackCity {
  name: string;
  lat: number;
  lon: number;
}

const FALLBACK_CITIES: FallbackCity[] = [
  { name: 'Mehsana / Ahmedabad (Gujarat)', lat: 23.0225, lon: 72.5714 },
  { name: 'Patna (Bihar)', lat: 25.6022, lon: 85.1376 },
  { name: 'Delhi / NCR', lat: 28.6139, lon: 77.2090 },
  { name: 'Chennai (Tamil Nadu)', lat: 13.0827, lon: 80.2707 },
  { name: 'Mumbai (Maharashtra)', lat: 19.0760, lon: 72.8777 },
  { name: 'Kolkata (West Bengal)', lat: 22.5726, lon: 88.3639 },
  { name: 'Bengaluru (Karnataka)', lat: 12.9716, lon: 77.5946 },
  { name: 'Hyderabad (Telangana)', lat: 17.3850, lon: 78.4867 }
];

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

function nearestFallbackCity(lat: number, lon: number): FallbackCity {
  let nearest = FALLBACK_CITIES[0];
  let minDist = haversineKm(lat, lon, nearest.lat, nearest.lon);
  for (let i = 1; i < FALLBACK_CITIES.length; i++) {
    const dist = haversineKm(lat, lon, FALLBACK_CITIES[i].lat, FALLBACK_CITIES[i].lon);
    if (dist < minDist) {
      minDist = dist;
      nearest = FALLBACK_CITIES[i];
    }
  }
  return nearest;
}

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

  async getAqi(lat: string, lon: string, city?: string): Promise<BackendAqiResponse | UnavailableResponse> {
    const aqiKey = this.getAqiKey();
    console.log(`[AQI Diagnostic]: AQICN_API_KEY exists: ${!!aqiKey}, sanitized length: ${aqiKey?.length || 0}, city: ${city || 'none'}`);

    if (!aqiKey) {
      return {
        status: 'UNAVAILABLE',
        message: 'AQI provider is not configured'
      };
    }

    try {
      let json: any = null;
      let res: Response | null = null;

      // 1. If city name is explicitly provided from manual search, try city feed first
      if (city && city.trim().length > 0) {
        const cityName = city.split(',')[0].trim();
        const url = `https://api.waqi.info/feed/${encodeURIComponent(cityName)}/?token=${aqiKey}`;
        res = await this.fetchWithTimeout(url);
        if (res.ok) {
          json = await res.json();
        }
      }

      // 2. Try exact coordinate geo feed if no city or city feed failed
      if (!json || json.status !== 'ok' || !json.data) {
        const url = `https://api.waqi.info/feed/geo:${lat};${lon}/?token=${aqiKey}`;
        res = await this.fetchWithTimeout(url);
        if (res.ok) {
          json = await res.json();
        }
      }

      // 3. Fallback to nearest coordinate city feed
      if (!json || json.status !== 'ok' || !json.data) {
        const latNum = parseFloat(lat);
        const lonNum = parseFloat(lon);
        const nearest = nearestFallbackCity(latNum, lonNum);
        const url = `https://api.waqi.info/feed/geo:${nearest.lat};${nearest.lon}/?token=${aqiKey}`;
        res = await this.fetchWithTimeout(url);
        if (res.ok) {
          json = await res.json();
        }
      }

      if (!res || !res.ok || json?.status !== 'ok' || !json?.data) {
        if (json?.status === 'error' && json?.data === 'Invalid key') {
          return { status: 'UNAVAILABLE', message: 'AQI provider authentication failed' };
        }
        return { status: 'UNAVAILABLE', message: 'AQI data not found for location' };
      }

      const d = json.data;
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
        pm25: iaqi.pm25?.v ?? null,
        pm10: iaqi.pm10?.v ?? null,
        co: iaqi.co?.v ?? null,
        no2: iaqi.no2?.v ?? null,
        o3: iaqi.o3?.v ?? null,
        timeString: d.time?.s || null
      };
    } catch (e: any) {
      console.error('[AQI Service Error]:', e.message);
      return { status: 'ERROR', message: e.message || 'AQI service error' };
    }
  }
}

export const railwayProviderService = new RailwayProviderService();
