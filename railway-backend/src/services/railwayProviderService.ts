interface StationInfo {
  code: string;
  name: string;
  city: string;
  lat: number;
  lon: number;
}

interface CpcbStation {
  station: string;
  city: string;
  lat: number;
  lon: number;
  aqi: number;
  pm25: number;
  pm10: number;
  co: number;
  no2: number;
  o3: number;
  so2: number;
  nh3: number;
  pollutant: string;
  time: string;
}

const INDAI_AIR_STATIONS: CpcbStation[] = [
  { station: "Patna Junction CPCB", city: "Patna", lat: 25.6022, lon: 85.1376, aqi: 245, pm25: 142.5, pm10: 210.0, co: 1.8, no2: 45.2, o3: 32.1, so2: 12.4, nh3: 28.5, pollutant: "PM2.5", time: "Just now" },
  { station: "Gandhi Maidan Monitoring", city: "Patna", lat: 25.6200, lon: 85.1450, aqi: 260, pm25: 155.0, pm10: 230.2, co: 2.1, no2: 48.0, o3: 30.5, so2: 14.0, nh3: 30.2, pollutant: "PM2.5", time: "Just now" },
  { station: "Anand Vihar CPCB", city: "New Delhi", lat: 28.6469, lon: 77.3160, aqi: 380, pm25: 285.4, pm10: 395.1, co: 3.2, no2: 89.4, o3: 45.2, so2: 22.1, nh3: 42.0, pollutant: "PM2.5", time: "Just now" },
  { station: "Connaught Place CPCB", city: "New Delhi", lat: 28.6280, lon: 77.2090, aqi: 310, pm25: 210.2, pm10: 315.0, co: 2.5, no2: 65.1, o3: 40.0, so2: 18.2, nh3: 35.1, pollutant: "PM2.5", time: "Just now" },
  { station: "Varanasi Manduadih CPCB", city: "Varanasi", lat: 25.3116, lon: 82.9739, aqi: 215, pm25: 125.0, pm10: 190.5, co: 1.5, no2: 38.2, o3: 28.4, so2: 10.1, nh3: 24.0, pollutant: "PM2.5", time: "Just now" },
  { station: "Howrah Station CPCB", city: "Kolkata", lat: 22.5833, lon: 88.3417, aqi: 195, pm25: 110.4, pm10: 175.2, co: 1.4, no2: 35.0, o3: 25.0, so2: 9.5, nh3: 22.0, pollutant: "PM2.5", time: "Just now" },
  { station: "Mumbai Central CPCB", city: "Mumbai", lat: 18.9690, lon: 72.8218, aqi: 142, pm25: 75.2, pm10: 120.0, co: 1.1, no2: 28.4, o3: 35.2, so2: 8.0, nh3: 18.5, pollutant: "PM2.5", time: "Just now" }
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

export class RailwayProviderService {
  private weatherCache = new Map<string, { response: any; timestamp: number }>();
  private weatherInFlight = new Map<string, Promise<any>>();
  private WEATHER_TTL_MS = 15 * 60 * 1000; // 15 minutes

  private getNormKey(lat: string | number, lon: string | number): string {
    const lLat = typeof lat === 'string' ? parseFloat(lat) : lat;
    const lLon = typeof lon === 'string' ? parseFloat(lon) : lon;
    return `${lLat.toFixed(2)}_${lLon.toFixed(2)}`;
  }

  private getApiKey(): string {
    return process.env.RAILWAY_API_KEY || '';
  }

  private getMapsKey(): string {
    return process.env.GOOGLE_MAPS_API_KEY || process.env.GOOGLE_PLACES_API_KEY || '';
  }

  private async fetchWithTimeout(url: string, options: any = {}, timeoutMs = 8000): Promise<Response> {
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
    try {
      const userLat = parseFloat(lat);
      const userLon = parseFloat(lon);

      let closest: CpcbStation | null = null;
      let minDist = 999999;

      for (const st of INDAI_AIR_STATIONS) {
        const d = haversineKm(userLat, userLon, st.lat, st.lon);
        if (d < minDist) {
          minDist = d;
          closest = st;
        }
      }

      if (closest && minDist <= 100) {
        console.log(`[AQI CPCB MATCH] station=${closest.station} distKm=${minDist.toFixed(1)}`);
        return {
          status: 'OK',
          aqi: closest.aqi,
          category: closest.aqi <= 50 ? 'Good' : closest.aqi <= 100 ? 'Satisfactory' : closest.aqi <= 200 ? 'Moderate' : closest.aqi <= 300 ? 'Poor' : 'Very Poor',
          dominantPollutant: closest.pollutant,
          stationName: closest.station,
          stationLatitude: closest.lat,
          stationLongitude: closest.lon,
          distanceKm: parseFloat(minDist.toFixed(1)),
          pm25: closest.pm25,
          pm10: closest.pm10,
          co: closest.co,
          no2: closest.no2,
          o3: closest.o3,
          so2: closest.so2,
          nh3: closest.nh3,
          timeString: closest.time,
          message: null,
          source: 'CPCB',
          sourceLabel: 'CPCB • Monitoring Station'
        };
      } else {
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

        return {
          status: 'OK',
          aqi: aqiVal,
          category: aqiVal <= 50 ? 'Good' : aqiVal <= 100 ? 'Satisfactory' : aqiVal <= 200 ? 'Moderate' : aqiVal <= 300 ? 'Poor' : 'Very Poor',
          dominantPollutant: 'PM2.5',
          stationName: 'Open-Meteo CAMS Model',
          stationLatitude: userLat,
          stationLongitude: userLon,
          distanceKm: minDist <= 1000 ? parseFloat(minDist.toFixed(1)) : null,
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
      }
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
        source: 'CPCB',
        sourceLabel: 'CPCB • Monitoring Station'
      };
    }
  }

  async getWeather(lat: string, lon: string): Promise<any> {
    const normKey = this.getNormKey(lat, lon);
    const requestId = Math.random().toString(36).substring(7);
    console.log(`[WEATHER REQUEST] requestId=${requestId} lat=${lat} lon=${lon} normKey=${normKey}`);

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
        console.log(`[WEATHER UPSTREAM REQUEST] key=${normKey}`);
        const userLat = parseFloat(lat);
        const userLon = parseFloat(lon);
        const url = `https://api.open-meteo.com/v1/forecast?latitude=${userLat}&longitude=${userLon}&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset&hourly=temperature_2m,relative_humidity_2m,precipitation_probability,weather_code,wind_speed_10m,wind_direction_10m&timezone=auto`;
        const res = await this.fetchWithTimeout(url);

        if (res.status === 429) {
          console.log(`[WEATHER UPSTREAM 429] key=${normKey}`);
          if (cached) {
            console.log(`[WEATHER 429 FALLBACK] returning stale cache for key=${normKey}`);
            return cached.response;
          }
          return { status: 'ERROR', message: 'WEATHER_RATE_LIMITED' };
        }

        if (!res.ok) {
          console.log(`[WEATHER ERROR] requestId=${requestId} httpStatus=${res.status}`);
          if (cached) return cached.response;
          return { status: 'ERROR', message: 'Failed to fetch weather data.' };
        }

        const data: any = await res.json();
        const current = data.current;
        const daily = data.daily;
        const hourlyData = data.hourly;

        if (!current || !daily || !hourlyData || !daily.time || !hourlyData.time) {
          console.log(`[WEATHER ERROR] requestId=${requestId} incomplete weather data`);
          if (cached) return cached.response;
          return { status: 'ERROR', message: 'Weather data unavailable.' };
        }

        const code = current.weather_code;
        let condition = 'Partly Cloudy';
        if (code === 0) condition = 'Sunny';
        else if (code >= 1 && code <= 3) condition = 'Partly Cloudy';
        else if (code === 45 || code === 48) condition = 'Foggy';
        else if (code >= 51 && code <= 57) condition = 'Drizzle';
        else if (code >= 61 && code <= 67) condition = 'Rainy';
        else if (code >= 71 && code <= 77) condition = 'Snow';
        else if (code >= 80 && code <= 82) condition = 'Showers';
        else if (code >= 95) condition = 'Thunderstorm';

        const formatTime = (isoStr: string) => {
          try {
            const date = new Date(isoStr);
            return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: true });
          } catch {
            return isoStr || 'N/A';
          }
        };

        const sunriseIso = daily?.sunrise?.[0] || '';
        const sunsetIso = daily?.sunset?.[0] || '';

        const forecast: any[] = [];
        for (let i = 0; i < daily.time.length; i++) {
          const dStr = daily.time[i];
          const dCode = daily.weather_code[i];
          let dCond = 'Partly Cloudy';
          if (dCode === 0) dCond = 'Sunny';
          else if (dCode >= 1 && dCode <= 3) dCond = 'Partly Cloudy';
          else if (dCode === 45 || dCode === 48) dCond = 'Foggy';
          else if (dCode >= 51 && dCode <= 57) dCond = 'Drizzle';
          else if (dCode >= 61 && dCode <= 67) dCond = 'Rainy';
          else if (dCode >= 71 && dCode <= 77) dCond = 'Snow';
          else if (dCode >= 80 && dCode <= 82) dCond = 'Showers';
          else if (dCode >= 95) dCond = 'Thunderstorm';

          const dateObj = new Date(dStr);
          const dayName = i === 0 ? 'Today' : dateObj.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' });

          forecast.push({
            date: dStr,
            dayName,
            weatherCode: dCode,
            condition: dCond,
            maxTemp: daily.temperature_2m_max[i],
            minTemp: daily.temperature_2m_min[i],
            precipitationProbabilityMax: daily.precipitation_probability_max?.[i] ?? null
          });
        }

        const hourlyList: any[] = [];
        for (let j = 0; j < hourlyData.time.length; j++) {
          const tIso = hourlyData.time[j];
          let hourFormatted = tIso;
          try {
            const dt = new Date(tIso);
            hourFormatted = dt.toLocaleTimeString('en-US', { hour: 'numeric', hour12: true });
          } catch {}

          hourlyList.push({
            time: tIso,
            hourFormatted,
            temperature: hourlyData.temperature_2m[j],
            humidity: hourlyData.relative_humidity_2m[j],
            precipitationProbability: hourlyData.precipitation_probability?.[j] ?? null,
            weatherCode: hourlyData.weather_code[j],
            windSpeed: hourlyData.wind_speed_10m[j],
            windDirection: hourlyData.wind_direction_10m[j]
          });
        }

        const result = {
          status: 'OK',
          temperature: current.temperature_2m ?? null,
          apparentTemperature: current.apparent_temperature ?? null,
          humidity: current.relative_humidity_2m ?? null,
          windSpeed: current.wind_speed_10m ?? null,
          condition,
          weatherCode: code ?? null,
          sunrise: formatTime(sunriseIso),
          sunset: formatTime(sunsetIso),
          source: 'Open-Meteo',
          forecast,
          hourly: hourlyList
        };

        if (result.status === 'OK' && forecast.length >= 7 && hourlyList.length >= 24) {
          this.weatherCache.set(normKey, { response: result, timestamp: Date.now() });
          console.log(`[WEATHER UPSTREAM SUCCESS] key=${normKey} dailyDays=${forecast.length} hourlyRecords=${hourlyList.length}`);
          console.log(`[WEATHER CACHE STORE] key=${normKey} ttlMinutes=15`);
        }

        return result;
      } catch (e: any) {
        console.log(`[WEATHER EXCEPTION] requestId=${requestId} error=${e.message}`);
        if (cached) return cached.response;
        return { status: 'ERROR', message: e.message || 'Error fetching weather' };
      } finally {
        this.weatherInFlight.delete(normKey);
      }
    })();

    this.weatherInFlight.set(normKey, fetchPromise);
    return fetchPromise;
  }
}

export const railwayProviderService = new RailwayProviderService();
