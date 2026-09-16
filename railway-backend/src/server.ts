import dotenv from 'dotenv';
dotenv.config();

import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import rateLimit from 'express-rate-limit';
import railwayRoutes from './routes/railwayRoutes';
import cityAlertsRoutes from './routes/cityAlertsRoutes';
import governmentRoutes from './routes/governmentRoutes';
import aiRoutes from './routes/aiRoutes';
import { errorHandler } from './middleware/errorHandler';

const app = express();
app.set('trust proxy', 1);
const PORT = process.env.PORT || 5000;

// Security & Middleware
app.use(helmet());
app.use(cors());
app.use(express.json());

// Rate Limiting
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100, // limit each IP to 100 requests per windowMs
  message: { status: 'ERROR', message: 'Too many requests, please try again later.' }
});
app.use('/api/', limiter);

// Health Check
app.get('/health', (req, res) => {
  res.json({ status: 'UP', timestamp: new Date().toISOString() });
});

// Routes
app.use('/api/v1/railway', railwayRoutes);
app.use('/api/v1/alerts', cityAlertsRoutes);
app.use('/api/v1/city-alerts', cityAlertsRoutes);
app.use('/api/v1/government', governmentRoutes);
app.use('/api/v1/ai', aiRoutes);

// Error Handler
app.use(errorHandler);

const server = app.listen(PORT, () => {
  console.log(`[Server]: Railway backend proxy running on port ${PORT}`);
});

// Graceful Shutdown
const shutdown = () => {
  console.log('[Server]: Shutting down gracefully...');
  server.close(() => {
    console.log('[Server]: Closed all HTTP connections.');
    process.exit(0);
  });
};

process.on('SIGTERM', shutdown);
process.on('SIGINT', shutdown);
