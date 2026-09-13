import { Router } from 'express';
import { cityAlertsController } from '../controllers/cityAlertsController';

const router = Router();

router.get('/', cityAlertsController.getAlerts);

export default router;
