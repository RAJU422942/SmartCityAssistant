import {
  BackendLiveStatusResponse,
  BackendPnrResponse,
  BackendAvailabilityResponse,
  UnavailableResponse
} from '../models/railwayModels';

export class RailwayProviderService {
  private getApiKey(): string | undefined {
    return process.env.RAILKIT_API_KEY;
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
          'Content-Type': 'application/json',
          ...(options.headers || {})
        }
      });
      clearTimeout(timeoutId);
      return response;
    } catch (error: any) {
      clearTimeout(timeoutId);
      if (error.name === 'AbortError') {
        throw new Error('RailKit provider request timed out');
      }
      throw error;
    }
  }

  async getLiveStatus(trainNumber: string, date: string): Promise<BackendLiveStatusResponse | UnavailableResponse> {
    if (!this.isConfigured()) {
      return {
        status: 'UNAVAILABLE',
        message: 'Live railway provider is not configured'
      };
    }

    try {
      const url = `${this.getBaseUrl()}/trains/${trainNumber}/live?date=${encodeURIComponent(date)}`;
      const res = await this.fetchWithTimeout(url);

      if (res.status === 401 || res.status === 403) {
        return {
          status: 'ERROR',
          message: 'RailKit provider authentication failed (Invalid API key)'
        };
      }
      if (res.status === 429) {
        return {
          status: 'ERROR',
          message: 'RailKit provider rate limit exceeded'
        };
      }
      if (!res.ok) {
        return {
          status: 'UNAVAILABLE',
          message: `RailKit provider unavailable (HTTP ${res.status})`
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
      console.error('[RailKit Live Error]:', error.message);
      return {
        status: 'ERROR',
        message: error.message || 'Failed to fetch live train status from RailKit'
      };
    }
  }

  async getPnrStatus(pnr: string): Promise<BackendPnrResponse | UnavailableResponse> {
    if (!this.isConfigured()) {
      return {
        status: 'UNAVAILABLE',
        message: 'Live railway provider is not configured'
      };
    }

    try {
      const url = `${this.getBaseUrl()}/pnr/${pnr}`;
      const res = await this.fetchWithTimeout(url);

      if (res.status === 401 || res.status === 403) {
        return {
          status: 'ERROR',
          message: 'RailKit provider authentication failed (Invalid API key)'
        };
      }
      if (res.status === 429) {
        return {
          status: 'ERROR',
          message: 'RailKit provider rate limit exceeded'
        };
      }
      if (!res.ok) {
        return {
          status: 'UNAVAILABLE',
          message: `RailKit PNR unavailable (HTTP ${res.status})`
        };
      }

      const data: any = await res.json();
      return {
        pnrNumber: data.pnrNumber || pnr,
        trainNumber: data.trainNumber || 'N/A',
        trainName: data.trainName || 'N/A',
        journeyDate: data.journeyDate || 'N/A',
        from: data.from || 'N/A',
        to: data.to || 'N/A',
        bookingStatus: data.bookingStatus || 'N/A',
        currentStatus: data.currentStatus || 'N/A',
        coachBerth: data.coachBerth || 'N/A',
        chartingStatus: data.chartingStatus || 'N/A'
      };
    } catch (error: any) {
      console.error('[RailKit PNR Error]:', error.message);
      return {
        status: 'ERROR',
        message: error.message || 'Failed to fetch PNR status from RailKit'
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
        message: 'Live railway provider is not configured'
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

      if (res.status === 401 || res.status === 403) {
        return {
          status: 'ERROR',
          message: 'RailKit provider authentication failed (Invalid API key)'
        };
      }
      if (res.status === 429) {
        return {
          status: 'ERROR',
          message: 'RailKit provider rate limit exceeded'
        };
      }
      if (!res.ok) {
        return {
          status: 'UNAVAILABLE',
          message: `RailKit availability unavailable (HTTP ${res.status})`
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
      console.error('[RailKit Availability Error]:', error.message);
      return {
        status: 'ERROR',
        message: error.message || 'Failed to fetch seat availability from RailKit'
      };
    }
  }
}

export const railwayProviderService = new RailwayProviderService();
