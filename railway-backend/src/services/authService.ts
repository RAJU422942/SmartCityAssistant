import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import crypto from 'crypto';
import { db } from '../models/db';
import nodemailer from 'nodemailer';

const JWT_SECRET = process.env.JWT_SECRET || 'smart_city_assistant_super_secret_jwt_key_2025';
const TOKEN_EXPIRES_IN = '7d';

// Helper to send real email via Nodemailer
async function sendEmailNotification(to: string, subject: string, text: string): Promise<void> {
  const smtpHost = process.env.EMAIL_HOST || process.env.SMTP_HOST;
  const smtpPort = process.env.EMAIL_PORT || process.env.SMTP_PORT || '587';
  const smtpUser = process.env.EMAIL_USER || process.env.SMTP_USER || process.env.EMAIL_FROM;
  const smtpPass = process.env.EMAIL_PASS || process.env.SMTP_PASS || process.env.EMAIL_PROVIDER_API_KEY;

  if (!smtpHost || !smtpUser || !smtpPass) {
    throw new Error('Email verification is not configured yet.');
  }

  const transporter = nodemailer.createTransport({
    host: smtpHost,
    port: parseInt(smtpPort),
    secure: parseInt(smtpPort) === 465,
    auth: { user: smtpUser, pass: smtpPass },
  });

  await transporter.sendMail({
    from: process.env.EMAIL_FROM || smtpUser,
    to,
    subject,
    text,
  });
  console.log(`[AuthService]: Real email sent successfully to ${to}`);
}

// Helper to send real SMS via Twilio
async function sendSmsNotification(phone: string, message: string): Promise<void> {
  const twilioAccountSid = process.env.TWILIO_ACCOUNT_SID;
  const twilioAuthToken = process.env.TWILIO_AUTH_TOKEN;
  const twilioPhone = process.env.TWILIO_PHONE_NUMBER;

  if (!twilioAccountSid || !twilioAuthToken || !twilioPhone) {
    throw new Error('Mobile verification is not configured yet.');
  }

  const url = `https://api.twilio.com/2010-04-01/Accounts/${twilioAccountSid}/Messages.json`;
  const body = new URLSearchParams({
    To: phone,
    From: twilioPhone,
    Body: message,
  });
  const auth = Buffer.from(`${twilioAccountSid}:${twilioAuthToken}`).toString('base64');
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Authorization': `Basic ${auth}`,
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: body.toString(),
  });

  if (!response.ok) {
    const errorText = await response.text();
    console.error('[AuthService]: Twilio SMS send failed:', errorText);
    throw new Error('Failed to send SMS verification code.');
  }
  console.log(`[AuthService]: Real SMS sent successfully via Twilio to ${phone}`);
}

export class AuthService {
  static generateToken(user: { id: number; username: string; email: string; phone: string }) {
    return jwt.sign(user, JWT_SECRET, { expiresIn: TOKEN_EXPIRES_IN });
  }

  static async register(data: {
    fullName: string;
    username: string;
    email: string;
    phone: string;
    password: string;
  }): Promise<{ token: string; user: any }> {
    const { fullName, username, email, phone, password } = data;

    let normalizedPhone = phone.trim();
    if (!normalizedPhone.startsWith('+')) {
      if (normalizedPhone.length === 10) {
        normalizedPhone = '+91' + normalizedPhone;
      } else {
        normalizedPhone = '+' + normalizedPhone;
      }
    }

    return new Promise((resolve, reject) => {
      db.get(
        `SELECT id, username, email, phone FROM users WHERE username = ? OR email = ? OR phone = ?`,
        [username.toLowerCase().trim(), email.toLowerCase().trim(), normalizedPhone],
        async (err, row: any) => {
          if (err) return reject(new Error('Database error during registration check.'));
          if (row) {
            if (row.username === username.toLowerCase().trim()) {
              return reject(new Error('Username is already taken.'));
            }
            if (row.email === email.toLowerCase().trim()) {
              return reject(new Error('Email is already registered.'));
            }
            if (row.phone === normalizedPhone) {
              return reject(new Error('Phone number is already registered.'));
            }
            return reject(new Error('Account with this username, email, or phone already exists.'));
          }

          const salt = await bcrypt.genSalt(10);
          const passwordHash = await bcrypt.hash(password, salt);

          // Cryptographically secure 6-digit OTP using crypto.randomInt
          const emailOtp = crypto.randomInt(100000, 1000000).toString();
          const emailOtpExpires = Date.now() + 10 * 60 * 1000;
          const phoneOtp = crypto.randomInt(100000, 1000000).toString();
          const phoneOtpExpires = Date.now() + 10 * 60 * 1000;

          db.run(
            `INSERT INTO users (fullName, username, email, phone, passwordHash, emailVerified, phoneVerified, emailOtp, emailOtpExpires, phoneOtp, phoneOtpExpires)
             VALUES (?, ?, ?, ?, ?, 0, 0, ?, ?, ?, ?)`,
            [
              fullName.trim(),
              username.toLowerCase().trim(),
              email.toLowerCase().trim(),
              normalizedPhone,
              passwordHash,
              emailOtp,
              emailOtpExpires,
              phoneOtp,
              phoneOtpExpires,
            ],
            async function (insertErr) {
              if (insertErr) return reject(new Error('Failed to create user account.'));
              const userId = this.lastID;

              // Try sending initial OTPs if configured (ignoring errors on register if provider not configured)
              try {
                await sendEmailNotification(
                  email,
                  'Smart City Assistant - Email Verification Code',
                  `Hello ${fullName},\n\nYour email verification code is: ${emailOtp}\n\nThis code expires in 10 minutes.\n\nSmart City Assistant Team`
                );
              } catch (e) {
                console.log('[AuthService]: Email provider not configured during register.');
              }

              try {
                await sendSmsNotification(
                  normalizedPhone,
                  `Smart City Assistant OTP: ${emailOtp}. Verify your account securely.`
                );
              } catch (e) {
                console.log('[AuthService]: SMS provider not configured during register.');
              }

              const userObj = {
                id: userId,
                fullName: fullName.trim(),
                username: username.toLowerCase().trim(),
                email: email.toLowerCase().trim(),
                phone: normalizedPhone,
                emailVerified: false,
                phoneVerified: false,
              };

              const token = AuthService.generateToken(userObj);
              resolve({ token, user: userObj });
            }
          );
        }
      );
    });
  }

  static async login(identifier: string, password: string): Promise<{ token: string; user: any }> {
    const cleanIdentifier = identifier.trim();
    let queryField = 'username';
    let queryVal = cleanIdentifier.toLowerCase();

    if (cleanIdentifier.includes('@')) {
      queryField = 'email';
    } else if (cleanIdentifier.startsWith('+') || /^\d{10,12}$/.test(cleanIdentifier)) {
      queryField = 'phone';
      if (!cleanIdentifier.startsWith('+')) {
        queryVal = '+91' + cleanIdentifier;
      }
    }

    return new Promise((resolve, reject) => {
      db.get(
        `SELECT * FROM users WHERE ${queryField} = ?`,
        [queryVal],
        async (err, user: any) => {
          if (err) return reject(new Error('Database error during login.'));
          if (!user) {
            return reject(new Error('Incorrect username, email, phone number or password.'));
          }

          const isMatch = await bcrypt.compare(password, user.passwordHash);
          if (!isMatch) {
            return reject(new Error('Incorrect username, email, phone number or password.'));
          }

          // CRITICAL: Login MUST NOT modify emailVerified or phoneVerified status.
          const userObj = {
            id: user.id,
            fullName: user.fullName,
            username: user.username,
            email: user.email,
            phone: user.phone,
            emailVerified: user.emailVerified === 1,
            phoneVerified: user.phoneVerified === 1,
          };

          const token = AuthService.generateToken(userObj);
          resolve({ token, user: userObj });
        }
      );
    });
  }

  static async getUserById(userId: number): Promise<any> {
    return new Promise((resolve, reject) => {
      db.get(
        `SELECT id, fullName, username, email, phone, emailVerified, phoneVerified, createdAt FROM users WHERE id = ?`,
        [userId],
        (err, user: any) => {
          if (err) return reject(new Error('Database error.'));
          if (!user) return reject(new Error('User not found.'));
          resolve({
            ...user,
            emailVerified: user.emailVerified === 1,
            phoneVerified: user.phoneVerified === 1,
          });
        }
      );
    });
  }

  static async sendEmailOtp(userId: number): Promise<void> {
    const otp = crypto.randomInt(100000, 1000000).toString();
    const expires = Date.now() + 10 * 60 * 1000;

    return new Promise((resolve, reject) => {
      db.get(`SELECT email, fullName, emailOtpExpires FROM users WHERE id = ?`, [userId], async (err, user: any) => {
        if (err || !user) return reject(new Error('User not found.'));

        // Resend cooldown check (e.g. 60 seconds since last generation or creation)
        if (user.emailOtpExpires && (user.emailOtpExpires - (10 * 60 * 1000) + 60 * 1000) > Date.now()) {
          return reject(new Error('Please wait before requesting another verification code.'));
        }

        db.run(`UPDATE users SET emailOtp = ?, emailOtpExpires = ? WHERE id = ?`, [otp, expires, userId], async (updateErr) => {
          if (updateErr) return reject(new Error('Failed to generate email OTP.'));

          try {
            await sendEmailNotification(
              user.email,
              'Smart City Assistant - Email Verification OTP',
              `Hello ${user.fullName},\n\nYour email verification code is: ${otp}\n\nThis code expires in 10 minutes.\n\nSmart City Assistant`
            );
            resolve();
          } catch (mailErr: any) {
            reject(mailErr);
          }
        });
      });
    });
  }

  static async verifyEmailOtp(userId: number, otp: string): Promise<void> {
    return new Promise((resolve, reject) => {
      db.get(`SELECT emailOtp, emailOtpExpires FROM users WHERE id = ?`, [userId], (err, user: any) => {
        if (err || !user) return reject(new Error('User not found.'));

        if (!user.emailOtp || user.emailOtp !== otp.trim()) {
          return reject(new Error('Incorrect verification code.'));
        }

        if (Date.now() > user.emailOtpExpires) {
          return reject(new Error('This verification code has expired. Please request a new code.'));
        }

        db.run(`UPDATE users SET emailVerified = 1, emailOtp = NULL, emailOtpExpires = NULL WHERE id = ?`, [userId], (updateErr) => {
          if (updateErr) return reject(new Error('Failed to update verification status.'));
          resolve();
        });
      });
    });
  }

  static async sendPhoneOtp(userId: number): Promise<void> {
    const otp = crypto.randomInt(100000, 1000000).toString();
    const expires = Date.now() + 10 * 60 * 1000;

    return new Promise((resolve, reject) => {
      db.get(`SELECT phone, fullName, phoneOtpExpires FROM users WHERE id = ?`, [userId], async (err, user: any) => {
        if (err || !user) return reject(new Error('User not found.'));

        // Resend cooldown check
        if (user.phoneOtpExpires && (user.phoneOtpExpires - (10 * 60 * 1000) + 60 * 1000) > Date.now()) {
          return reject(new Error('Please wait before requesting another verification code.'));
        }

        db.run(`UPDATE users SET phoneOtp = ?, phoneOtpExpires = ? WHERE id = ?`, [otp, expires, userId], async (updateErr) => {
          if (updateErr) return reject(new Error('Failed to generate phone OTP.'));

          try {
            await sendSmsNotification(
              user.phone,
              `Smart City Assistant Mobile Verification OTP: ${otp}. Valid for 10 minutes.`
            );
            resolve();
          } catch (smsErr: any) {
            reject(smsErr);
          }
        });
      });
    });
  }

  static async verifyPhoneOtp(userId: number, otp: string): Promise<void> {
    return new Promise((resolve, reject) => {
      db.get(`SELECT phoneOtp, phoneOtpExpires FROM users WHERE id = ?`, [userId], (err, user: any) => {
        if (err || !user) return reject(new Error('User not found.'));

        if (!user.phoneOtp || user.phoneOtp !== otp.trim()) {
          return reject(new Error('Incorrect verification code.'));
        }

        if (Date.now() > user.phoneOtpExpires) {
          return reject(new Error('This verification code has expired. Please request a new code.'));
        }

        db.run(`UPDATE users SET phoneVerified = 1, phoneOtp = NULL, phoneOtpExpires = NULL WHERE id = ?`, [userId], (updateErr) => {
          if (updateErr) return reject(new Error('Failed to update verification status.'));
          resolve();
        });
      });
    });
  }

  static async forgotPassword(identifier: string): Promise<void> {
    const clean = identifier.trim().toLowerCase();
    let field = 'email';
    let val = clean;
    if (!clean.includes('@') && (clean.startsWith('+') || /^\d{10,12}$/.test(clean))) {
      field = 'phone';
      val = clean.startsWith('+') ? clean : '+91' + clean;
    } else if (!clean.includes('@')) {
      field = 'username';
    }

    return new Promise((resolve, reject) => {
      db.get(`SELECT id, email, phone, fullName FROM users WHERE ${field} = ?`, [val], async (err, user: any) => {
        if (err || !user) {
          return resolve();
        }

        const resetOtp = crypto.randomInt(100000, 1000000).toString();
        const expires = Date.now() + 15 * 60 * 1000;

        db.run(`UPDATE users SET resetOtp = ?, resetOtpExpires = ? WHERE id = ?`, [resetOtp, expires, user.id], async (updateErr) => {
          if (updateErr) return resolve();

          try {
            await sendEmailNotification(
              user.email,
              'Smart City Assistant - Password Reset Code',
              `Hello ${user.fullName},\n\nYour password reset verification code is: ${resetOtp}\n\nValid for 15 minutes.`
            );
          } catch (e) {
            console.log('[AuthService]: Email provider not configured for password reset.');
          }
          resolve();
        });
      });
    });
  }

  static async resetPassword(identifier: string, otp: string, newPassword: string): Promise<void> {
    const clean = identifier.trim().toLowerCase();
    let field = 'email';
    let val = clean;
    if (!clean.includes('@') && (clean.startsWith('+') || /^\d{10,12}$/.test(clean))) {
      field = 'phone';
      val = clean.startsWith('+') ? clean : '+91' + clean;
    } else if (!clean.includes('@')) {
      field = 'username';
    }

    return new Promise((resolve, reject) => {
      db.get(`SELECT id, resetOtp, resetOtpExpires FROM users WHERE ${field} = ?`, [val], async (err, user: any) => {
        if (err || !user) return reject(new Error('Invalid request or code.'));

        if (!user.resetOtp || user.resetOtp !== otp.trim()) {
          return reject(new Error('Incorrect verification code.'));
        }

        if (Date.now() > user.resetOtpExpires) {
          return reject(new Error('This verification code has expired. Please request a new code.'));
        }

        const salt = await bcrypt.genSalt(10);
        const passwordHash = await bcrypt.hash(newPassword, salt);

        db.run(
          `UPDATE users SET passwordHash = ?, resetOtp = NULL, resetOtpExpires = NULL WHERE id = ?`,
          [passwordHash, user.id],
          (updateErr) => {
            if (updateErr) return reject(new Error('Failed to update password.'));
            resolve();
          }
        );
      });
    });
  }
}
