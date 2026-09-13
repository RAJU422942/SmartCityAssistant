import { Request, Response, NextFunction } from 'express';
import { cityAlertsService } from '../services/cityAlertsService';

export class CityAlertsController {
  async getAlerts(req: Request, res: Response, next: NextFunction) {
    try {
      const { lat, lon, city, district, category } = req.query;

      const latitude = lat ? parseFloat(lat as string) : undefined;
      const longitude = lon ? parseFloat(lon as string) : undefined;

      const result = await cityAlertsService.getAlerts(
        latitude,
        longitude,
        city as string,
        district as string,
        category as string
      );
      return res.json(result);
    } catch (error) {
      next(error);
    }
  }
}

export const cityAlertsController = new CityAlertsController();
