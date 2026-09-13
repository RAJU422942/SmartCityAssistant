import { Request, Response, NextFunction } from 'express';
import { governmentService } from '../services/governmentService';

export class GovernmentController {
  async getSchemes(req: Request, res: Response, next: NextFunction) {
    try {
      const result = await governmentService.getSchemes();
      return res.json(result);
    } catch (error) {
      next(error);
    }
  }

  async getBenefits(req: Request, res: Response, next: NextFunction) {
    try {
      const result = await governmentService.getBenefits();
      return res.json(result);
    } catch (error) {
      next(error);
    }
  }

  async getNotices(req: Request, res: Response, next: NextFunction) {
    try {
      const result = await governmentService.getNotices();
      return res.json(result);
    } catch (error) {
      next(error);
    }
  }

  async getHelplines(req: Request, res: Response, next: NextFunction) {
    try {
      const result = await governmentService.getHelplines();
      return res.json(result);
    } catch (error) {
      next(error);
    }
  }
}

export const governmentController = new GovernmentController();
