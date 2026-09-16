import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';
import { db } from '../models/db';

const JWT_SECRET = process.env.JWT_SECRET || 'smart_city_assistant_super_secret_jwt_key_2025';

export interface AuthenticatedRequest extends Request {
  user?: {
    id: number;
    username: string;
    email: string;
    phone: string;
    tokenVersion?: number;
  };
}

export const authenticateToken = (req: AuthenticatedRequest, res: Response, next: NextFunction): void => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) {
    res.status(401).json({ status: 'ERROR', message: 'Access token missing or unauthorized.' });
    return;
  }

  jwt.verify(token, JWT_SECRET, (err, decoded: any) => {
    if (err) {
      res.status(403).json({ status: 'ERROR', message: 'Invalid or expired token.' });
      return;
    }

    db.get(`SELECT id, username, email, phone, tokenVersion FROM users WHERE id = ?`, [decoded.id], (dbErr, user: any) => {
      if (dbErr || !user) {
        res.status(401).json({ status: 'ERROR', message: 'User no longer exists.' });
        return;
      }

      if (decoded.tokenVersion !== undefined && user.tokenVersion !== undefined && decoded.tokenVersion !== user.tokenVersion) {
        res.status(401).json({ status: 'ERROR', message: 'Token has been revoked or logged out.' });
        return;
      }

      req.user = {
        id: user.id,
        username: user.username,
        email: user.email,
        phone: user.phone,
        tokenVersion: user.tokenVersion,
      };
      next();
    });
  });
};
