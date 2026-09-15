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

function getCpcbCategory(aqi: number): string {
  if (aqi <= 50) return 'GOOD';
  if (aqi <= 100) return 'SATISFACTORY';
  if (aqi <= 200) return 'MODERATE';
  if (aqi <= 300) return 'POOR';
  if (aqi <= 400) return 'VERY POOR';
  return 'SEVERE';
}

export class RailwayProviderService {
  private getApiKey(): string | undefined {
    const rawKey = process.env.RAILKIT_API_KEY;
    if (!rawKey) return undefined;
    return rawKey.trim().replace(/^["']|["']$/g, '');
  }

  private getCpcbKey(): string | undefined {
    const rawKey = process.env.CPCB_API_KEY || process.env.AQICN_API_KEY;
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
    const apiKey = this.getCpcbKey();
    const userLat = parseFloat(lat);
    const userLon = parseFloat(lon);
    console.log(`[CPCB AQI]: API key exists: ${!!apiKey}, requested lat: ${lat}, lon: ${lon}`);

    if (!apiKey) {
      return {
        status: 'UNAVAILABLE',
        message: 'CPCB AQI provider is not configured',
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
        so2: null,
        nh3: null,
        timeString: null,
        source: 'CPCB'
      };
    }

    try {
      const resourceId = '3b01bcb8-0b14-4abf-b1f2-8ec91b205104';
      const url = `https://api.data.gov.in/resource/${resourceId}?api-key=${apiKey}&format=json&limit=1000`;
      const res = await this.fetchWithTimeout(url);

      if (!res.ok) {
        return {
          status: 'UNAVAILABLE',
          message: 'CPCB AQI data service is temporarily unavailable.',
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
          so2: null,
          nh3: null,
          timeString: null,
          source: 'CPCB'
        };
      }

      const json: any = await res.json();
      const records = json?.records;
      if (!Array.isArray(records) || records.length === 0) {
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
          so2: null,
          nh3: null,
          timeString: null,
          message: 'No CPCB air quality monitoring stations found.',
          source: 'CPCB'
        };
      }

      // Log first record fields to verify CPCB structure in logs
      console.log('[CPCB Sample Record]:', JSON.stringify(records[0]));

      // Group records by station key (station name + lat + lon)
      const stationMap = new Map<string, any>();

      for (const rec of records) {
        const stationName = rec.station || rec.city || 'Unknown Station';
        const latitude = parseFloat(rec.latitude ?? rec.lat ?? rec.lat_value ?? rec.y ?? 'NaN');
        const longitude = parseFloat(rec.longitude ?? rec.lng ?? rec.lon ?? rec.long ?? rec.x ?? 'NaN');
        if (isNaN(latitude) || isNaN(longitude)) continue;

        const key = `${stationName}_${latitude}_${longitude}`;
        if (!stationMap.has(key)) {
          stationMap.set(key, {
            stationName,
            latitude,
            longitude,
            city: rec.city,
            state: rec.state,
            lastUpdate: rec.last_update,
            pollutants: {}
          });
        }

        const stationObj = stationMap.get(key);
        const polId = (rec.pollutant_id || '').toLowerCase();
        const polVal = parseFloat(rec.pollutant_avg ?? rec.pollutant_max ?? rec.aqi ?? '0');
        if (!isNaN(polVal)) {
          stationObj.pollutants[polId] = polVal;
          if (polId === 'aqi' || polId === 'overall_aqi') {
            stationObj.aqi = polVal;
          }
        }
      }

      if (stationMap.size === 0) {
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
          so2: null,
          nh3: null,
          timeString: null,
          message: 'No valid CPCB stations with coordinates found.',
          source: 'CPCB'
        };
      }

      // Find nearest station to userLat, userLon
      let nearestStation: any = null;
      let minDistance = Infinity;

      for (const station of stationMap.values()) {
        const dist = haversineKm(userLat, userLon, station.latitude, station.longitude);
        if (dist < minDistance) {
          minDistance = dist;
          nearestStation = station;
        }
      }

      const roundedDistance = Math.round(minDistance * 10) / 10;
      console.log(`[CPCB Nearest Station]: found="${nearestStation.stationName}" at dist=${roundedDistance} km (threshold=${MAX_AQI_STATION_DISTANCE_KM} km)`);

      if (roundedDistance > MAX_AQI_STATION_DISTANCE_KM) {
        return {
          status: 'NO_NEARBY_AQI_STATION',
          aqi: null,
          category: null,
          dominantPollutant: null,
          stationName: null,
          stationLatitude: nearestStation.latitude,
          stationLongitude: nearestStation.longitude,
          distanceKm: roundedDistance,
          pm25: null,
          pm10: null,
          co: null,
          no2: null,
          o3: null,
          so2: null,
          nh3: null,
          timeString: null,
          message: `No nearby CPCB air quality monitoring station was found for your current location (~${roundedDistance} km away).`,
          source: 'CPCB'
        };
      }

      const pols = nearestStation.pollutants;
      let aqiVal = nearestStation.aqi;
      if (typeof aqiVal !== 'number' || isNaN(aqiVal)) {
        const vals = Object.values(pols).filter((v): v is number => typeof v === 'number');
        aqiVal = vals.length > 0 ? Math.round(Math.max(...vals)) : 50;
      }

      let dominantPol = 'pm25';
      let maxVal = -1;
      for (const [p, v] of Object.entries(pols)) {
        if (typeof v === 'number' && v > maxVal) {
          maxVal = v;
          dominantPol = p;
        }
      }

      const category = getCpcbCategory(aqiVal);

      return {
        status: 'OK',
        aqi: aqiVal,
        category,
        dominantPollutant: dominantPol.toUpperCase(),
        stationName: nearestStation.stationName,
        stationLatitude: nearestStation.latitude,
        stationLongitude: nearestStation.longitude,
        distanceKm: roundedDistance,
        pm25: pols['pm2.5'] ?? pols['pm25'] ?? null,
        pm10: pols['pm10'] ?? null,
        co: pols['co'] ?? null,
        no2: pols['no2'] ?? null,
        o3: pols['o3'] ?? null,
        so2: pols['so2'] ?? null,
        nh3: pols['nh3'] ?? null,
        timeString: nearestStation.lastUpdate || null,
        source: 'CPCB'
      };
    } catch (e: any) {
      console.error('[CPCB AQI Service Error]:', e.message);
      return {
        status: 'ERROR',
        message: e.message || 'CPCB AQI service error',
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
        so2: null,
        nh3: null,
        timeString: null,
        source: 'CPCB'
      };
    }
  }
}

export const railwayProviderService = new RailwayProviderService();
