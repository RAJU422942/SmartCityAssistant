import { CityAlert, AlertCategory, AlertSeverity, AlertStatus, CityAlertsResponse } from '../models/cityAlertModels';
import { railwayProviderService } from './railwayProviderService';

interface CacheEntry<T> {
  data: T;
  timestamp: number;
}

export class CityAlertsService {
  private weatherCache = new Map<string, CacheEntry<CityAlert[]>>();
  private gdacsCache: CacheEntry<CityAlert[]> | null = null;
  private weatherInFlight = new Map<string, Promise<CityAlert[]>>();
  private gdacsInFlight: Promise<CityAlert[]> | null = null;

  private readonly WEATHER_TTL = 15 * 60 * 1000; // 15 minutes
  private readonly GDACS_TTL = 10 * 60 * 1000;  // 10 minutes

  private haversineKm(lat1: number, lon1: number, lat2: number, lon2: number): number {
    const R = 6371;
    const dLat = (lat2 - lat1) * (Math.PI / 180);
    const dLon = (lon2 - lon1) * (Math.PI / 180);
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(lat1 * (Math.PI / 180)) * Math.cos(lat2 * (Math.PI / 180)) *
      Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }

  private async fetchGdacsAlerts(lat: number, lon: number): Promise<CityAlert[]> {
    const now = Date.now();
    if (this.gdacsCache && (now - this.gdacsCache.timestamp < this.GDACS_TTL)) {
      return this.filterGdacsByLocation(this.gdacsCache.data, lat, lon);
    }

    if (this.gdacsInFlight) {
      const alerts = await this.gdacsInFlight;
      return this.filterGdacsByLocation(alerts, lat, lon);
    }

    this.gdacsInFlight = (async () => {
      try {
        const res = await fetch('https://www.gdacs.org/xml/rss.xml', {
          headers: { 'User-Agent': 'SmartCityAssistant/1.0 contact@smartcity.com' },
          signal: AbortSignal.timeout(8000)
        });
        if (!res.ok) {
          console.warn('[GDACS]: Status not OK:', res.status);
          return [];
        }
        const xmlText = await res.text();
        const alerts: CityAlert[] = [];
        const itemRegex = /<item>([\s\S]*?)<\/item>/g;
        let match;
        const currentTime = new Date();

        while ((match = itemRegex.exec(xmlText)) !== null) {
          const itemContent = match[1];
          const getTag = (tag: string) => {
            const tMatch = new RegExp(`<${tag}[^>]*>([\\s\\S]*?)<\/${tag}>`, 'i').exec(itemContent);
            return tMatch ? tMatch[1].replace(/<!\[CDATA\[([\s\S]*?)\]\]>/g, '$1').trim() : '';
          };

          const title = getTag('title');
          const description = getTag('description');
          const link = getTag('link');
          const pubDateStr = getTag('pubDate');
          const fromDateStr = getTag('gdacs:from') || getTag('from') || pubDateStr;
          const toDateStr = getTag('gdacs:to') || getTag('to') || '';

          let geomLat = 0;
          let geomLon = 0;

          const pointStr = getTag('georss:point');
          if (pointStr) {
            const parts = pointStr.trim().split(/\s+/);
            if (parts.length >= 2) {
              geomLat = parseFloat(parts[0]);
              geomLon = parseFloat(parts[1]);
            }
          }

          if (!geomLat || !geomLon) {
            const latTag = getTag('geo:lat') || getTag('latitude') || getTag('gdacs:latitude');
            const lonTag = getTag('geo:long') || getTag('geo:lon') || getTag('longitude') || getTag('gdacs:longitude');
            if (latTag && lonTag) {
              geomLat = parseFloat(latTag);
              geomLon = parseFloat(lonTag);
            }
          }

          const startTime = fromDateStr ? new Date(fromDateStr) : new Date();
          const endTime = toDateStr ? new Date(toDateStr) : new Date(startTime.getTime() + 7 * 24 * 60 * 1000); // default 7 days validity if not specified

          let status: AlertStatus = 'ACTIVE';
          if (endTime < currentTime) {
            status = 'EXPIRED';
          } else if (startTime > currentTime) {
            status = 'UPCOMING';
          } else {
            status = 'ACTIVE';
          }

          let severity: AlertSeverity = 'MODERATE';
          const lowerTitle = title.toLowerCase();
          if (lowerTitle.includes('red') || lowerTitle.includes('severe') || lowerTitle.includes('magnitude 6') || lowerTitle.includes('catastrophe')) {
            severity = 'CRITICAL';
          } else if (lowerTitle.includes('orange') || lowerTitle.includes('high') || lowerTitle.includes('moderate')) {
            severity = 'HIGH';
          } else {
            severity = 'MODERATE';
          }

          alerts.push({
            id: `gdacs_${Math.random().toString(36).substring(2, 9)}`,
            type: 'DISASTER',
            title: title || 'Disaster Alert',
            description: description || 'GDACS disaster event reported.',
            category: 'DISASTER',
            severity,
            status,
            startTime: startTime.toISOString(),
            endTime: endTime.toISOString(),
            lastUpdated: new Date().toISOString(),
            latitude: !isNaN(geomLat) ? geomLat : undefined,
            longitude: !isNaN(geomLon) ? geomLon : undefined,
            sourceName: 'GDACS',
            sourceUrl: link || 'https://www.gdacs.org',
            isVerified: true
          });
        }

        this.gdacsCache = { data: alerts, timestamp: Date.now() };
        return alerts;
      } catch (e: any) {
        console.warn('[GDACS Error]:', e.message);
        return [];
      } finally {
        this.gdacsInFlight = null;
      }
    })();

    const allAlerts = await this.gdacsInFlight;
    return this.filterGdacsByLocation(allAlerts, lat, lon);
  }

  private filterGdacsByLocation(allAlerts: CityAlert[], lat: number, lon: number): CityAlert[] {
    const DEFAULT_RADIUS_KM = 300;
    const filtered: CityAlert[] = [];

    for (const alert of allAlerts) {
      if (alert.latitude != null && alert.longitude != null && alert.latitude !== 0 && alert.longitude !== 0) {
        const dist = this.haversineKm(lat, lon, alert.latitude, alert.longitude);
        const isMajor = alert.title.toLowerCase().includes('cyclone') || alert.title.toLowerCase().includes('earthquake') || alert.title.toLowerCase().includes('storm');
        const maxRadius = isMajor ? 600 : DEFAULT_RADIUS_KM;

        if (dist <= maxRadius) {
          filtered.push({
            ...alert,
            locationName: `Local Event (~${Math.round(dist)} km away)`
          });
        } else if (dist <= 1500) {
          filtered.push({
            ...alert,
            title: `[Regional] ${alert.title}`,
            locationName: `Regional Event (~${Math.round(dist)} km away)`
          });
        }
      }
    }
    return filtered;
  }

  private async fetchMetNorwayWeatherAlerts(lat: number, lon: number): Promise<CityAlert[]> {
    const normLat = Number(lat.toFixed(4));
    const normLon = Number(lon.toFixed(4));
    const cacheKey = `${normLat},${normLon}`;
    const now = Date.now();

    if (this.weatherCache.has(cacheKey)) {
      const entry = this.weatherCache.get(cacheKey)!;
      if (now - entry.timestamp < this.WEATHER_TTL) {
        return entry.data;
      }
    }

    if (this.weatherInFlight.has(cacheKey)) {
      return this.weatherInFlight.get(cacheKey)!;
    }

    const promise = (async () => {
      try {
        const url = `https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=${normLat}&lon=${normLon}`;
        const res = await fetch(url, {
          headers: { 'User-Agent': 'SmartCityAssistant/1.0 contact@smartcity.com' },
          signal: AbortSignal.timeout(6000)
        });

        if (res.status === 429) {
          console.warn('[MET Norway]: Rate limited (429)');
          return [];
        }
        if (!res.ok) {
          console.warn('[MET Norway]: Status not OK:', res.status);
          return [];
        }

        const json: any = await res.json();
        const timeseries = json?.properties?.timeseries;
        if (!Array.isArray(timeseries) || timeseries.length === 0) {
          return [];
        }

        const alerts: CityAlert[] = [];
        const currentTime = new Date();

        // Check first few timeseries records for weather conditions
        for (let i = 0; i < Math.min(timeseries.length, 12); i++) {
          const entry = timeseries[i];
          const timeStr = entry.time;
          const details = entry?.data?.instant?.details;
          const next1h = entry?.data?.next_1_hours;
          const next6h = entry?.data?.next_6_hours;

          if (!details) continue;

          const temp = details.air_temperature; // Celsius
          const windSpeedMs = details.wind_speed; // m/s
          const windSpeedKmh = windSpeedMs != null ? windSpeedMs * 3.6 : 0;
          const precipAmount = next1h?.details?.precipitation_amount ?? next6h?.details?.precipitation_amount ?? 0;
          const precipProb = next1h?.details?.probability_of_precipitation ?? next6h?.details?.probability_of_precipitation ?? 0;
          const symbolCode = next1h?.summary?.symbol_code ?? next6h?.summary?.symbol_code ?? '';

          const validTime = timeStr ? new Date(timeStr) : currentTime;
          if (validTime < currentTime && (currentTime.getTime() - validTime.getTime() > 3 * 3600 * 1000)) {
            continue; // skip old timestamps
          }

          // Threshold checks
          // 1. Heavy Rain
          if (precipProb >= 70 || precipAmount >= 15) {
            alerts.push({
              id: `met_rain_${i}_${Date.now()}`,
              type: 'WEATHER',
              title: 'Weather Advisory: Heavy Rain',
              description: `Heavy rainfall forecast with probability ${precipProb}% and amount ${precipAmount} mm. Exercise caution during travel.`,
              category: 'WEATHER',
              severity: precipAmount >= 30 ? 'HIGH' : 'MODERATE',
              status: 'ACTIVE',
              startTime: timeStr,
              endTime: new Date(validTime.getTime() + 3 * 3600 * 1000).toISOString(),
              lastUpdated: new Date().toISOString(),
              latitude: normLat,
              longitude: normLon,
              sourceName: 'MET Norway',
              sourceUrl: 'https://www.met.no',
              isVerified: true
            });
            break; // add once per check
          }

          // 2. Thunderstorm
          if (symbolCode.includes('thunder')) {
            alerts.push({
              id: `met_thunder_${i}_${Date.now()}`,
              type: 'WEATHER',
              title: 'Weather Advisory: Thunderstorm',
              description: `Thunderstorm conditions detected in weather forecast (${symbolCode}). Stay indoors and avoid open areas.`,
              category: 'WEATHER',
              severity: 'HIGH',
              status: 'ACTIVE',
              startTime: timeStr,
              endTime: new Date(validTime.getTime() + 3 * 3600 * 1000).toISOString(),
              lastUpdated: new Date().toISOString(),
              latitude: normLat,
              longitude: normLon,
              sourceName: 'MET Norway',
              sourceUrl: 'https://www.met.no',
              isVerified: true
            });
            break;
          }

          // 3. Strong Wind (>= 40 km/h i.e. 11.1 m/s)
          if (windSpeedKmh >= 40) {
            alerts.push({
              id: `met_wind_${i}_${Date.now()}`,
              type: 'WEATHER',
              title: 'Weather Advisory: Strong Wind',
              description: `Strong winds forecast at ${Math.round(windSpeedKmh)} km/h. Secure loose outdoor objects.`,
              category: 'WEATHER',
              severity: windSpeedKmh >= 60 ? 'HIGH' : 'MODERATE',
              status: 'ACTIVE',
              startTime: timeStr,
              endTime: new Date(validTime.getTime() + 3 * 3600 * 1000).toISOString(),
              lastUpdated: new Date().toISOString(),
              latitude: normLat,
              longitude: normLon,
              sourceName: 'MET Norway',
              sourceUrl: 'https://www.met.no',
              isVerified: true
            });
            break;
          }

          // 4. Extreme Heat (>= 40°C)
          if (temp != null && temp >= 40) {
            alerts.push({
              id: `met_heat_${i}_${Date.now()}`,
              type: 'WEATHER',
              title: 'Weather Advisory: Extreme Heat',
              description: `High temperature forecast at ${temp}°C. Stay hydrated and avoid prolonged sun exposure.`,
              category: 'WEATHER',
              severity: 'HIGH',
              status: 'ACTIVE',
              startTime: timeStr,
              endTime: new Date(validTime.getTime() + 6 * 3600 * 1000).toISOString(),
              lastUpdated: new Date().toISOString(),
              latitude: normLat,
              longitude: normLon,
              sourceName: 'MET Norway',
              sourceUrl: 'https://www.met.no',
              isVerified: true
            });
            break;
          }

          // 5. Extreme Cold (<= 5°C)
          if (temp != null && temp <= 5) {
            alerts.push({
              id: `met_cold_${i}_${Date.now()}`,
              type: 'WEATHER',
              title: 'Weather Advisory: Extreme Cold',
              description: `Low temperature forecast at ${temp}°C. Take appropriate warm clothing precautions.`,
              category: 'WEATHER',
              severity: 'MODERATE',
              status: 'ACTIVE',
              startTime: timeStr,
              endTime: new Date(validTime.getTime() + 6 * 3600 * 1000).toISOString(),
              lastUpdated: new Date().toISOString(),
              latitude: normLat,
              longitude: normLon,
              sourceName: 'MET Norway',
              sourceUrl: 'https://www.met.no',
              isVerified: true
            });
            break;
          }
        }

        this.weatherCache.set(cacheKey, { data: alerts, timestamp: Date.now() });
        return alerts;
      } catch (e: any) {
        console.warn('[MET Norway Error]:', e.message);
        return [];
      } finally {
        this.weatherInFlight.delete(cacheKey);
      }
    })();

    this.weatherInFlight.set(cacheKey, promise);
    return promise;
  }

  private async fetchAqiAlerts(lat: number, lon: number): Promise<CityAlert[]> {
    try {
      const aqiRes: any = await railwayProviderService.getAqi(lat.toString(), lon.toString());
      if (aqiRes && aqiRes.status === 'OK' && typeof aqiRes.aqi === 'number') {
        const aqiVal = aqiRes.aqi;
        const categoryName = aqiRes.category || (aqiVal <= 50 ? 'Good' : aqiVal <= 100 ? 'Moderate' : aqiVal <= 150 ? 'Unhealthy for Sensitive Groups' : aqiVal <= 200 ? 'Unhealthy' : 'Very Unhealthy');

        if (aqiVal > 50) {
          let severity: AlertSeverity = 'MODERATE';
          if (aqiVal > 300) severity = 'CRITICAL';
          else if (aqiVal > 200) severity = 'HIGH';
          else if (aqiVal > 150) severity = 'MODERATE';
          else severity = 'LOW';

          return [{
            id: `aqi_alert_${Date.now()}`,
            type: 'AIR_QUALITY',
            title: `Air Quality Advisory: ${categoryName}`,
            description: `Air Quality Index is ${aqiVal} (${categoryName}). ${aqiVal > 150 ? 'Sensitive individuals and general public should reduce prolonged outdoor exposure.' : 'Unusually sensitive people may want to reduce outdoor exertion.'}`,
            category: 'AIR_QUALITY',
            severity,
            status: 'ACTIVE',
            startTime: new Date().toISOString(),
            lastUpdated: aqiRes.timeString ? new Date(aqiRes.timeString).toISOString() : new Date().toISOString(),
            locationName: aqiRes.stationName || 'Local Air Quality Station',
            latitude: lat,
            longitude: lon,
            sourceName: aqiRes.source || 'CPCB / Open-Meteo • CAMS',
            sourceUrl: 'https://aqicn.org',
            isVerified: true
          }];
        }
      }
      return [];
    } catch (e: any) {
      console.warn('[AQI Alert Error]:', e.message);
      return [];
    }
  }

  async getAlerts(
    lat?: number,
    lon?: number,
    city?: string,
    district?: string,
    category?: string
  ): Promise<CityAlertsResponse> {
    const targetLat = lat ?? 23.58; // Default to a valid neutral location if none provided
    const targetLon = lon ?? 72.36;

    console.log(`[CityAlerts Service]: location=${city || 'Custom Location'}, lat=${targetLat}, lon=${targetLon}, category=${category || 'ALL'}`);

    const [gdacsAlerts, metAlerts, aqiAlerts] = await Promise.all([
      this.fetchGdacsAlerts(targetLat, targetLon),
      this.fetchMetNorwayWeatherAlerts(targetLat, targetLon),
      this.fetchAqiAlerts(targetLat, targetLon)
    ]);

    let allAlerts = [...gdacsAlerts, ...metAlerts, ...aqiAlerts];

    // Filter out expired alerts by default
    allAlerts = allAlerts.filter(a => a.status !== 'EXPIRED');

    if (category) {
      const upperCat = category.toUpperCase();
      allAlerts = allAlerts.filter(a => a.category.toUpperCase() === upperCat);
    }

    // Sorting rules: ACTIVE first, then severity (CRITICAL > HIGH > MODERATE > LOW > INFO), then newest update
    const severityRank: Record<AlertSeverity, number> = {
      CRITICAL: 5,
      SEVERE: 5,
      HIGH: 4,
      MODERATE: 3,
      LOW: 2,
      INFO: 1
    };

    allAlerts.sort((a, b) => {
      if (a.status === 'ACTIVE' && b.status !== 'ACTIVE') return -1;
      if (b.status === 'ACTIVE' && a.status !== 'ACTIVE') return 1;

      const rankA = severityRank[a.severity] || 0;
      const rankB = severityRank[b.severity] || 0;
      if (rankA !== rankB) return rankB - rankA;

      const timeA = a.lastUpdated ? new Date(a.lastUpdated).getTime() : 0;
      const timeB = b.lastUpdated ? new Date(b.lastUpdated).getTime() : 0;
      return timeB - timeA;
    });

    return {
      status: 'OK',
      location: {
        latitude: targetLat,
        longitude: targetLon,
        city: city || undefined,
        district: district || undefined
      },
      alerts: allAlerts,
      updatedAt: new Date().toISOString()
    };
  }
}

export const cityAlertsService = new CityAlertsService();
