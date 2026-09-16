import { find as findTimeZone } from 'geo-tz';
import SunCalc from 'suncalc';

interface StationInfo {
  code: string;
  name: string;
  city: string;
  lat: number;
  lon: number;
}

const CPCB_MAX_DISTANCE_KM = 100;

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

function mapWeatherApiConditionToWeather(code: number, text: string): { condition: string; weatherCode: number } {
  const t = (text || '').toLowerCase();
  if ([1087, 1273, 1276, 1279, 1282].includes(code) || t.includes('thunder')) {
    return { condition: 'Thunderstorm', weatherCode: 95 };
  }
  if ([1114, 1117, 1210, 1213, 1216, 1219, 1222, 1225, 1255, 1258].includes(code) || t.includes('snow') || t.includes('sleet')) {
    return { condition: 'Snow', weatherCode: 71 };
  }
  if ([1189, 1192, 1195, 1243, 1246].includes(code) || t.includes('heavy rain') || t.includes('torrential')) {
    return { condition: 'Rainy', weatherCode: 63 };
  }
  if ([1240, 1241, 1242].includes(code) || t.includes('shower')) {
    return { condition: 'Showers', weatherCode: 80 };
  }
  if ([1063, 1180, 1183, 1186, 1198, 1201].includes(code) || t.includes('rain') || t.includes('drizzle')) {
    return { condition: 'Rainy', weatherCode: 61 };
  }
  if ([1030, 1135, 1147].includes(code) || t.includes('fog') || t.includes('mist')) {
    return { condition: 'Foggy', weatherCode: 45 };
  }
  if ([1006, 1009].includes(code) || t.includes('overcast') || t.includes('cloud')) {
    return { condition: 'Overcast', weatherCode: 3 };
  }
  if ([1003].includes(code) || t.includes('partly cloudy')) {
    return { condition: 'Partly Cloudy', weatherCode: 2 };
  }
  if ([1000].includes(code) || t.includes('sunny') || t.includes('clear')) {
    return { condition: 'Mainly Clear', weatherCode: 1 };
  }
  return { condition: text || 'Partly Cloudy', weatherCode: 2 };
}

function calculateSunriseSunset(lat: number, lon: number, date: Date = new Date()): { sunrise: string; sunset: string } {
  try {
    const timeZones = findTimeZone(lat, lon);
    const timeZone = timeZones[0] || 'UTC';

    const times = SunCalc.getTimes(date, lat, lon);
    const sunriseUtc = times.sunrise;
    const sunsetUtc = times.sunset;

    if (!sunriseUtc || !sunsetUtc) {
      return { sunrise: "05:32 AM", sunset: "05:49 PM" };
    }

    const formatter = new Intl.DateTimeFormat('en-US', {
      timeZone,
      hour: '2-digit',
      minute: '2-digit',
      hour12: true
    });

    const sunrise = formatter.format(sunriseUtc);
    const sunset = formatter.format(sunsetUtc);

    console.log(`[WEATHER SUN] date=${date.toISOString().split('T')[0]} timezone=${timeZone} sunriseUtc=${sunriseUtc.toISOString()} sunriseLocal=${sunrise} sunsetUtc=${sunsetUtc.toISOString()} sunsetLocal=${sunset}`);

    return { sunrise, sunset };
  } catch (e: any) {
    console.log(`[WEATHER SUN ERROR] error=${e.message}`);
    return { sunrise: "05:32 AM", sunset: "05:49 PM" };
  }
}

export class RailwayProviderService {
  private weatherCache = new Map<string, { response: any; timestamp: number }>();
  private weatherInFlight = new Map<string, Promise<any>>();
  private WEATHER_TTL_MS = 15 * 60 * 1000; // 15 minutes

  private getNormKey(lat: string | number, lon: string | number): string {
    const lLat = typeof lat === 'string' ? parseFloat(lat) : lat;
    const lLon = typeof lon === 'string' ? parseFloat(lon) : lon;
    return `${lLat.toFixed(4)}_${lLon.toFixed(4)}`;
  }

  private getApiKey(): string {
    return process.env.RAILWAY_API_KEY || '';
  }

  private getMapsKey(): string {
    return process.env.GOOGLE_MAPS_API_KEY || process.env.GOOGLE_PLACES_API_KEY || '';
  }

  private async fetchWithTimeout(url: string, options: any = {}, timeoutMs = 12000): Promise<Response> {
    const controller = new AbortController();
    const id = setTimeout(() => controller.abort(), timeoutMs);
    try {
      const response = await fetch(url, {
        ...options,
        signal: controller.signal
      });
      clearTimeout(id);
      return response;
    } catch (error) {
      clearTimeout(id);
      throw error;
    }
  }

  async getLiveStatus(trainNumber: string, date: string): Promise<any> {
    const apiKey = this.getApiKey();
    if (!apiKey) {
      return {
        trainNumber,
        trainName: "Rajdhani Express (Demo)",
        currentStation: "New Delhi (NDLS)",
        nextStation: "Kanpur Central (CNB)",
        delayMinutes: 12,
        runningState: "RUNNING",
        lastUpdated: new Date().toLocaleTimeString(),
        status: "OK"
      };
    }
    try {
      const url = `https://irctc1.p.rapidapi.com/api/v1/liveTrainStatus?trainNo=${trainNumber}&startDay=1`;
      const res = await this.fetchWithTimeout(url, {
        headers: {
          'X-RapidAPI-Key': apiKey,
          'X-RapidAPI-Host': 'irctc1.p.rapidapi.com'
        }
      });
      if (!res.ok) {
        return { status: 'ERROR', message: `API error: ${res.status}` };
      }
      const data: any = await res.json();
      return data;
    } catch (e: any) {
      return { status: 'ERROR', message: e.message };
    }
  }

  async getPnrStatus(pnr: string): Promise<any> {
    const apiKey = this.getApiKey();
    if (!apiKey) {
      return {
        pnrNumber: pnr,
        trainNumber: "12952",
        trainName: "Mumbai Rajdhani",
        journeyDate: "2026-03-25",
        from: "NDLS",
        to: "BCT",
        bookingStatus: "CNF / B1 / 23",
        currentStatus: "CNF / B1 / 23",
        coachBerth: "B1 - 23 (Middle)",
        chartingStatus: "CHART PREPARED",
        status: "OK"
      };
    }
    try {
      const url = `https://irctc1.p.rapidapi.com/api/v3/pnrStatus?pnr=${pnr}`;
      const res = await this.fetchWithTimeout(url, {
        headers: {
          'X-RapidAPI-Key': apiKey,
          'X-RapidAPI-Host': 'irctc1.p.rapidapi.com'
        }
      });
      if (!res.ok) {
        return { status: 'ERROR', message: `API error: ${res.status}` };
      }
      const data: any = await res.json();
      return data;
    } catch (e: any) {
      return { status: 'ERROR', message: e.message };
    }
  }

  async getSeatAvailability(train: string, from: string, to: string, date: string, trainClass: string, quota: string): Promise<any> {
    const apiKey = this.getApiKey();
    if (!apiKey) {
      return {
        trainNumber: train,
        status: "OK",
        availabilityText: "AVAILABLE - 42 Seats (3A)",
        fare: "₹1,850"
      };
    }
    try {
      const url = `https://irctc1.p.rapidapi.com/api/v3/checkSeatAvailability?trainNo=${train}&fromStation=${from}&toStation=${to}&date=${date}&class=${trainClass}&quota=${quota}`;
      const res = await this.fetchWithTimeout(url, {
        headers: {
          'X-RapidAPI-Key': apiKey,
          'X-RapidAPI-Host': 'irctc1.p.rapidapi.com'
        }
      });
      if (!res.ok) {
        return { status: 'ERROR', message: `API error: ${res.status}` };
      }
      const data: any = await res.json();
      return data;
    } catch (e: any) {
      return { status: 'ERROR', message: e.message };
    }
  }

  async getTrainInfo(trainNumber: string): Promise<any> {
    return {
      trainNumber,
      trainName: "Smart City Express",
      source: "NDLS",
      destination: "CSMT",
      departureTime: "16:30",
      arrivalTime: "08:15",
      duration: "15h 45m",
      runningDays: ["MON", "WED", "FRI"],
      status: "OK",
      classes: ["1A", "2A", "3A", "SL"]
    };
  }

  async getTrainsBetween(from: string, to: string, date: string): Promise<any[]> {
    return [
      {
        trainNumber: "12952",
        trainName: "Rajdhani Express",
        source: from,
        destination: to,
        departureTime: "16:30",
        arrivalTime: "08:15",
        duration: "15h 45m",
        runningDays: ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"],
        status: "OK",
        classes: ["1A", "2A", "3A"]
      },
      {
        trainNumber: "12954",
        trainName: "August Kranti Rajdhani",
        source: from,
        destination: to,
        departureTime: "17:15",
        arrivalTime: "09:40",
        duration: "16h 25m",
        runningDays: ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"],
        status: "OK",
        classes: ["2A", "3A", "CC"]
      }
    ];
  }

  async searchStations(query: string): Promise<any[]> {
    const q = (query || '').toLowerCase();
    const stations = [
      { code: "NDLS", name: "New Delhi", city: "New Delhi", lat: 28.6139, lon: 77.2090 },
      { code: "CSMT", name: "Mumbai CSMT", city: "Mumbai", lat: 18.9400, lon: 72.8350 },
      { code: "PNBE", name: "Patna Junction", city: "Patna", lat: 25.6022, lon: 85.1376 },
      { code: "BSB", name: "Varanasi Junction", city: "Varanasi", lat: 25.3176, lon: 82.9739 },
      { code: "HWH", name: "Howrah Junction", city: "Kolkata", lat: 22.5833, lon: 88.3417 },
      { code: "MS", name: "Chennai Egmore", city: "Chennai", lat: 13.0827, lon: 80.2707 }
    ];
    if (!q) return stations;
    return stations.filter(s => s.name.toLowerCase().includes(q) || s.code.toLowerCase().includes(q) || s.city.toLowerCase().includes(q));
  }

  async searchTrainsByName(query: string): Promise<any[]> {
    const q = (query || '').toLowerCase();
    const trains = [
      { trainNumber: "12952", trainName: "Mumbai Rajdhani", source: "NDLS", destination: "BCT", departureTime: "16:30", arrivalTime: "08:15", duration: "15h 45m", runningDays: ["DAILY"], status: "OK", classes: ["1A", "2A", "3A"] },
      { trainNumber: "12302", trainName: "Howrah Rajdhani", source: "NDLS", destination: "HWH", departureTime: "17:00", arrivalTime: "09:55", duration: "16h 55m", runningDays: ["DAILY"], status: "OK", classes: ["1A", "2A", "3A"] }
    ];
    if (!q) return trains;
    return trains.filter(t => t.trainName.toLowerCase().includes(q) || t.trainNumber.includes(q));
  }

  async getAqi(lat: string, lon: string): Promise<any> {
    const requestId = Math.random().toString(36).substring(7);
    const userLat = parseFloat(lat);
    const userLon = parseFloat(lon);

    console.log(`[AQI REQUEST] requestId=${requestId} lat=${lat} lon=${lon}`);

    try {
      const cpcbApiKey = process.env.CPCB_API_KEY || '579b464db66ec23bdd000001cdd3946e44ce4a7f0970f19efead690d';
      const cpcbUrl = `https://api.data.gov.in/resource/3b01bcb8-0b14-4abf-b1f2-8ec91b205104?api-key=${cpcbApiKey}&format=json&limit=1000`;

      console.log(`[CPCB FETCH] url=${cpcbUrl.replace(cpcbApiKey, 'MASKED')}`);
      const res = await this.fetchWithTimeout(cpcbUrl, {}, 10000);

      if (!res.ok) {
        console.log(`[CPCB FETCH] status=${res.status} message=HTTP error`);
        throw new Error(`CPCB API error: ${res.status}`);
      }

      const json: any = await res.json();
      const records = json?.records || json?.data || json?.results || (Array.isArray(json) ? json : []);
      console.log(`[CPCB FETCH] httpStatus=${res.status} recordCount=${records.length}`);

      let validCount = 0;
      let closestStation: any = null;
      let minDist = 999999;

      for (const rec of records) {
        const rLat = parseFloat(rec.latitude ?? rec.lat ?? rec.lat_value ?? rec.y ?? '');
        const rLon = parseFloat(rec.longitude ?? rec.lng ?? rec.lon ?? rec.long ?? rec.x ?? '');
        const aqiVal = parseInt(rec.aqi ?? rec.pollutant_avg ?? rec.pollutant_max ?? '0', 10);

        if (!isNaN(rLat) && !isNaN(rLon) && !isNaN(aqiVal) && aqiVal > 0) {
          validCount++;
          const dist = haversineKm(userLat, userLon, rLat, rLon);
          if (dist < minDist) {
            minDist = dist;
            closestStation = { ...rec, parsedLat: rLat, parsedLon: rLon, parsedAqi: aqiVal, distance: dist };
          }
        }
      }

      console.log(`[CPCB FETCH] validCoordinateCount=${validCount}`);

      if (closestStation && minDist <= CPCB_MAX_DISTANCE_KM) {
        const lastUpdate = closestStation.last_update || closestStation.updated_date || closestStation.time || 'Just now';
        console.log(`[CPCB NEAREST] station=${closestStation.station || closestStation.location || closestStation.city || 'CPCB Station'} distanceKm=${minDist.toFixed(1)} AQI=${closestStation.parsedAqi} lastUpdate=${lastUpdate}`);
        console.log(`[AQI SOURCE] source=CPCB AQI=${closestStation.parsedAqi} category=${closestStation.parsedAqi <= 50 ? 'Good' : closestStation.parsedAqi <= 100 ? 'Satisfactory' : closestStation.parsedAqi <= 200 ? 'Moderate' : closestStation.parsedAqi <= 300 ? 'Poor' : 'Very Poor'} station=${closestStation.station || closestStation.location || 'CPCB Station'} distance=${minDist.toFixed(1)}km`);

        return {
          status: 'OK',
          aqi: closestStation.parsedAqi,
          category: closestStation.parsedAqi <= 50 ? 'Good' : closestStation.parsedAqi <= 100 ? 'Satisfactory' : closestStation.parsedAqi <= 200 ? 'Moderate' : closestStation.parsedAqi <= 300 ? 'Poor' : 'Very Poor',
          dominantPollutant: closestStation.pollutant_id || closestStation.dominant_pollutant || 'PM2.5',
          stationName: closestStation.station || closestStation.location || closestStation.city || 'CPCB Monitoring Station',
          stationLatitude: closestStation.parsedLat,
          stationLongitude: closestStation.parsedLon,
          distanceKm: parseFloat(minDist.toFixed(1)),
          pm25: parseFloat(closestStation.pm25 || '45.0'),
          pm10: parseFloat(closestStation.pm10 || '85.0'),
          co: parseFloat(closestStation.co || '1.2'),
          no2: parseFloat(closestStation.no2 || '25.4'),
          o3: parseFloat(closestStation.o3 || '30.1'),
          so2: parseFloat(closestStation.so2 || '10.2'),
          nh3: 15.0,
          timeString: lastUpdate,
          message: null,
          source: 'CPCB',
          sourceLabel: 'CPCB • Monitoring Station'
        };
      } else {
        const reason = minDist > CPCB_MAX_DISTANCE_KM ? `Nearest station distance ${minDist.toFixed(1)}km exceeds ${CPCB_MAX_DISTANCE_KM}km limit` : `No valid CPCB records found`;
        console.log(`[CPCB NEAREST] no station within ${CPCB_MAX_DISTANCE_KM}km (minDist=${minDist.toFixed(1)}km)`);
        console.log(`[AQI FALLBACK] reason=${reason}`);
        return await this.fetchOpenMeteoAqi(userLat, userLon, requestId);
      }
    } catch (cpcbErr: any) {
      console.log(`[AQI FALLBACK] reason=${cpcbErr.message}`);
      return await this.fetchOpenMeteoAqi(userLat, userLon, requestId);
    }
  }

  private async fetchOpenMeteoAqi(userLat: number, userLon: number, requestId: string): Promise<any> {
    try {
      const omUrl = `https://air-quality-api.open-meteo.com/v1/air-quality?latitude=${userLat}&longitude=${userLon}&current=pm10,pm2_5,carbon_monoxide,nitrogen_dioxide,sulphur_dioxide,ozone&timezone=auto`;
      const res = await this.fetchWithTimeout(omUrl);
      if (!res.ok) {
        throw new Error(`Open-Meteo AQI failed with status ${res.status}`);
      }
      const data: any = await res.json();
      const current = data.current;
      const pm25 = current?.pm2_5 || 45.0;
      const pm10 = current?.pm10 || 85.0;
      const aqiVal = Math.round((pm25 / 60.0) * 50);

      console.log(`[AQI SOURCE] source=OPEN_METEO AQI=${aqiVal} category=${aqiVal <= 50 ? 'Good' : aqiVal <= 100 ? 'Satisfactory' : aqiVal <= 200 ? 'Moderate' : aqiVal <= 300 ? 'Poor' : 'Very Poor'} station=Open-Meteo CAMS Model distance=null`);

      return {
        status: 'OK',
        aqi: aqiVal,
        category: aqiVal <= 50 ? 'Good' : aqiVal <= 100 ? 'Satisfactory' : aqiVal <= 200 ? 'Moderate' : aqiVal <= 300 ? 'Poor' : 'Very Poor',
        dominantPollutant: 'PM2.5',
        stationName: 'Open-Meteo CAMS Model',
        stationLatitude: userLat,
        stationLongitude: userLon,
        distanceKm: null,
        pm25: pm25,
        pm10: pm10,
        co: current?.carbon_monoxide || 1.2,
        no2: current?.nitrogen_dioxide || 25.4,
        o3: current?.ozone || 30.1,
        so2: current?.sulphur_dioxide || 10.2,
        nh3: 15.0,
        timeString: 'Just now',
        message: null,
        source: 'OPEN_METEO',
        sourceLabel: 'Open-Meteo • CAMS'
      };
    } catch (omError: any) {
      console.log(`[AQI FINAL] status=ERROR message=${omError.message}`);
      return {
        status: 'ERROR',
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
        message: 'Air quality data unavailable from all providers',
        source: 'OPEN_METEO',
        sourceLabel: 'Open-Meteo • CAMS'
      };
    }
  }

  async getWeather(lat: string, lon: string): Promise<any> {
    const requestId = Math.random().toString(36).substring(7);
    const userLat = parseFloat(lat);
    const userLon = parseFloat(lon);
    const normLat = userLat.toFixed(4);
    const normLon = userLon.toFixed(4);
    const normKey = `${normLat}_${normLon}`;

    console.log(`[WEATHER REQUEST] requestId=${requestId} lat=${lat} lon=${lon} provider=WeatherAPI`);

    // 1. Check cache
    const cached = this.weatherCache.get(normKey);
    if (cached) {
      const age = Date.now() - cached.timestamp;
      if (age < this.WEATHER_TTL_MS) {
        console.log(`[WEATHER CACHE HIT] key=${normKey}`);
        return cached.response;
      } else {
        this.weatherCache.delete(normKey);
      }
    } else {
      console.log(`[WEATHER CACHE MISS] key=${normKey}`);
    }

    // 2. Check in-flight request deduplication
    const activePromise = this.weatherInFlight.get(normKey);
    if (activePromise) {
      console.log(`[WEATHER INFLIGHT REUSE] key=${normKey}`);
      return activePromise;
    }

    // 3. Create upstream fetch promise
    const fetchPromise = (async () => {
      try {
        const apiKey = process.env.WEATHER_API_KEY;
        if (!apiKey) {
          throw new Error('WEATHER_API_KEY environment variable is not configured');
        }

        const url = `https://api.weatherapi.com/v1/forecast.json?key=${apiKey}&q=${userLat},${userLon}&days=7&aqi=no&alerts=yes`;
        const res = await this.fetchWithTimeout(url, {}, 12000);

        console.log(`[WEATHERAPI RESPONSE] requestId=${requestId} httpStatus=${res.status}`);

        if (res.status === 429) {
          console.log(`[WEATHER UPSTREAM 429] key=${normKey}`);
          if (cached) return cached.response;
          return { status: 'ERROR', message: 'WEATHER_RATE_LIMITED' };
        }

        if (!res.ok) {
          console.log(`[WEATHER ERROR] requestId=${requestId} httpStatus=${res.status}`);
          if (cached) return cached.response;
          return { status: 'ERROR', message: `Weather provider error (${res.status})` };
        }

        const data: any = await res.json();
        const current = data?.current;
        const forecastDays = data?.forecast?.forecastday;

        if (!current || !forecastDays || !Array.isArray(forecastDays) || forecastDays.length === 0) {
          console.log(`[WEATHER ERROR] requestId=${requestId} missing forecast data from WeatherAPI`);
          if (cached) return cached.response;
          return { status: 'ERROR', message: 'Weather data unavailable.' };
        }

        const temperature = current.temp_c ?? null;
        const apparentTemperature = current.feelslike_c ?? temperature;
        const humidity = current.humidity ?? null;
        const windSpeed = current.wind_kph ?? null;
        const conditionText = current.condition?.text ?? 'Partly Cloudy';
        const conditionCode = current.condition?.code ?? 1003;
        const { condition, weatherCode } = mapWeatherApiConditionToWeather(conditionCode, conditionText);

        const sunrise = "06:30 AM";
        const sunset = "08:00 PM";

        const hourlyList: any[] = [];
        const forecastList: any[] = [];

        for (const dayObj of forecastDays) {
          const dateStr = dayObj.date; // "YYYY-MM-DD"
          const dayData = dayObj.day;
          const hours = dayObj.hour;

          const maxTemp = dayData?.maxtemp_c ?? 30;
          const minTemp = dayData?.mintemp_c ?? 20;
          const dayCondText = dayData?.condition?.text ?? 'Partly Cloudy';
          const dayCondCode = dayData?.condition?.code ?? 1003;
          const { condition: dCond, weatherCode: dCode } = mapWeatherApiConditionToWeather(dayCondCode, dayCondText);
          const precipProbMax = dayData?.daily_chance_of_rain ?? 0;

          const dateRef = new Date(dateStr);
          const dayName = forecastList.length === 0 ? 'Today' : dateRef.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' });

          forecastList.push({
            date: dateStr,
            dayName,
            weatherCode: dCode,
            condition: dCond,
            maxTemp,
            minTemp,
            precipitationProbabilityMax: precipProbMax
          });

          if (Array.isArray(hours)) {
            for (const h of hours) {
              const timeIso = h.time; // e.g. "2026-03-25 00:00"
              const hTemp = h.temp_c;
              const hHum = h.humidity;
              const hWind = h.wind_kph;
              const hDir = h.wind_degree;
              const hRainProb = h.chance_of_rain ?? 0;
              const hCondText = h.condition?.text ?? 'Partly Cloudy';
              const hCondCode = h.condition?.code ?? 1003;
              const { weatherCode: hCode } = mapWeatherApiConditionToWeather(hCondCode, hCondText);

              let hourFormatted = timeIso;
              try {
                const parts = timeIso.split(' ');
                if (parts.length === 2) {
                  const timePart = parts[1]; // "00:00"
                  const hourInt = parseInt(timePart.split(':')[0], 10);
                  const h12 = hourInt % 12 === 0 ? 12 : hourInt % 12;
                  const ampm = hourInt >= 12 ? 'PM' : 'AM';
                  hourFormatted = `${h12} ${ampm}`;
                }
              } catch (_) {}

              hourlyList.push({
                time: timeIso,
                hourFormatted,
                temperature: hTemp,
                humidity: hHum,
                precipitationProbability: hRainProb,
                weatherCode: hCode,
                windSpeed: hWind,
                windDirection: hDir
              });
            }
          }
        }

        const result = {
          status: 'OK',
          temperature,
          apparentTemperature,
          humidity,
          windSpeed,
          condition,
          weatherCode,
          sunrise,
          sunset,
          source: 'WeatherAPI',
          forecast: forecastList,
          hourly: hourlyList
        };

        console.log(`[WEATHERAPI PARSER] requestId=${requestId} hourlyCount=${hourlyList.length} dailyCount=${forecastList.length}`);
        console.log(`[WEATHER FINAL] requestId=${requestId} source=WeatherAPI`);

        this.weatherCache.set(normKey, { response: result, timestamp: Date.now() });
        return result;
      } catch (err: any) {
        console.log(`[WEATHER FETCH ERROR] key=${normKey} error=${err.message}`);
        if (cached) return cached.response;
        return { status: 'ERROR', message: err.message || 'Weather unavailable' };
      } finally {
        this.weatherInFlight.delete(normKey);
      }
    })();

    this.weatherInFlight.set(normKey, fetchPromise);
    return fetchPromise;
  }
}

export const railwayProviderService = new RailwayProviderService();
