import { Router } from 'express';
import { railwayController } from '../controllers/railwayController';

const router = Router();

router.get('/live/:trainNumber', railwayController.getLiveStatus);
router.get('/pnr/:pnr', railwayController.getPnrStatus);
router.get('/availability', railwayController.getSeatAvailability);

export default router;
