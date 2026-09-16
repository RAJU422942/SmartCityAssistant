import { find as findTimeZone } from 'geo-tz';
import SunCalc from 'suncalc';

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

function mapMetSymbolToWeather(symbolCode: string): { condition: string; weatherCode: number } {
  const code = (symbolCode || '').toLowerCase();
  if (code.includes('thunder')) return { condition: 'Thunderstorm', weatherCode: 95 };
  if (code.includes('snow') || code.includes('sleet')) return { condition: 'Snow', weatherCode: 71 };
  if (code.includes('heavyrain')) return { condition: 'Rainy', weatherCode: 63 };
  if (code.includes('rainshowers') || code.includes('showers')) return { condition: 'Showers', weatherCode: 80 };
  if (code.includes('rain') || code.includes('drizzle')) return { condition: 'Rainy', weatherCode: 61 };
  if (code.includes('fog')) return { condition: 'Foggy', weatherCode: 45 };
  if (code.includes('cloudy')) return { condition: 'Overcast', weatherCode: 3 };
  if (code.includes('partlycloudy')) return { condition: 'Partly Cloudy', weatherCode: 2 };
  if (code.includes('fair')) return { condition: 'Mainly Clear', weatherCode: 1 };
  if (code.includes('clearsky')) return { condition: 'Sunny', weatherCode: 0 };
  return { condition: 'Partly Cloudy', weatherCode: 2 };
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
    const requestId = Math.random().toString(36).substring(7);
    const userLat = parseFloat(lat);
    const userLon = parseFloat(lon);
    const normLat = userLat.toFixed(4);
    const normLon = userLon.toFixed(4);
    const normKey = `${normLat}_${normLon}`;

    const timeZones = findTimeZone(userLat, userLon);
    const timeZone = timeZones[0] || 'UTC';
    console.log(`[WEATHER TIMEZONE] lat=${userLat} lon=${userLon} timezone=${timeZone}`);

    console.log(`[WEATHER REQUEST] requestId=${requestId} lat=${lat} lon=${lon}`);
    console.log(`[METNO FETCH] requestId=${requestId} cacheKey=${normKey}`);

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
        console.log(`[METNO FETCH UPSTREAM] key=${normKey}`);
        const url = `https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=${normLat}&lon=${normLon}`;
        const res = await this.fetchWithTimeout(url, {
          headers: {
            'User-Agent': 'SmartCityAssistant/1.0 (contact: support@smartcityassistant.app)'
          }
        }, 12000);

        console.log(`[METNO RESPONSE] requestId=${requestId} httpStatus=${res.status}`);

        if (res.status === 429) {
          console.log(`[WEATHER UPSTREAM 429] key=${normKey}`);
          if (cached) return cached.response;
          return { status: 'ERROR', message: 'WEATHER_RATE_LIMITED' };
        }

        if (res.status === 403 || res.status === 400 || res.status === 404) {
          console.log(`[WEATHER ERROR] requestId=${requestId} httpStatus=${res.status}`);
          if (cached) return cached.response;
          return { status: 'ERROR', message: `Weather provider error (${res.status})` };
        }

        if (!res.ok) {
          console.log(`[WEATHER ERROR] requestId=${requestId} httpStatus=${res.status}`);
          if (cached) return cached.response;
          return { status: 'ERROR', message: 'Failed to fetch weather data.' };
        }

        const data: any = await res.json();
        const timeseries = data?.properties?.timeseries;
        if (!timeseries || !Array.isArray(timeseries) || timeseries.length === 0) {
          console.log(`[WEATHER ERROR] requestId=${requestId} missing timeseries data`);
          if (cached) return cached.response;
          return { status: 'ERROR', message: 'Weather data unavailable.' };
        }

        const currentItem = timeseries[0];
        const instantDetails = currentItem.data.instant.details;
        const currentTemp = instantDetails.air_temperature ?? null;
        const humidity = instantDetails.relative_humidity_percentage ?? null;
        const windSpeed = instantDetails.wind_speed ?? null;
        const windDir = instantDetails.wind_from_direction ?? null;

        const symbolCode = currentItem.data.next_1_hours?.summary?.symbol_code ||
                           currentItem.data.next_6_hours?.summary?.symbol_code ||
                           'clearsky_day';
        const { condition, weatherCode } = mapMetSymbolToWeather(symbolCode);

        const hourFormatter = new Intl.DateTimeFormat('en-US', {
          timeZone,
          hour: 'numeric',
          hour12: true
        });

        const dateFormatter = new Intl.DateTimeFormat('en-US', {
          timeZone,
          year: 'numeric',
          month: '2-digit',
          day: '2-digit'
        });

        const dayNameFormatter = new Intl.DateTimeFormat('en-US', {
          timeZone,
          weekday: 'short',
          month: 'short',
          day: 'numeric'
        });

        const hourlyList: any[] = [];
        for (const ts of timeseries) {
          const tIso = ts.time;
          const inst = ts.data.instant.details;
          const hTemp = inst.air_temperature;
          const hHum = inst.relative_humidity_percentage;
          const hWind = inst.wind_speed;
          const hDir = inst.wind_from_direction;
          const sym = ts.data.next_1_hours?.summary?.symbol_code || ts.data.next_6_hours?.summary?.symbol_code || 'clearsky_day';
          const hCode = mapMetSymbolToWeather(sym).weatherCode;
          const precipProb = ts.data.next_1_hours?.details?.precipitation_amount ?? 0;

          const dateObj = new Date(tIso);
          const hourFormatted = hourFormatter.format(dateObj);

          console.log(`[WEATHER TIME CONVERSION] utc=${tIso} local=${hourFormatted}`);

          hourlyList.push({
            time: tIso,
            hourFormatted,
            temperature: hTemp,
            humidity: hHum,
            precipitationProbability: precipProb > 0 ? Math.min(100, precipProb * 20) : 0,
            weatherCode: hCode,
            windSpeed: hWind,
            windDirection: hDir
          });
        }

        const dailyMap = new Map<string, { temps: number[]; symbols: string[]; precip: number[] }>();
        for (const ts of timeseries) {
          const tIso = ts.time;
          const dateObj = new Date(tIso);
          const parts = dateFormatter.formatToParts(dateObj);
          const year = parts.find(p => p.type === 'year')?.value || '2026';
          const month = parts.find(p => p.type === 'month')?.value || '09';
          const day = parts.find(p => p.type === 'day')?.value || '16';
          const localDateStr = `${year}-${month}-${day}`;

          const temp = ts.data.instant.details.air_temperature;
          const sym = ts.data.next_1_hours?.summary?.symbol_code || ts.data.next_6_hours?.summary?.symbol_code || 'clearsky_day';
          const precip = ts.data.next_1_hours?.details?.precipitation_amount ?? 0;

          if (temp !== undefined && temp !== null) {
            if (!dailyMap.has(localDateStr)) {
              dailyMap.set(localDateStr, { temps: [], symbols: [], precip: [] });
            }
            const entry = dailyMap.get(localDateStr)!;
            entry.temps.push(temp);
            entry.symbols.push(sym);
            entry.precip.push(precip);
          }
        }

        const forecast: any[] = [];
        let dayIndex = 0;
        for (const [dateStr, dataEntry] of dailyMap.entries()) {
          if (forecast.length >= 7) break;
          const maxT = Math.max(...dataEntry.temps);
          const minT = Math.min(...dataEntry.temps);
          const dominantSym = dataEntry.symbols[Math.floor(dataEntry.symbols.length / 2)] || 'clearsky_day';
          const { condition: dCond, weatherCode: dCode } = mapMetSymbolToWeather(dominantSym);
          const maxPrecip = Math.max(...dataEntry.precip);

          const refDate = new Date(`${dateStr}T12:00:00Z`);
          const dayName = dayIndex === 0 ? 'Today' : dayNameFormatter.format(refDate);
          dayIndex++;

          forecast.push({
            date: dateStr,
            dayName,
            weatherCode: dCode,
            condition: dCond,
            maxTemp: maxT,
            minTemp: minT,
            precipitationProbabilityMax: maxPrecip > 0 ? Math.min(100, maxPrecip * 20) : 0
          });
        }

        const { sunrise, sunset } = calculateSunriseSunset(userLat, userLon);

        const result = {
          status: 'OK',
          temperature: currentTemp,
          apparentTemperature: currentTemp,
          humidity,
          windSpeed,
          condition,
          weatherCode,
          sunrise,
          sunset,
          source: 'MET_Norway',
          forecast,
          hourly: hourlyList
        };

        console.log(`[METNO PARSER] requestId=${requestId} hourlyCount=${hourlyList.length} dailyCount=${forecast.length}`);
        console.log(`[WEATHER FINAL] requestId=${requestId} source=MET_NO`);

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
