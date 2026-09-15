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

function getNumericField(rec: any, ...keys: string[]): number {
  for (const k of keys) {
    for (const recKey of Object.keys(rec)) {
      if (recKey.toLowerCase() === k.toLowerCase()) {
        const val = parseFloat(rec[recKey]);
        if (!isNaN(val)) return val;
      }
    }
  }
  return NaN;
}

function getStringField(rec: any, ...keys: string[]): string | undefined {
  for (const k of keys) {
    for (const recKey of Object.keys(rec)) {
      if (recKey.toLowerCase() === k.toLowerCase()) {
        return rec[recKey]?.toString();
      }
    }
  }
  return undefined;
}

export class RailwayProviderService {
  private cachedRecords: any[] | null = null;
  private cacheTimestamp: number = 0;
  private activeFetchPromise: Promise<any[] | null> | null = null;
  private CACHE_DURATION_MS = 15 * 60 * 1000; // 15 minutes cache

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

  private getMapsKey(): string | undefined {
    const rawKey = process.env.GOOGLE_MAPS_API_KEY || process.env.MAPS_API_KEY;
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

  private async fetchCpcbRecords(apiKey: string): Promise<any[] | null> {
    const now = Date.now();
    const hasCache = !!this.cachedRecords;
    const cacheAgeSec = hasCache ? Math.round((now - this.cacheTimestamp) / 1000) : -1;
    const isFresh = hasCache && (now - this.cacheTimestamp < this.CACHE_DURATION_MS);

    console.log(`[AQI CACHE] cache exists = ${hasCache}, cache age = ${cacheAgeSec}s, cached station count = ${hasCache ? this.cachedRecords?.length : 0}`);

    if (hasCache && isFresh) {
      console.log(`[CPCB Cache]: Returning server-side fresh cached records.`);
      return this.cachedRecords;
    }

    if (this.activeFetchPromise) {
      console.log(`[CPCB Cache]: Reusing active in-flight fetch promise for CPCB records.`);
      return this.activeFetchPromise;
    }

    this.activeFetchPromise = (async () => {
      try {
        const resourceId = '3b01bcb8-0b14-4abf-b1f2-8ec91b205104';
        const url = `https://api.data.gov.in/resource/${resourceId}?api-key=REDACTED&format=json&limit=1000`;
        const realUrl = `https://api.data.gov.in/resource/${resourceId}?api-key=${apiKey}&format=json&limit=1000`;
        console.log(`[CPCB API URL]: ${url}`);

        const res = await this.fetchWithTimeout(realUrl);
        console.log(`[CPCB FETCH] HTTP status = ${res.status} ${res.statusText}`);

        if (res.status === 429) {
          console.warn(`[CPCB Rate Limit]: HTTP 429 Too Many Requests received from data.gov.in.`);
          if (this.cachedRecords) {
            console.log(`[CPCB Fallback]: Falling back to previously cached CPCB records due to HTTP 429.`);
            return this.cachedRecords;
          }
        }

        if (!res.ok) {
          const errBody = await res.text().catch(() => '');
          console.error(`[CPCB Fetch Failed]: HTTP status ${res.status}, body: ${errBody.substring(0, 200)}`);
          if (this.cachedRecords) {
            console.log(`[CPCB Fallback]: Falling back to previously cached CPCB records due to HTTP ${res.status}`);
            return this.cachedRecords;
          }
          return null;
        }

        const json: any = await res.json();
        const records = json?.records || json?.data || json?.results || (Array.isArray(json) ? json : null);
        console.log(`[CPCB FETCH] records received = ${Array.isArray(records) ? records.length : 0}`);

        if (Array.isArray(records) && records.length > 0) {
          this.cachedRecords = records;
          this.cacheTimestamp = Date.now();
          console.log(`[CPCB Cache]: Successfully fetched and cached ${records.length} records.`);
          return records;
        }

        return this.cachedRecords;
      } catch (err: any) {
        console.error('[CPCB Fetch Error Exception]:', err.message);
        return this.cachedRecords;
      } finally {
        this.activeFetchPromise = null;
      }
    })();

    return this.activeFetchPromise;
  }

  async getAqi(lat: string, lon: string): Promise<BackendAqiResponse | UnavailableResponse> {
    const requestId = 'AQI-' + Date.now() + '-' + Math.random().toString(36).substring(2, 7);
    console.log(`[AQI REQUEST] requestId=${requestId}\nlat=${lat}\nlon=${lon}`);

    const apiKey = this.getCpcbKey();
    const userLat = parseFloat(lat);
    const userLon = parseFloat(lon);

    if (!apiKey) {
      const errResp: UnavailableResponse = {
        status: 'UNAVAILABLE',
        message: 'CPCB AQI provider is not configured'
      };
      console.log(`[RESPONSE] requestId=${requestId} final status=${errResp.status}`);
      return errResp;
    }

    try {
      const records = await this.fetchCpcbRecords(apiKey);
      if (!Array.isArray(records) || records.length === 0) {
        const errResp: BackendAqiResponse = {
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
        console.log(`[RESPONSE] requestId=${requestId} final status=${errResp.status}`);
        return errResp;
      }

      // Group records by station key (station name + lat + lon)
      const stationMap = new Map<string, any>();
      let validRecordsCount = 0;

      for (const rec of records) {
        const stationName = getStringField(rec, 'station', 'station_name', 'location', 'city') || 'Unknown Station';
        const latitude = getNumericField(rec, 'latitude', 'lat', 'lat_value', 'y', 'coordinate_lat');
        const longitude = getNumericField(rec, 'longitude', 'lng', 'lon', 'long', 'x', 'coordinate_long');
        if (isNaN(latitude) || isNaN(longitude)) continue;

        validRecordsCount++;
        const key = `${stationName}_${latitude}_${longitude}`;
        if (!stationMap.has(key)) {
          stationMap.set(key, {
            stationName,
            latitude,
            longitude,
            city: getStringField(rec, 'city'),
            state: getStringField(rec, 'state'),
            lastUpdate: getStringField(rec, 'last_update', 'timestamp', 'time'),
            pollutants: {}
          });
        }

        const stationObj = stationMap.get(key);
        const polId = (getStringField(rec, 'pollutant_id', 'pollutant', 'parameter') || '').toLowerCase();
        const polVal = getNumericField(rec, 'pollutant_avg', 'pollutant_max', 'aqi', 'value', 'avg', 'max');
        if (!isNaN(polVal)) {
          if (polId) {
            stationObj.pollutants[polId] = polVal;
          }
          if (polId === 'aqi' || polId === 'overall_aqi' || polId === 'index') {
            stationObj.aqi = polVal;
          }
        }
      }

      console.log(`[PARSER] valid records = ${validRecordsCount}, unique stations = ${stationMap.size}`);

      if (stationMap.size === 0) {
        const errResp: BackendAqiResponse = {
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
        console.log(`[RESPONSE] requestId=${requestId} final status=${errResp.status}`);
        return errResp;
      }

      // Find nearest station to userLat, userLon
      const stationsList: any[] = [];
      for (const station of stationMap.values()) {
        const dist = haversineKm(userLat, userLon, station.latitude, station.longitude);
        stationsList.push({ ...station, distanceKm: Math.round(dist * 10) / 10 });
      }

      stationsList.sort((a, b) => a.distanceKm - b.distanceKm);

      const nearestStation = stationsList[0];
      const roundedDistance = nearestStation.distanceKm;

      console.log(`[NEAREST] nearest station = ${nearestStation.stationName}, nearest distance = ${roundedDistance} km`);
      console.log(`[SELECTION] MAX_AQI_STATION_DISTANCE_KM = ${MAX_AQI_STATION_DISTANCE_KM}, selected station = ${nearestStation.stationName}, selected distance = ${roundedDistance} km`);

      if (roundedDistance > MAX_AQI_STATION_DISTANCE_KM) {
        const errResp: BackendAqiResponse = {
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
        console.log(`[RESPONSE] requestId=${requestId} final status=${errResp.status}`);
        return errResp;
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

      const successResp: BackendAqiResponse = {
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

      console.log(`[RESPONSE] requestId=${requestId} final status=${successResp.status} station=${successResp.stationName} aqi=${successResp.aqi}`);
      console.log(`[EXACT OBJECT RETURNED]:`, JSON.stringify(successResp));
      return successResp;

    } catch (e: any) {
      console.error('[CPCB AQI Service Error]:', e.message);
      const errResp: BackendAqiResponse = {
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
      console.log(`[RESPONSE] requestId=${requestId} final status=${errResp.status}`);
      return errResp;
    }
  }

  async getParking(lat: string, lon: string): Promise<any> {
    const mapsKey = this.getMapsKey();
    const userLat = parseFloat(lat);
    const userLon = parseFloat(lon);

    if (!mapsKey) {
      return { status: 'ERROR', results: [], message: 'Google Maps API key is not configured on backend.' };
    }

    try {
      const url = `https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=${lat},${lon}&radius=5000&type=parking&key=${mapsKey}`;
      const res = await this.fetchWithTimeout(url);
      if (!res.ok) {
        return { status: 'ERROR', results: [], message: 'Failed to fetch parking locations.' };
      }

      const data: any = await res.json();
      if (data.status !== 'OK' && data.status !== 'ZERO_RESULTS') {
        return { status: 'ERROR', results: [], message: data.error_message || 'Places API error' };
      }

      if (!data.results || data.results.length === 0) {
        return { status: 'NO_RESULTS', results: [] };
      }

      const results = data.results.map((p: any) => {
        const pLat = p.geometry?.location?.lat ?? userLat;
        const pLon = p.geometry?.location?.lng ?? userLon;
        const dist = haversineKm(userLat, userLon, pLat, pLon);
        const isOpen = p.opening_hours?.open_now;
        return {
          name: p.name || 'Parking Area',
          address: p.vicinity || p.formatted_address || 'Nearby',
          latitude: pLat,
          longitude: pLon,
          distanceKm: Math.round(dist * 10) / 10,
          status: isOpen === true ? 'Open' : (isOpen === false ? 'Closed' : 'Available'),
          source: 'Google Places'
        };
      });

      results.sort((a: any, b: any) => a.distanceKm - b.distanceKm);
      return { status: 'OK', results };
    } catch (e: any) {
      return { status: 'ERROR', results: [], message: e.message || 'Error fetching parking' };
    }
  }

  async getEvCharging(lat: string, lon: string): Promise<any> {
    const mapsKey = this.getMapsKey();
    const userLat = parseFloat(lat);
    const userLon = parseFloat(lon);

    if (!mapsKey) {
      return { status: 'ERROR', results: [], message: 'Google Maps API key is not configured on backend.' };
    }

    try {
      const url = `https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=${lat},${lon}&radius=5000&keyword=ev%20charging%20station&key=${mapsKey}`;
      const res = await this.fetchWithTimeout(url);
      if (!res.ok) {
        return { status: 'ERROR', results: [], message: 'Failed to fetch EV charging stations.' };
      }

      const data: any = await res.json();
      if (data.status !== 'OK' && data.status !== 'ZERO_RESULTS') {
        return { status: 'ERROR', results: [], message: data.error_message || 'Places API error' };
      }

      if (!data.results || data.results.length === 0) {
        return { status: 'NO_RESULTS', results: [] };
      }

      const results = data.results.map((p: any) => {
        const pLat = p.geometry?.location?.lat ?? userLat;
        const pLon = p.geometry?.location?.lng ?? userLon;
        const dist = haversineKm(userLat, userLon, pLat, pLon);
        const isOpen = p.opening_hours?.open_now;
        return {
          name: p.name || 'EV Charging Station',
          address: p.vicinity || p.formatted_address || 'Nearby',
          latitude: pLat,
          longitude: pLon,
          distanceKm: Math.round(dist * 10) / 10,
          status: isOpen === true ? 'Open' : (isOpen === false ? 'Closed' : 'Available'),
          source: 'Google Places'
        };
      });

      results.sort((a: any, b: any) => a.distanceKm - b.distanceKm);
      return { status: 'OK', results };
    } catch (e: any) {
      return { status: 'ERROR', results: [], message: e.message || 'Error fetching EV charging' };
    }
  }
}

export const railwayProviderService = new RailwayProviderService();
