import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import crypto from 'crypto';
import { db } from '../models/db';
import nodemailer from 'nodemailer';

const JWT_SECRET = process.env.JWT_SECRET || 'smart_city_assistant_super_secret_jwt_key_2025';
const TOKEN_EXPIRES_IN = '7d';

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
}

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
    throw new Error('Failed to send SMS verification code.');
  }
}

export class AuthService {
  static generateToken(user: { id: number; username: string; email: string; phone: string; tokenVersion?: number }) {
    return jwt.sign(
      { id: user.id, username: user.username, email: user.email, phone: user.phone, tokenVersion: user.tokenVersion || 0 },
      JWT_SECRET,
      { expiresIn: TOKEN_EXPIRES_IN }
    );
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

          const plainEmailOtp = crypto.randomInt(100000, 1000000).toString();
          const emailOtpHash = await bcrypt.hash(plainEmailOtp, 10);
          const emailOtpExpires = Date.now() + 10 * 60 * 1000;

          const plainPhoneOtp = crypto.randomInt(100000, 1000000).toString();
          const phoneOtpHash = await bcrypt.hash(plainPhoneOtp, 10);
          const phoneOtpExpires = Date.now() + 10 * 60 * 1000;

          db.run(
            `INSERT INTO users (fullName, username, email, phone, passwordHash, emailVerified, phoneVerified, emailOtp, emailOtpExpires, emailOtpAttempts, phoneOtp, phoneOtpExpires, phoneOtpAttempts, tokenVersion)
             VALUES (?, ?, ?, ?, ?, 0, 0, ?, ?, 0, ?, ?, 0, 0)`,
            [
              fullName.trim(),
              username.toLowerCase().trim(),
              email.toLowerCase().trim(),
              normalizedPhone,
              passwordHash,
              emailOtpHash,
              emailOtpExpires,
              phoneOtpHash,
              phoneOtpExpires,
            ],
            async function (insertErr) {
              if (insertErr) return reject(new Error('Failed to create user account.'));
              const userId = this.lastID;

              try {
                await sendEmailNotification(
                  email,
                  'Smart City Assistant - Email Verification Code',
                  `Hello ${fullName},\n\nYour email verification code is: ${plainEmailOtp}\n\nThis code expires in 10 minutes.\n\nSmart City Assistant Team`
                );
              } catch (_) {}

              try {
                await sendSmsNotification(
                  normalizedPhone,
                  `Smart City Assistant OTP: ${plainEmailOtp}. Verify your account securely.`
                );
              } catch (_) {}

              const userObj = {
                id: userId,
                fullName: fullName.trim(),
                username: username.toLowerCase().trim(),
                email: email.toLowerCase().trim(),
                phone: normalizedPhone,
                emailVerified: false,
                phoneVerified: false,
                tokenVersion: 0,
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

          const userObj = {
            id: user.id,
            fullName: user.fullName,
            username: user.username,
            email: user.email,
            phone: user.phone,
            emailVerified: user.emailVerified === 1,
            phoneVerified: user.phoneVerified === 1,
            tokenVersion: user.tokenVersion || 0,
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
        `SELECT id, fullName, username, email, phone, emailVerified, phoneVerified, tokenVersion, createdAt FROM users WHERE id = ?`,
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

  static async logoutUser(userId: number): Promise<void> {
    return new Promise((resolve, reject) => {
      db.run(`UPDATE users SET tokenVersion = tokenVersion + 1 WHERE id = ?`, [userId], (err) => {
        if (err) return reject(new Error('Logout failed.'));
        resolve();
      });
    });
  }

  static async sendEmailOtp(userId: number): Promise<void> {
    const plainOtp = crypto.randomInt(100000, 1000000).toString();
    const otpHash = await bcrypt.hash(plainOtp, 10);
    const expires = Date.now() + 10 * 60 * 1000;

    return new Promise((resolve, reject) => {
      db.get(`SELECT email, fullName, emailOtpExpires FROM users WHERE id = ?`, [userId], async (err, user: any) => {
        if (err || !user) return reject(new Error('User not found.'));

        if (user.emailOtpExpires && (user.emailOtpExpires - (10 * 60 * 1000) + 60 * 1000) > Date.now()) {
          return reject(new Error('Please wait 60 seconds before requesting another verification code.'));
        }

        db.run(`UPDATE users SET emailOtp = ?, emailOtpExpires = ?, emailOtpAttempts = 0 WHERE id = ?`, [otpHash, expires, userId], async (updateErr) => {
          if (updateErr) return reject(new Error('Failed to generate email OTP.'));

          try {
            await sendEmailNotification(
              user.email,
              'Smart City Assistant - Email Verification OTP',
              `Hello ${user.fullName},\n\nYour email verification code is: ${plainOtp}\n\nThis code expires in 10 minutes.\n\nSmart City Assistant`
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
      db.get(`SELECT emailOtp, emailOtpExpires, emailOtpAttempts FROM users WHERE id = ?`, [userId], async (err, user: any) => {
        if (err || !user) return reject(new Error('User not found.'));

        const attempts = (user.emailOtpAttempts || 0) + 1;
        if (attempts > 5) {
          db.run(`UPDATE users SET emailOtp = NULL, emailOtpExpires = NULL, emailOtpAttempts = 0 WHERE id = ?`, [userId], () => {});
          return reject(new Error('Too many failed verification attempts. Please request a new code.'));
        }

        if (!user.emailOtp) {
          return reject(new Error('No active verification code found. Please request a new code.'));
        }

        if (Date.now() > user.emailOtpExpires) {
          return reject(new Error('This verification code has expired. Please request a new code.'));
        }

        const isMatch = await bcrypt.compare(otp.trim(), user.emailOtp);
        if (!isMatch) {
          db.run(`UPDATE users SET emailOtpAttempts = ? WHERE id = ?`, [attempts, userId], () => {});
          return reject(new Error('Incorrect verification code.'));
        }

        db.run(`UPDATE users SET emailVerified = 1, emailOtp = NULL, emailOtpExpires = NULL, emailOtpAttempts = 0 WHERE id = ?`, [userId], (updateErr) => {
          if (updateErr) return reject(new Error('Failed to update verification status.'));
          resolve();
        });
      });
    });
  }

  static async sendPhoneOtp(userId: number): Promise<void> {
    const plainOtp = crypto.randomInt(100000, 1000000).toString();
    const otpHash = await bcrypt.hash(plainOtp, 10);
    const expires = Date.now() + 10 * 60 * 1000;

    return new Promise((resolve, reject) => {
      db.get(`SELECT phone, fullName, phoneOtpExpires FROM users WHERE id = ?`, [userId], async (err, user: any) => {
        if (err || !user) return reject(new Error('User not found.'));

        if (user.phoneOtpExpires && (user.phoneOtpExpires - (10 * 60 * 1000) + 60 * 1000) > Date.now()) {
          return reject(new Error('Please wait 60 seconds before requesting another verification code.'));
        }

        db.run(`UPDATE users SET phoneOtp = ?, phoneOtpExpires = ?, phoneOtpAttempts = 0 WHERE id = ?`, [otpHash, expires, userId], async (updateErr) => {
          if (updateErr) return reject(new Error('Failed to generate phone OTP.'));

          try {
            await sendSmsNotification(
              user.phone,
              `Smart City Assistant Mobile Verification OTP: ${plainOtp}. Valid for 10 minutes.`
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
      db.get(`SELECT phoneOtp, phoneOtpExpires, phoneOtpAttempts FROM users WHERE id = ?`, [userId], async (err, user: any) => {
        if (err || !user) return reject(new Error('User not found.'));

        const attempts = (user.phoneOtpAttempts || 0) + 1;
        if (attempts > 5) {
          db.run(`UPDATE users SET phoneOtp = NULL, phoneOtpExpires = NULL, phoneOtpAttempts = 0 WHERE id = ?`, [userId], () => {});
          return reject(new Error('Too many failed verification attempts. Please request a new code.'));
        }

        if (!user.phoneOtp) {
          return reject(new Error('No active verification code found. Please request a new code.'));
        }

        if (Date.now() > user.phoneOtpExpires) {
          return reject(new Error('This verification code has expired. Please request a new code.'));
        }

        const isMatch = await bcrypt.compare(otp.trim(), user.phoneOtp);
        if (!isMatch) {
          db.run(`UPDATE users SET phoneOtpAttempts = ? WHERE id = ?`, [attempts, userId], () => {});
          return reject(new Error('Incorrect verification code.'));
        }

        db.run(`UPDATE users SET phoneVerified = 1, phoneOtp = NULL, phoneOtpExpires = NULL, phoneOtpAttempts = 0 WHERE id = ?`, [userId], (updateErr) => {
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

        const plainResetOtp = crypto.randomInt(100000, 1000000).toString();
        const resetOtpHash = await bcrypt.hash(plainResetOtp, 10);
        const expires = Date.now() + 15 * 60 * 1000;

        db.run(`UPDATE users SET resetOtp = ?, resetOtpExpires = ?, resetOtpAttempts = 0 WHERE id = ?`, [resetOtpHash, expires, user.id], async (updateErr) => {
          if (updateErr) return resolve();

          try {
            await sendEmailNotification(
              user.email,
              'Smart City Assistant - Password Reset Code',
              `Hello ${user.fullName},\n\nYour password reset verification code is: ${plainResetOtp}\n\nValid for 15 minutes.`
            );
          } catch (_) {}
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
      db.get(`SELECT id, resetOtp, resetOtpExpires, resetOtpAttempts FROM users WHERE ${field} = ?`, [val], async (err, user: any) => {
        if (err || !user) return reject(new Error('Invalid request or code.'));

        const attempts = (user.resetOtpAttempts || 0) + 1;
        if (attempts > 5) {
          db.run(`UPDATE users SET resetOtp = NULL, resetOtpExpires = NULL, resetOtpAttempts = 0 WHERE id = ?`, [user.id], () => {});
          return reject(new Error('Too many failed verification attempts. Please request a new code.'));
        }

        if (!user.resetOtp) {
          return reject(new Error('Incorrect verification code.'));
        }

        if (Date.now() > user.resetOtpExpires) {
          return reject(new Error('This verification code has expired. Please request a new code.'));
        }

        const isMatch = await bcrypt.compare(otp.trim(), user.resetOtp);
        if (!isMatch) {
          db.run(`UPDATE users SET resetOtpAttempts = ? WHERE id = ?`, [attempts, user.id], () => {});
          return reject(new Error('Incorrect verification code.'));
        }

        const salt = await bcrypt.genSalt(10);
        const passwordHash = await bcrypt.hash(newPassword, salt);

        db.run(
          `UPDATE users SET passwordHash = ?, resetOtp = NULL, resetOtpExpires = NULL, resetOtpAttempts = 0, tokenVersion = tokenVersion + 1 WHERE id = ?`,
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
