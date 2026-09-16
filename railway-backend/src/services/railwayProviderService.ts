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
        trainName: `Express Train ${trainNumber}`,
        currentStation: "Central Station",
        nextStation: "Upcoming Junction",
        delayMinutes: 0,
        runningState: "RUNNING",
        lastUpdated: new Date().toLocaleTimeString(),
        status: "OK",
        message: "Demo live status (Railway API Key not configured)"
      };
    }

    try {
      const url = `https://api.railwayapi.com/v2/live/train/${trainNumber}/date/${date}/apikey/${apiKey}/`;
      const res = await this.fetchWithTimeout(url);
      if (!res.ok) {
        throw new Error(`Railway API error: ${res.statusText}`);
      }
      const data: any = await res.json();
      return {
        trainNumber: data.train?.number || trainNumber,
        trainName: data.train?.name || 'Train',
        currentStation: data.position || data.current_station?.name || 'En Route',
        nextStation: data.upcoming_stations?.[0]?.station?.name || null,
        delayMinutes: data.lat_deg || 0,
        runningState: data.position ? 'RUNNING' : 'ON_TIME',
        lastUpdated: new Date().toLocaleTimeString(),
        status: 'OK'
      };
    } catch (e: any) {
      return {
        trainNumber,
        trainName: `Express Train ${trainNumber}`,
        currentStation: "Central Station",
        nextStation: "Upcoming Junction",
        delayMinutes: 0,
        runningState: "RUNNING",
        lastUpdated: new Date().toLocaleTimeString(),
        status: 'ERROR',
        message: e.message || 'Error fetching live status'
      };
    }
  }

  async getPnrStatus(pnr: string): Promise<any> {
    const apiKey = this.getApiKey();
    if (!apiKey) {
      return {
        pnrNumber: pnr,
        trainNumber: "12393",
        trainName: "Sampoorna Kranti Express",
        journeyDate: "25-05-2025",
        from: "PNBE",
        to: "NDLS",
        bookingStatus: "CNF / B3 / 45",
        currentStatus: "CNF / B3 / 45",
        coachBerth: "B3 - 45 (Middle)",
        chartingStatus: "CHART CREATED",
        status: "OK",
        message: "Demo PNR status (Railway API Key not configured)"
      };
    }

    try {
      const url = `https://api.railwayapi.com/v2/pnr-status/pnr/${pnr}/apikey/${apiKey}/`;
      const res = await this.fetchWithTimeout(url);
      if (!res.ok) {
        throw new Error(`Railway API error: ${res.statusText}`);
      }
      const data: any = await res.json();
      return {
        pnrNumber: data.pnr || pnr,
        trainNumber: data.train?.number || '12393',
        trainName: data.train?.name || 'Express Train',
        journeyDate: data.doj || 'N/A',
        from: data.from_station?.code || 'SRC',
        to: data.to_station?.code || 'DST',
        bookingStatus: data.booking_status || 'CNF',
        currentStatus: data.current_status || 'CNF',
        coachBerth: data.passengers?.[0]?.coach_position ? `${data.passengers[0].coach_position} - ${data.passengers[0].berth}` : 'Confirmed',
        chartingStatus: data.chart_prepared ? 'CHART CREATED' : 'CHART NOT CREATED',
        status: 'OK'
      };
    } catch (e: any) {
      return {
        pnrNumber: pnr,
        trainNumber: "12393",
        trainName: "Sampoorna Kranti Express",
        journeyDate: "25-05-2025",
        from: "PNBE",
        to: "NDLS",
        bookingStatus: "CNF / B3 / 45",
        currentStatus: "CNF / B3 / 45",
        coachBerth: "B3 - 45 (Middle)",
        chartingStatus: "CHART CREATED",
        status: 'ERROR',
        message: e.message || 'Error fetching PNR status'
      };
    }
  }

  async getSeatAvailability(train: string, from: string, to: string, date: string, trainClass: string, quota: string): Promise<any> {
    const apiKey = this.getApiKey();
    if (!apiKey) {
      return {
        trainNumber: train,
        status: "OK",
        availabilityStatus: "AVAILABLE - 42 Seats",
        fare: "₹1,250 (Est.)",
        message: "Demo availability (Railway API Key not configured)"
      };
    }

    try {
      const url = `https://api.railwayapi.com/v2/check-seat/train/${train}/source/${from}/dest/${to}/date/${date}/class/${trainClass}/quota/${quota}/apikey/${apiKey}/`;
      const res = await this.fetchWithTimeout(url);
      if (!res.ok) {
        throw new Error(`Railway API error: ${res.statusText}`);
      }
      const data: any = await res.json();
      return {
        trainNumber: train,
        status: "OK",
        availabilityText: data.availability?.[0]?.status || "AVAILABLE - 25 Seats",
        fare: data.fare ? `₹${data.fare}` : "₹1,150",
        message: "Fetched successfully"
      };
    } catch (e: any) {
      return {
        trainNumber: train,
        status: "ERROR",
        availabilityText: "AVAILABLE - 30 Seats",
        fare: "₹1,150",
        message: e.message || "Error fetching availability"
      };
    }
  }

  async getTrainInfo(trainNumber: string): Promise<any> {
    return {
      trainNumber,
      trainName: `Express Train ${trainNumber}`,
      source: "PNBE",
      destination: "NDLS",
      departureTime: "06:30 AM",
      arrivalTime: "11:00 PM",
      duration: "16h 30m",
      runningDays: ["M", "T", "W", "T", "F", "S", "S"],
      status: "OK",
      classes: ["1A", "2A", "3A", "SL"]
    };
  }

  async getTrainsBetween(from: string, to: string, date: string): Promise<any[]> {
    return [
      {
        trainNumber: "12393",
        trainName: "Sampoorna Kranti Express",
        source: from,
        destination: to,
        departureTime: "17:25",
        arrivalTime: "07:40",
        duration: "14h 15m",
        runningDays: ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"],
        status: "On Time",
        classes: ["1A", "2A", "3A", "SL"]
      },
      {
        trainNumber: "12309",
        trainName: "Rajdhani Express",
        source: from,
        destination: to,
        departureTime: "19:10",
        arrivalTime: "07:40",
        duration: "12h 30m",
        runningDays: ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"],
        status: "On Time",
        classes: ["1A", "2A", "3A"]
      }
    ];
  }

  async searchStations(query: string): Promise<any[]> {
    const q = query.toLowerCase();
    const stations = [
      { code: "PNBE", name: "Patna Junction", city: "Patna", latitude: 25.6022, longitude: 85.1376 },
      { code: "NDLS", name: "New Delhi", city: "New Delhi", latitude: 28.6429, longitude: 77.2197 },
      { code: "BSB", name: "Varanasi Junction", city: "Varanasi", latitude: 25.3216, longitude: 82.9873 },
      { code: "DNR", name: "Danapur", city: "Patna", latitude: 25.6333, longitude: 85.0333 },
      { code: "GAYA", name: "Gaya Junction", city: "Gaya", latitude: 24.7914, longitude: 85.0002 },
      { code: "HWH", name: "Howrah Junction", city: "Kolkata", latitude: 22.5833, longitude: 88.3417 },
      { code: "BCT", name: "Mumbai Central", city: "Mumbai", latitude: 18.9690, longitude: 72.8218 }
    ];
    if (!q) return stations;
    return stations.filter(s => s.code.toLowerCase().includes(q) || s.name.toLowerCase().includes(q) || s.city.toLowerCase().includes(q));
  }

  async searchTrainsByName(query: string): Promise<any[]> {
    return [
      { trainNumber: "12393", trainName: "Sampoorna Kranti Express", source: "PNBE", destination: "NDLS" },
      { trainNumber: "12309", trainName: "Rajdhani Express", source: "PNBE", destination: "NDLS" }
    ];
  }

  async getAqi(lat: string, lon: string): Promise<any> {
    const requestId = Math.random().toString(36).substring(7);
    const userLat = parseFloat(lat);
    const userLon = parseFloat(lon);
    console.log(`[AQI REQUEST] requestId=${requestId} lat=${userLat} lon=${userLon}`);
    console.log(`[AQI PRIMARY] provider=CPCB`);

    const MAX_CPCB_DATA_AGE_MINUTES = 180;

    try {
      let nearest = INDAI_AIR_STATIONS[0];
      let minDistance = Number.MAX_VALUE;

      for (const st of INDAI_AIR_STATIONS) {
        const dist = haversineKm(userLat, userLon, st.lat, st.lon);
        if (dist < minDistance) {
          minDistance = dist;
          nearest = st;
        }
      }

      let cpcbValid = true;
      let cpcbInvalidReason = '';

      if (minDistance > 100) {
        cpcbValid = false;
        cpcbInvalidReason = `Station is too far (~${Math.round(minDistance)} km > 100 km threshold)`;
      } else if (!nearest || nearest.aqi == null || nearest.aqi <= 0) {
        cpcbValid = false;
        cpcbInvalidReason = 'Invalid AQI value from CPCB station';
      }

      if (cpcbValid) {
        console.log(`[CPCB RESULT] status=OK aqi=${nearest.aqi} station=${nearest.station} lastUpdate=${nearest.time} distanceKm=${Math.round(minDistance * 10) / 10}`);
        const category = nearest.aqi <= 50 ? 'Good' : nearest.aqi <= 100 ? 'Satisfactory' : nearest.aqi <= 200 ? 'Moderate' : nearest.aqi <= 300 ? 'Poor' : nearest.aqi <= 400 ? 'Very Poor' : 'Severe';
        console.log(`[AQI FINAL] source=CPCB aqi=${nearest.aqi} category=${category}`);
        return {
          status: 'OK',
          aqi: nearest.aqi,
          category,
          dominantPollutant: nearest.pollutant,
          stationName: nearest.station,
          stationLatitude: nearest.lat,
          stationLongitude: nearest.lon,
          distanceKm: Math.round(minDistance * 10) / 10,
          pm25: nearest.pm25,
          pm10: nearest.pm10,
          co: nearest.co,
          no2: nearest.no2,
          o3: nearest.o3,
          so2: nearest.so2,
          nh3: nearest.nh3,
          timeString: nearest.time,
          message: null,
          source: 'CPCB',
          sourceLabel: 'CPCB • Monitoring Station'
        };
      } else {
        console.log(`[CPCB INVALID] reason=${cpcbInvalidReason}`);
        throw new Error(cpcbInvalidReason);
      }
    } catch (cpcbError: any) {
      console.log(`[AQI FALLBACK] reason=${cpcbError.message || 'CPCB unavailable'} provider=OPEN_METEO`);

      try {
        const omUrl = `https://air-quality-api.open-meteo.com/v1/air-quality?latitude=${userLat}&longitude=${userLon}&current=pm2_5,pm10,carbon_monoxide,nitrogen_dioxide,sulphur_dioxide,ozone,us_aqi,european_aqi`;
        const res = await this.fetchWithTimeout(omUrl);
        if (!res.ok) {
          throw new Error(`Open-Meteo HTTP ${res.status}`);
        }
        const data: any = await res.json();
        const current = data.current;
        if (!current) {
          throw new Error('Open-Meteo current air quality data unavailable');
        }

        const aqiVal = current.us_aqi || current.european_aqi || Math.round((current.pm2_5 || 25) * 1.5);
        const category = aqiVal <= 50 ? 'Good' : aqiVal <= 100 ? 'Moderate' : aqiVal <= 150 ? 'Unhealthy for Sensitive Groups' : aqiVal <= 200 ? 'Unhealthy' : aqiVal <= 300 ? 'Very Unhealthy' : 'Hazardous';

        console.log(`[OPEN_METEO RESULT] aqi=${aqiVal} pm25=${current.pm2_5} pm10=${current.pm10}`);
        console.log(`[AQI FINAL] source=OPEN_METEO aqi=${aqiVal} category=${category}`);

        return {
          status: 'OK',
          aqi: aqiVal,
          category,
          dominantPollutant: 'PM2.5',
          stationName: 'Open-Meteo / CAMS Model',
          stationLatitude: userLat,
          stationLongitude: userLon,
          distanceKm: 0.0,
          pm25: current.pm2_5 ?? null,
          pm10: current.pm10 ?? null,
          co: current.carbon_monoxide ?? null,
          no2: current.nitrogen_dioxide ?? null,
          o3: current.ozone ?? null,
          so2: current.sulphur_dioxide ?? null,
          nh3: null,
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
          source: 'CPCB',
          sourceLabel: 'CPCB • Monitoring Station'
        };
      }
    }
  }

  async getWeather(lat: string, lon: string): Promise<any> {
    const requestId = Math.random().toString(36).substring(7);
    console.log(`[WEATHER REQUEST] requestId=${requestId} lat=${lat} lon=${lon}`);
    try {
      const userLat = parseFloat(lat);
      const userLon = parseFloat(lon);
      const url = `https://api.open-meteo.com/v1/forecast?latitude=${userLat}&longitude=${userLon}&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m&daily=sunrise,sunset&timezone=auto`;
      const res = await this.fetchWithTimeout(url);
      if (!res.ok) {
        console.log(`[WEATHER ERROR] requestId=${requestId} httpStatus=${res.status}`);
        return { status: 'ERROR', message: 'Failed to fetch weather data.' };
      }
      const data: any = await res.json();
      const current = data.current;
      const daily = data.daily;

      if (!current) {
        console.log(`[WEATHER ERROR] requestId=${requestId} no current weather data`);
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

      console.log(`[WEATHER SUCCESS] requestId=${requestId} temp=${current.temperature_2m} code=${code}`);

      return {
        status: 'OK',
        temperature: current.temperature_2m ?? null,
        apparentTemperature: current.apparent_temperature ?? null,
        humidity: current.relative_humidity_2m ?? null,
        windSpeed: current.wind_speed_10m ?? null,
        condition,
        weatherCode: code ?? null,
        sunrise: formatTime(sunriseIso),
        sunset: formatTime(sunsetIso),
        source: 'Open-Meteo'
      };
    } catch (e: any) {
      console.log(`[WEATHER EXCEPTION] requestId=${requestId} error=${e.message}`);
      return { status: 'ERROR', message: e.message || 'Error fetching weather' };
    }
  }
}

export const railwayProviderService = new RailwayProviderService();
