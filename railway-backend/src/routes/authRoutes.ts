import { Router } from 'express';
import {
  register,
  login,
  logout,
  getMe,
  sendEmailOtpController,
  verifyEmailController,
  sendPhoneOtpController,
  verifyPhoneController,
  forgotPasswordController,
  resetPasswordController,
} from '../controllers/authController';
import { authenticateToken } from '../middleware/authMiddleware';

const router = Router();

router.post('/register', register);
router.post('/login', login);
router.post('/logout', authenticateToken, logout);
router.get('/me', authenticateToken, getMe);

router.post('/forgot-password', forgotPasswordController);
router.post('/reset-password', resetPasswordController);

router.post('/send-email-otp', authenticateToken, sendEmailOtpController);
router.post('/verify-email', authenticateToken, verifyEmailController);

router.post('/send-phone-otp', authenticateToken, sendPhoneOtpController);
router.post('/verify-phone', authenticateToken, verifyPhoneController);

// Aliases matching prompt requirements
router.post('/resend-email-otp', authenticateToken, sendEmailOtpController);

export default router;
