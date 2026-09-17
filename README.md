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

## 📱 Project Screenshots
### Home Screen
<img width="351" height="757" alt="image" src="https://github.com/user-attachments/assets/02496ce1-44d1-4c60-a4c3-0335234c7159" />
<img width="347" height="748" alt="image" src="https://github.com/user-attachments/assets/b43065a3-5d6f-4547-9362-2fa22b133e0c" />


### Emergency Screen
<img width="358" height="756" alt="image" src="https://github.com/user-attachments/assets/08a86ca8-573d-47ec-97fa-cb3a26f5f52a" />
<img width="353" height="721" alt="image" src="https://github.com/user-attachments/assets/4d6e3797-958a-4ab5-91f2-e511c848977d" />
<img width="356" height="731" alt="image" src="https://github.com/user-attachments/assets/ac8175a0-0b31-4de1-8826-6094fda37344" />
<img width="350" height="730" alt="image" src="https://github.com/user-attachments/assets/ac4185ac-f0aa-4534-927c-5113d9700a64" />
<img width="352" height="737" alt="image" src="https://github.com/user-attachments/assets/dfcf829f-81e1-469e-8c4c-17642e6529ab" />
<img width="317" height="736" alt="image" src="https://github.com/user-attachments/assets/48c52462-9193-4ce3-81f7-573d7a637e99" />

### Report Problem
<img width="347" height="722" alt="image" src="https://github.com/user-attachments/assets/87a8274c-7d07-4275-bb29-e3053f26eaa0" />
<img width="353" height="725" alt="image" src="https://github.com/user-attachments/assets/f026a2c9-92ee-4213-b954-d855017015ca" />
<img width="360" height="732" alt="image" src="https://github.com/user-attachments/assets/fc466306-e552-4a51-86bb-33079f39be39" />
<img width="355" height="737" alt="image" src="https://github.com/user-attachments/assets/894fa0a7-f6f3-41b5-a546-09ba8aaa7743" />
<img width="355" height="727" alt="image" src="https://github.com/user-attachments/assets/bf111660-6cb0-4502-8054-2b27439eef9a" />
<img width="358" height="732" alt="image" src="https://github.com/user-attachments/assets/20733e0b-4a1e-4d33-8da2-0d6628fb3dcc" />

### my complaints
<img width="356" height="737" alt="image" src="https://github.com/user-attachments/assets/c65e0120-cca7-4e90-98dd-a0758a36a486" />


### nearby service
<img width="347" height="736" alt="image" src="https://github.com/user-attachments/assets/2d8f3a19-ed1f-417b-9942-ae354403066c" />
<img width="358" height="747" alt="image" src="https://github.com/user-attachments/assets/be606ff1-94d2-488d-96da-d837def95e18" />

### Government
<img width="353" height="736" alt="image" src="https://github.com/user-attachments/assets/079711e2-623f-4a4a-8df9-30e4a921541b" />
<img width="348" height="736" alt="image" src="https://github.com/user-attachments/assets/4268a5a3-1588-4fc3-9854-3ec126e9fd35" />
<img width="352" height="737" alt="image" src="https://github.com/user-attachments/assets/a25fe45f-009e-4cf7-a42b-d04ae4cc2e2c" />
<img width="347" height="737" alt="image" src="https://github.com/user-attachments/assets/65fa9035-8cb9-4fdb-af70-528e687f4168" />
<img width="353" height="733" alt="image" src="https://github.com/user-attachments/assets/c8033727-48f0-4486-8ced-4bf9c1755ae0" />
<img width="341" height="750" alt="image" src="https://github.com/user-attachments/assets/c7fa8ec5-e2bd-4b0b-998e-1b6794480773" />
<img width="357" height="742" alt="image" src="https://github.com/user-attachments/assets/b4d8d6b1-f813-423f-b5ab-8157d9a481e7" />

### city Alerts
<img width="353" height="731" alt="image" src="https://github.com/user-attachments/assets/f3e27523-55e8-4b1b-9d5e-b9ffd2448ede" />

### compas
<img width="347" height="756" alt="image" src="https://github.com/user-attachments/assets/7d44eff4-5f13-4ab0-afd3-305af52cd1bf" />
<img width="353" height="750" alt="image" src="https://github.com/user-attachments/assets/a3585ccd-3e07-4499-825c-bb9bcb37a160" />
<img width="352" height="757" alt="image" src="https://github.com/user-attachments/assets/39cfa640-c563-4b52-a1c8-113a5787a8b6" />

### Ai Assistant
<img width="357" height="743" alt="image" src="https://github.com/user-attachments/assets/3c128c52-c6c9-401c-a100-79a14e7681f7" />
### Explore
<img width="362" height="748" alt="image" src="https://github.com/user-attachments/assets/5fc5ecf2-4809-4653-9f2f-3df1aad29ff4" />

### profile
<img width="357" height="743" alt="image" src="https://github.com/user-attachments/assets/33afbc24-7537-4cd7-99e3-0603186a8ef7" />
<img width="356" height="748" alt="image" src="https://github.com/user-attachments/assets/30685939-fd7f-4e51-ad18-765e029f9fa3" />
<img width="358" height="737" alt="image" src="https://github.com/user-attachments/assets/45807c83-2845-4115-971d-f1ceeb8989c7" />
<img width="352" height="753" alt="image" src="https://github.com/user-attachments/assets/f7411353-e148-4793-be9b-7b2c8456a4cd" />
<img width="353" height="753" alt="image" src="https://github.com/user-attachments/assets/42dc6387-f92a-4536-8306-4ebae7fc88c0" />

### Author
Raju kumar sah



































