import { Router } from 'express';
import { aiController } from '../controllers/aiController';

const router = Router();

router.post('/chat', aiController.chat);

export default router;
