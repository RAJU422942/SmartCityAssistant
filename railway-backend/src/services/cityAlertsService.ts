import { CityAlert, AlertCategory, AlertSeverity, AlertStatus, CityAlertsResponse } from '../models/cityAlertModels';
import { railwayProviderService } from './railwayProviderService';

export class CityAlertsService {
  private async fetchGdacsAlerts(lat: number, lon: number): Promise<{ alerts: CityAlert[], rawCount: number, withCoords: number, withinRadius: number }> {
    try {
      const res = await fetch('https://www.gdacs.org/xml/rss.xml', { signal: AbortSignal.timeout(6000) });
      if (!res.ok) {
        console.warn('[GDACS]: Status not OK:', res.status);
        return { alerts: [], rawCount: 0, withCoords: 0, withinRadius: 0 };
      }
      const xmlText = await res.text();

      const alerts: CityAlert[] = [];
      const itemRegex = /<item>([\s\S]*?)<\/item>/g;
      let match;
      let rawCount = 0;
      let withCoords = 0;
      let withinRadius = 0;

      while ((match = itemRegex.exec(xmlText)) !== null) {
        rawCount++;
        const itemContent = match[1];
        const getTag = (tag: string) => {
          const tMatch = new RegExp(`<${tag}[^>]*>([\\s\\S]*?)<\/${tag}>`, 'i').exec(itemContent);
          return tMatch ? tMatch[1].replace(/<!\[CDATA\[([\s\S]*?)\]\]>/g, '$1').trim() : '';
        };

        const title = getTag('title');
        const description = getTag('description');
        const link = getTag('link');
        const pubDate = getTag('pubDate');

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

        const hasCoords = !isNaN(geomLat) && !isNaN(geomLon) && (geomLat !== 0 || geomLon !== 0);
        if (hasCoords) {
          withCoords++;
        }

        const dist = hasCoords ? this.haversineKm(lat, lon, geomLat, geomLon) : 99999;
        const DEFAULT_DISASTER_RADIUS_KM = 400;
        const lowerTitle = title.toLowerCase();
        const isMajorStorm = lowerTitle.includes('cyclone') || lowerTitle.includes('storm') || lowerTitle.includes('typhoon') || lowerTitle.includes('hurricane');
        const maxRadius = isMajorStorm ? 600 : DEFAULT_DISASTER_RADIUS_KM;

        const included = hasCoords && (dist <= maxRadius);
        if (included) {
          withinRadius++;
        }

        console.log(`[GDACS Event Detail]: event="${title.substring(0, 35)}...", eventLat=${geomLat}, eventLon=${geomLon}, targetLat=${lat}, targetLon=${lon}, distance=${Math.round(dist)}km, maxRadius=${maxRadius}km, included=${included}`);

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
            latitude: geomLat,
            longitude: geomLon,
            issuedAt: pubDate ? new Date(pubDate).toISOString() : new Date().toISOString(),
            sourceName: 'GDACS',
            sourceUrl: link || 'https://www.gdacs.org',
            status: 'ACTIVE',
            isVerified: true
          });
        }
      }

      console.log(`[GDACS]: status=OK, rawCount=${rawCount}, withCoords=${withCoords}, withinRadius=${withinRadius}, finalAlertCount=${alerts.length}`);
      return { alerts, rawCount, withCoords, withinRadius };
    } catch (e: any) {
      console.warn('[GDACS Error]:', e.message);
      return { alerts: [], rawCount: 0, withCoords: 0, withinRadius: 0 };
    }
  }

  private async fetchOpenMeteoAlerts(lat: number, lon: number): Promise<{ alerts: CityAlert[], requestSuccessful: boolean, weatherDataReceived: boolean }> {
    try {
      const url = `https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lon}&current=temperature_2m,relative_humidity_2m,precipitation,weather_code,wind_speed_10m`;
      const res = await fetch(url, { signal: AbortSignal.timeout(5000) });
      if (!res.ok) {
        console.log(`[OPEN_METEO]: lat=${lat}, lon=${lon}, requestSuccessful=false, status=${res.status}`);
        return { alerts: [], requestSuccessful: false, weatherDataReceived: false };
      }
      const json: any = await res.json();
      const current = json.current;
      if (!current) {
        console.log(`[OPEN_METEO]: lat=${lat}, lon=${lon}, requestSuccessful=true, weatherDataReceived=false`);
        return { alerts: [], requestSuccessful: true, weatherDataReceived: false };
      }

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

      console.log(`[OPEN_METEO]: lat=${lat}, lon=${lon}, requestSuccessful=true, weatherDataReceived=true, weatherAlertCount=${alerts.length}`);
      return { alerts, requestSuccessful: true, weatherDataReceived: true };
    } catch (e: any) {
      console.warn('[Open-Meteo Error]:', e.message);
      console.log(`[OPEN_METEO]: lat=${lat}, lon=${lon}, requestSuccessful=false, weatherDataReceived=false`);
      return { alerts: [], requestSuccessful: false, weatherDataReceived: false };
    }
  }

  private async fetchAqiAlerts(lat: number, lon: number): Promise<{ alerts: CityAlert[], providerStatus: string, aqi: number | null }> {
    try {
      const aqiRes: any = await railwayProviderService.getAqi(lat.toString(), lon.toString());
      const providerStatus = aqiRes?.status || 'UNKNOWN';
      const aqi = typeof aqiRes?.aqi === 'number' ? aqiRes.aqi : null;

      console.log(`[AQICN]: lat=${lat}, lon=${lon}, providerStatus=${providerStatus}, aqi=${aqi}`);

      if (aqiRes && aqiRes.status === 'OK' && typeof aqiRes.aqi === 'number') {
        const val = aqiRes.aqi;
        if (val > 100) {
          let severity: AlertSeverity = 'MODERATE';
          if (val > 300) severity = 'CRITICAL';
          else if (val > 200) severity = 'HIGH';
          else if (val > 150) severity = 'MODERATE';

          const alertItem: CityAlert = {
            id: `aqi_alert_${Date.now()}`,
            title: `Air Quality Alert: ${aqiRes.category || 'Unhealthy'}`,
            description: `Air Quality Index is ${val} (${aqiRes.category}). Dominant pollutant: ${aqiRes.dominantPollutant || 'PM2.5'}. ${val > 200 ? 'Sensitive groups and general public should avoid outdoor exposure.' : 'Sensitive individuals should limit prolonged outdoor exertion.'}`,
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
          };
          return { alerts: [alertItem], providerStatus, aqi: val };
        }
      }
      return { alerts: [], providerStatus, aqi };
    } catch (e: any) {
      console.warn('[AQICN Error]:', e.message);
      return { alerts: [], providerStatus: 'ERROR', aqi: null };
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

    console.log(`[CityAlerts TEST]: location=${city || 'Madhuban / Patna'}, lat=${targetLat}, lon=${targetLon}, category=${category || 'ALL'}`);

    const [gdacsResult, meteoResult, aqiResult] = await Promise.all([
      this.fetchGdacsAlerts(targetLat, targetLon),
      this.fetchOpenMeteoAlerts(targetLat, targetLon),
      this.fetchAqiAlerts(targetLat, targetLon)
    ]);

    let allAlerts = [...gdacsResult.alerts, ...meteoResult.alerts, ...aqiResult.alerts];

    console.log(`[CityAlerts Summary]: totalAlerts=${allAlerts.length} (GDACS: ${gdacsResult.alerts.length}, Open-Meteo: ${meteoResult.alerts.length}, AQICN: ${aqiResult.alerts.length})`);

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
