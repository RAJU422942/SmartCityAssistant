import { Request, Response, NextFunction } from 'express';
import { aiService } from '../services/aiService';

export class AiController {
  async chat(req: Request, res: Response, next: NextFunction) {
    try {
      const { message } = req.body;

      if (!message || typeof message !== 'string' || message.trim() === '') {
        return res.status(400).json({
          success: false,
          error: {
            code: 'INVALID_MESSAGE',
            message: 'Message is required.'
          }
        });
      }

      if (message.length > 2000) {
        return res.status(400).json({
          success: false,
          error: {
            code: 'INVALID_MESSAGE',
            message: 'Message exceeds maximum length of 2000 characters.'
          }
        });
      }

      const data = await aiService.generateChatResponse(message);
      return res.json({
        success: true,
        data
      });
    } catch (error: any) {
      const statusCode = error.statusCode || 500;
      const code = error.errorCode || 'AI_SERVICE_ERROR';
      const message = error.message || 'AI service is temporarily unavailable.';

      return res.status(statusCode).json({
        success: false,
        error: {
          code,
          message
        }
      });
    }
  }
}

export const aiController = new AiController();
