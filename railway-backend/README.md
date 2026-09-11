# Smart City Assistant - Secure Railway Backend Proxy

Secure backend proxy designed to communicate with legitimate Indian Railways / NTES / IRCTC partner API providers, shielding API keys and provider credentials from the Android APK.

## Setup & Run

1. Install dependencies:
   ```bash
   npm install
   ```
2. Configure environment variables:
   ```bash
   cp .env.example .env
   ```
   (Fill in your `RAILWAY_API_KEY` in `.env`)
3. Build and start:
   ```bash
   npm run build
   npm start
   ```
   Or for development:
   ```bash
   npm run dev
   ```

## Endpoints

- `GET /health`
- `GET /api/v1/railway/live/:trainNumber?date=DD-MM-YYYY`
- `GET /api/v1/railway/pnr/:pnr`
- `GET /api/v1/railway/availability?train=...&from=...&to=...&date=...&class=...&quota=...`
