# 🏙️ Smart City Assistant

A comprehensive, production-ready full-stack Android application and secure Node.js backend designed to empower citizens with civic services, emergency assistance, public transit tracking, government schemes, real-time city alerts, and secure account management.

---

## 🚀 Tech Stack

### **Android App**
* **Language:** Kotlin
* **UI Toolkit:** Jetpack Compose & Material 3
* **Concurrency:** Kotlin Coroutines & Flow
* **Networking:** Retrofit 2 & OkHttp 4 (with Logging Interceptors)
* **Security:** EncryptedSharedPreferences (AES-256 GCM)
* **Location & Maps:** Google Play Services Location, Google Maps Compose

### **Backend Server**
* **Runtime:** Node.js & TypeScript
* **Framework:** Express.js
* **Database:** SQLite (`smart_city.db`) with persistent schema support
* **Security & Auth:** JSON Web Tokens (JWT), BcryptJS password hashing, Helmet, Express Rate Limit
* **Integrations:** Nodemailer (Email OTP / Notifications), Twilio (SMS OTP)
* **Deployment:** Render (`https://smart-city-assistant-railway-backend.onrender.com/`)

---

## 🔒 Production Authentication System

The app features a robust full-stack authentication and verification system:
1. **Flexible Login:** Authenticate securely using **Username**, **Email**, or **Mobile Number** alongside a password.
2. **Secure Registration:** Comprehensive sign-up with server-side uniqueness checks, password confirmation validation, and keyboard auto-correct prevention for passwords.
3. **Password Security:** Passwords are never stored in plain text; hashed securely on the backend using `bcryptjs`. Session management relies on cryptographically signed JWT access tokens stored via `EncryptedSharedPreferences`.
4. **Email & Mobile OTP Verification:** Real OTP generation and validation workflows for email addresses and mobile numbers (supporting Indian `+91` numbers).
5. **Forgot Password:** Secure account recovery workflow via OTP verification and password reset.
6. **Profile Integration:** Dedicated verification status hub inside user profile settings (`Verification >`).

---

## 📱 Core Features & Modules

* 🏠 **Home Dashboard:** Quick access to city weather, AQI, alerts, and essential services.
* 🚇 **Public Transport & Railway:** Live train status, PNR enquiry, seat availability, train search, bus schedules, and stop tracking.
* 🚨 **Emergency & Safety:** Emergency SOS contacts, audio recorder, voice classifier, and safety guidelines.
* 🏛️ **Government Portals & Schemes:** Ayushman Bharat, Aadhaar portal links, CPGRAMS grievance filing, welfare benefits, and government notices.
* 🤖 **AI Assistant:** Powered by Gemini for smart citizen queries and shortcut navigation.
* 📝 **Report Problem & Complaints:** Citizen grievance reporting with location tagging and complaint status tracking.
* 📄 **Secure Documents:** Encrypted local storage for government ID documents.
* 🧭 **Utilities:** Compass, WMM2025 magnetic declination calculator, sound, vibration, and direction lock.

---

## 🛠️ Getting Started & Setup

### **1. Backend Setup (`railway-backend`)**
1. Navigate to the backend directory:
   ```bash
   cd railway-backend
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Configure environment variables (copy `.env.example` to `.env`):
   ```env
   PORT=5000
   JWT_SECRET=your_jwt_secret_key_here
   EMAIL_HOST=smtp.example.com
   EMAIL_PORT=587
   EMAIL_USER=your_email@example.com
   EMAIL_PASS=your_email_password
   EMAIL_FROM=noreply@smartcityassistant.com
   TWILIO_ACCOUNT_SID=your_twilio_sid
   TWILIO_AUTH_TOKEN=your_twilio_token
   TWILIO_PHONE_NUMBER=your_twilio_phone
   ```
4. Build and run the server locally:
   ```bash
   npm run build
   npm start
   ```

### **2. Android App Setup (`app`)**
1. Open the project in **Android Studio**.
2. Ensure `local.properties` contains your Google Maps API key:
   ```properties
   MAPS_API_KEY=your_google_maps_api_key
   ```
3. Build the debug APK:
   ```bash
   ./gradlew app:assembleDebug
   ```
4. Install and run on an Android device or emulator.

---

## 🔌 API Endpoints (`/api/v1`)

### **Authentication (`/api/v1/auth`)**
* `POST /register` - Register a new user account
* `POST /login` - Authenticate via username/email/phone + password
* `POST /logout` - Invalidate session / log out
* `GET /me` - Get current authenticated user profile & verification status
* `POST /forgot-password` - Request password reset code
* `POST /reset-password` - Reset password using OTP
* `POST /send-email-otp` - Send email verification code
* `POST /verify-email` - Verify email OTP
* `POST /send-phone-otp` - Send mobile SMS OTP
* `POST /verify-phone` - Verify mobile OTP

### **Railway & Transport (`/api/v1/railway`)**
* `GET /live/{trainNumber}` - Live running status
* `GET /pnr/{pnr}` - PNR status check
* `GET /availability` - Seat availability
* `GET /trains/between/{from}/{to}` - Trains between stations

### **City Alerts & Government (`/api/v1/alerts`, `/api/v1/government`, `/api/v1/ai`)**
* Real-time municipal alerts, government notices, helplines, and AI assistant proxy endpoints.

---

## 📄 License
This project is developed for educational and civic public-service purposes.
