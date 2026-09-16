import { Response } from 'express';
import { AuthenticatedRequest } from '../middleware/authMiddleware';
import { AuthService } from '../services/authService';

export const register = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const { fullName, username, email, phone, password, confirmPassword } = req.body;

    if (!fullName || !username || !email || !phone || !password || !confirmPassword) {
      res.status(400).json({ status: 'ERROR', message: 'All fields are required.' });
      return;
    }

    if (password !== confirmPassword) {
      res.status(400).json({ status: 'ERROR', message: 'Passwords do not match.' });
      return;
    }

    if (password.length < 6) {
      res.status(400).json({ status: 'ERROR', message: 'Password must be at least 6 characters long.' });
      return;
    }

    const result = await AuthService.register({ fullName, username, email, phone, password });
    res.status(201).json({ status: 'SUCCESS', ...result });
  } catch (error: any) {
    res.status(400).json({ status: 'ERROR', message: error.message || 'Registration failed.' });
  }
};

export const login = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const { identifier, password } = req.body;

    if (!identifier || !password) {
      res.status(400).json({ status: 'ERROR', message: 'Identifier and password are required.' });
      return;
    }

    const result = await AuthService.login(identifier, password);
    res.json({ status: 'SUCCESS', ...result });
  } catch (error: any) {
    res.status(401).json({ status: 'ERROR', message: error.message || 'Incorrect username, email, phone number or password.' });
  }
};

export const logout = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  // Stateless JWT logout is handled client-side by clearing token, but endpoint confirms success
  res.json({ status: 'SUCCESS', message: 'Logged out successfully.' });
};

export const getMe = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    if (!req.user || !req.user.id) {
      res.status(401).json({ status: 'ERROR', message: 'Unauthorized.' });
      return;
    }
    const user = await AuthService.getUserById(req.user.id);
    res.json({ status: 'SUCCESS', user });
  } catch (error: any) {
    res.status(404).json({ status: 'ERROR', message: error.message || 'User not found.' });
  }
};

export const sendEmailOtpController = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    if (!req.user || !req.user.id) {
      res.status(401).json({ status: 'ERROR', message: 'Unauthorized.' });
      return;
    }
    await AuthService.sendEmailOtp(req.user.id);
    res.json({ status: 'SUCCESS', message: 'Email verification code sent successfully.' });
  } catch (error: any) {
    res.status(400).json({ status: 'ERROR', message: error.message || 'Failed to send email OTP.' });
  }
};

export const verifyEmailController = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    if (!req.user || !req.user.id) {
      res.status(401).json({ status: 'ERROR', message: 'Unauthorized.' });
      return;
    }
    const { otp } = req.body;
    if (!otp) {
      res.status(400).json({ status: 'ERROR', message: 'Verification code is required.' });
      return;
    }
    await AuthService.verifyEmailOtp(req.user.id, otp);
    res.json({ status: 'SUCCESS', message: 'Email verified successfully.' });
  } catch (error: any) {
    res.status(400).json({ status: 'ERROR', message: error.message || 'Verification failed.' });
  }
};

export const sendPhoneOtpController = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    if (!req.user || !req.user.id) {
      res.status(401).json({ status: 'ERROR', message: 'Unauthorized.' });
      return;
    }
    await AuthService.sendPhoneOtp(req.user.id);
    res.json({ status: 'SUCCESS', message: 'Mobile verification code sent successfully.' });
  } catch (error: any) {
    res.status(400).json({ status: 'ERROR', message: error.message || 'Failed to send mobile OTP.' });
  }
};

export const verifyPhoneController = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    if (!req.user || !req.user.id) {
      res.status(401).json({ status: 'ERROR', message: 'Unauthorized.' });
      return;
    }
    const { otp } = req.body;
    if (!otp) {
      res.status(400).json({ status: 'ERROR', message: 'Verification code is required.' });
      return;
    }
    await AuthService.verifyPhoneOtp(req.user.id, otp);
    res.json({ status: 'SUCCESS', message: 'Mobile number verified successfully.' });
  } catch (error: any) {
    res.status(400).json({ status: 'ERROR', message: error.message || 'Verification failed.' });
  }
};

export const forgotPasswordController = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const { identifier } = req.body;
    if (!identifier) {
      res.status(400).json({ status: 'ERROR', message: 'Identifier (email, username, or phone) is required.' });
      return;
    }
    await AuthService.forgotPassword(identifier);
    res.json({ status: 'SUCCESS', message: 'If the account exists, a password reset code has been sent.' });
  } catch (error: any) {
    res.json({ status: 'SUCCESS', message: 'If the account exists, a password reset code has been sent.' });
  }
};

export const resetPasswordController = async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  try {
    const { identifier, otp, newPassword, confirmPassword } = req.body;
    if (!identifier || !otp || !newPassword || !confirmPassword) {
      res.status(400).json({ status: 'ERROR', message: 'All fields are required.' });
      return;
    }
    if (newPassword !== confirmPassword) {
      res.status(400).json({ status: 'ERROR', message: 'Passwords do not match.' });
      return;
    }
    if (newPassword.length < 6) {
      res.status(400).json({ status: 'ERROR', message: 'Password must be at least 6 characters long.' });
      return;
    }
    await AuthService.resetPassword(identifier, otp, newPassword);
    res.json({ status: 'SUCCESS', message: 'Password updated successfully.' });
  } catch (error: any) {
    res.status(400).json({ status: 'ERROR', message: error.message || 'Password reset failed.' });
  }
};
