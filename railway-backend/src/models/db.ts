import sqlite3 from 'sqlite3';
import path from 'path';
import fs from 'fs';

const dbDir = path.join(__dirname, '../../data');
if (!fs.existsSync(dbDir)) {
  fs.mkdirSync(dbDir, { recursive: true });
}

const dbPath = path.join(dbDir, 'smart_city.db');
const sqlite = sqlite3.verbose();

export const db = new sqlite.Database(dbPath, (err) => {
  if (err) {
    console.error('[Database]: Error opening database', err.message);
  } else {
    console.log('[Database]: Connected to SQLite database at', dbPath);
  }
});

db.serialize(() => {
  db.run(`
    CREATE TABLE IF NOT EXISTS users (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      fullName TEXT NOT NULL,
      username TEXT UNIQUE NOT NULL,
      email TEXT UNIQUE NOT NULL,
      phone TEXT UNIQUE NOT NULL,
      passwordHash TEXT NOT NULL,
      emailVerified INTEGER DEFAULT 0,
      phoneVerified INTEGER DEFAULT 0,
      emailOtp TEXT,
      emailOtpExpires INTEGER,
      phoneOtp TEXT,
      phoneOtpExpires INTEGER,
      resetOtp TEXT,
      resetOtpExpires INTEGER,
      createdAt DATETIME DEFAULT CURRENT_TIMESTAMP
    )
  `, (err) => {
    if (err) {
      console.error('[Database]: Error creating users table', err.message);
    } else {
      console.log('[Database]: Users table verified/created successfully');
    }
  });
});
