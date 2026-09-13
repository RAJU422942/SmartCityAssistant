import { CityAlert, AlertCategory, AlertSeverity, AlertStatus, CityAlertsResponse } from '../models/cityAlertModels';
import { railwayProviderService } from './railwayProviderService';

export class CityAlertsService {
  private async fetchGdacsAlerts(lat: number, lon: number): Promise<CityAlert[]> {
    try {
      const res = await fetch('https://www.gdacs.org/xml/rss.xml', { signal: AbortSignal.timeout(6000) });
      if (!res.ok) return [];
      const xmlText = await res.text();

      const alerts: CityAlert[] = [];
      const itemRegex = /<item>([\s\S]*?)<\/item>/g;
      let match;
      while ((match = itemRegex.exec(xmlText)) !== null) {
        const itemContent = match[1];
        const getTag = (tag: string) => {
          const tMatch = new RegExp(`<${tag}[^>]*>([\\s\\S]*?)<\/${tag}>`, 'i').exec(itemContent);
          return tMatch ? tMatch[1].replace(/<!\[CDATA\[([\s\S]*?)\]\]>/g, '$1').trim() : '';
        };

        const title = getTag('title');
        const description = getTag('description');
        const link = getTag('link');
        const pubDate = getTag('pubDate');
        const pointStr = getTag('georss:point');
        const parts = pointStr.split(/\s+/);
        const geomLat = parseFloat(parts[0] || '0');
        const geomLon = parseFloat(parts[1] || '0');

        const dist = (geomLat && geomLon) ? this.haversineKm(lat, lon, geomLat, geomLon) : 99999;

        // Centralized configurable relevance radius (400 km for local disasters, 600 km for major storms/cyclones)
        const DEFAULT_DISASTER_RADIUS_KM = 400;
        const lowerTitle = title.toLowerCase();
        const isMajorStorm = lowerTitle.includes('cyclone') || lowerTitle.includes('storm') || lowerTitle.includes('typhoon') || lowerTitle.includes('hurricane');
        const maxRadius = isMajorStorm ? 600 : DEFAULT_DISASTER_RADIUS_KM;

        const included = dist <= maxRadius;
        console.log(`[GDACS Filtering]: event="${title.substring(0, 30)}...", eventLat=${geomLat}, eventLon=${geomLon}, targetLat=${lat}, targetLon=${lon}, distance=${Math.round(dist)}km, maxRadius=${maxRadius}km, included=${included}`);

        if (included) {
          let severity: AlertSeverity = 'MODERATE';
          if (lowerTitle.includes('red') || lowerTitle.includes('severe') || lowerTitle.includes('magnitude 6')) {
            severity = 'CRITICAL';
          } else if (lowerTitle.includes('orange') || lowerTitle.includes('moderate')) {
            severity = 'HIGH';
          }

          alerts.push({
            id: `gdacs_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`,
            title: title || 'Disaster Alert',
            description: description || 'GDACS disaster event reported within regional relevance radius.',
            category: 'DISASTER',
            severity,
            latitude: geomLat || undefined,
            longitude: geomLon || undefined,
            issuedAt: pubDate ? new Date(pubDate).toISOString() : new Date().toISOString(),
            sourceName: 'GDACS',
            sourceUrl: link || 'https://www.gdacs.org',
            status: 'ACTIVE',
            isVerified: true
          });
        }
      }
      return alerts;
    } catch (e: any) {
      console.warn('[CityAlertsService GDACS Error]:', e.message);
      return [];
    }
  }

  private async fetchOpenMeteoAlerts(lat: number, lon: number): Promise<CityAlert[]> {
    try {
      const url = `https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lon}&current=temperature_2m,relative_humidity_2m,precipitation,weather_code,wind_speed_10m`;
      const res = await fetch(url, { signal: AbortSignal.timeout(5000) });
      if (!res.ok) return [];
      const json: any = await res.json();
      const current = json.current;
      if (!current) return [];

      const alerts: CityAlert[] = [];
      const temp = current.temperature_2m;
      const precip = current.precipitation;
      const wind = current.wind_speed_10m;

      if (precip != null && precip > 15.0) {
        alerts.push({
          id: `om_precip_${Date.now()}`,
          title: 'Heavy Precipitation Alert',
          description: `Heavy rainfall/precipitation detected (${precip} mm). Exercise caution while traveling.`,
          category: 'WEATHER',
          severity: precip > 35 ? 'HIGH' : 'MODERATE',
          latitude: lat,
          longitude: lon,
          issuedAt: new Date().toISOString(),
          sourceName: 'Open-Meteo',
          sourceUrl: 'https://open-meteo.com',
          status: 'ACTIVE',
          isVerified: true
        });
      }

      if (wind != null && wind > 45.0) {
        alerts.push({
          id: `om_wind_${Date.now()}`,
          title: 'Strong Wind Advisory',
          description: `High wind speeds detected (${wind} km/h). Secure loose outdoor objects.`,
          category: 'WEATHER',
          severity: wind > 65 ? 'HIGH' : 'MODERATE',
          latitude: lat,
          longitude: lon,
          issuedAt: new Date().toISOString(),
          sourceName: 'Open-Meteo',
          sourceUrl: 'https://open-meteo.com',
          status: 'ACTIVE',
          isVerified: true
        });
      }

      if (temp != null && (temp > 42.0 || temp < 2.0)) {
        alerts.push({
          id: `om_temp_${Date.now()}`,
          title: temp > 42 ? 'Extreme Heat Advisory' : 'Extreme Cold Advisory',
          description: `Temperature has reached an extreme level (${temp}°C). Take necessary health precautions.`,
          category: 'WEATHER',
          severity: 'HIGH',
          latitude: lat,
          longitude: lon,
          issuedAt: new Date().toISOString(),
          sourceName: 'Open-Meteo',
          sourceUrl: 'https://open-meteo.com',
          status: 'ACTIVE',
          isVerified: true
        });
      }

      return alerts;
    } catch (e: any) {
      console.warn('[CityAlertsService Open-Meteo Error]:', e.message);
      return [];
    }
  }

  private async fetchAqiAlerts(lat: number, lon: number): Promise<CityAlert[]> {
    try {
      const aqiRes: any = await railwayProviderService.getAqi(lat.toString(), lon.toString());
      if (aqiRes && aqiRes.status === 'OK' && typeof aqiRes.aqi === 'number') {
        const aqi = aqiRes.aqi;
        if (aqi > 100) {
          let severity: AlertSeverity = 'MODERATE';
          if (aqi > 300) severity = 'CRITICAL';
          else if (aqi > 200) severity = 'HIGH';
          else if (aqi > 150) severity = 'MODERATE';

          return [{
            id: `aqi_alert_${Date.now()}`,
            title: `Air Quality Alert: ${aqiRes.category || 'Unhealthy'}`,
            description: `Air Quality Index is ${aqi} (${aqiRes.category}). Dominant pollutant: ${aqiRes.dominantPollutant || 'PM2.5'}. ${aqi > 200 ? 'Sensitive groups and general public should avoid outdoor exposure.' : 'Sensitive individuals should limit prolonged outdoor exertion.'}`,
            category: 'AIR_QUALITY',
            severity,
            city: aqiRes.stationName || undefined,
            latitude: lat,
            longitude: lon,
            issuedAt: aqiRes.timeString ? new Date(aqiRes.timeString).toISOString() : new Date().toISOString(),
            sourceName: 'AQICN',
            sourceUrl: 'https://aqicn.org',
            status: 'ACTIVE',
            isVerified: true
          }];
        }
      }
      return [];
    } catch (e: any) {
      console.warn('[CityAlertsService AQICN Error]:', e.message);
      return [];
    }
  }

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

  async getAlerts(
    lat?: number,
    lon?: number,
    city?: string,
    district?: string,
    category?: string
  ): Promise<CityAlertsResponse> {
    const targetLat = lat ?? 25.6022;
    const targetLon = lon ?? 85.1376;

    console.log(`[CITY_ALERTS_REQUEST]: lat=${targetLat}, lon=${targetLon}, city=${city || 'none'}, district=${district || 'none'}, category=${category || 'all'}`);

    const [gdacsAlerts, meteoAlerts, aqiAlerts] = await Promise.all([
      this.fetchGdacsAlerts(targetLat, targetLon),
      this.fetchOpenMeteoAlerts(targetLat, targetLon),
      this.fetchAqiAlerts(targetLat, targetLon)
    ]);

    let allAlerts = [...gdacsAlerts, ...meteoAlerts, ...aqiAlerts];

    if (category) {
      const upperCat = category.toUpperCase();
      allAlerts = allAlerts.filter(a => a.category.toUpperCase() === upperCat);
    }

    return {
      status: 'OK',
      location: {
        city: city || 'Madhuban / Patna',
        district: district || 'Bihar',
        latitude: targetLat,
        longitude: targetLon
      },
      alerts: allAlerts
    };
  }
}

export const cityAlertsService = new CityAlertsService();
