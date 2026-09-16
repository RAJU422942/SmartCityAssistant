import { Request, Response, NextFunction } from 'express';
import { railwayProviderService } from '../services/railwayProviderService';

export class RailwayController {
  async getLiveStatus(req: Request, res: Response, next: NextFunction) {
    try {
      const { trainNumber } = req.params;
      const { date } = req.query;
      if (!trainNumber) {
        return res.status(400).json({ status: 'ERROR', message: 'Train number is required' });
      }
      const result = await railwayProviderService.getLiveStatus(trainNumber, (date as string) || '');
      return res.json(result);
    } catch (error) {
      next(error);
    }
  }

  async getPnrStatus(req: Request, res: Response, next: NextFunction) {
    try {
      const { pnr } = req.params;
      if (!pnr || pnr.length !== 10) {
        return res.status(400).json({ status: 'ERROR', message: 'Valid 10-digit PNR number is required' });
      }
      const result = await railwayProviderService.getPnrStatus(pnr);
      return res.json(result);
    } catch (error) {
      next(error);
    }
  }

  async getSeatAvailability(req: Request, res: Response, next: NextFunction) {
    try {
      const { train, from, to, date, class: trainClass, quota } = req.query;
      if (!train || !from || !to || !date) {
        return res.status(400).json({ status: 'ERROR', message: 'Missing required query parameters (train, from, to, date)' });
      }
      const result = await railwayProviderService.getSeatAvailability(
        train as string,
        from as string,
        to as string,
        date as string,
        (trainClass as string) || '3A',
        (quota as string) || 'GN'
      );
      return res.json(result);
    } catch (error) {
      next(error);
    }
  }

  async getTrainInfo(req: Request, res: Response, next: NextFunction) {
    try {
      const { trainNumber } = req.params;
      if (!trainNumber) {
        return res.status(400).json({ status: 'ERROR', message: 'Train number is required' });
      }
      const result = await railwayProviderService.getTrainInfo(trainNumber);
      return res.json(result);
    } catch (error) {
      next(error);
    }
  }

  async getTrainsBetween(req: Request, res: Response, next: NextFunction) {
    try {
      const { from, to } = req.params;
      const { date } = req.query;
      if (!from || !to) {
        return res.status(400).json({ status: 'ERROR', message: 'From and To station codes are required' });
      }
      const result = await railwayProviderService.getTrainsBetween(from, to, (date as string) || '');
      return res.json(result);
    } catch (error) {
      next(error);
    }
  }

  async searchStations(req: Request, res: Response, next: NextFunction) {
    try {
      const { name } = req.query;
      const result = await railwayProviderService.searchStations((name as string) || '');
      return res.json(result);
    } catch (error) {
      next(error);
    }
  }

  async searchTrains(req: Request, res: Response, next: NextFunction) {
    try {
      const { name } = req.query;
      const result = await railwayProviderService.searchTrainsByName((name as string) || '');
      return res.json(result);
    } catch (error) {
      next(error);
    }
  }

  async getAqi(req: Request, res: Response, next: NextFunction) {
    try {
      const { lat, lon } = req.query;
      if (!lat || !lon) {
        return res.status(400).json({ status: 'ERROR', message: 'Latitude and longitude are required' });
      }
      const result = await railwayProviderService.getAqi(lat as string, lon as string);
      return res.json(result);
    } catch (error) {
      next(error);
    }
  }

  async getWeather(req: Request, res: Response, next: NextFunction) {
    try {
      const { lat, lon } = req.query;
      if (!lat || !lon) {
        return res.status(400).json({ status: 'ERROR', message: 'Latitude and longitude are required' });
      }
      const result = await railwayProviderService.getWeather(lat as string, lon as string);
      return res.json(result);
    } catch (error) {
      next(error);
    }
  }
}

export const railwayController = new RailwayController();
