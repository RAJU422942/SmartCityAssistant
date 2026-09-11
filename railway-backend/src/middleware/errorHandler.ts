import { Request, Response, NextFunction } from 'express';

export const errorHandler = (err: any, req: Request, res: Response, next: NextFunction) => {
  console.error('[Error]:', err.message || err);
  const statusCode = err.statusCode || 500;
  res.status(statusCode).json({
    status: 'ERROR',
    message: err.message || 'Internal Server Error'
  });
};
