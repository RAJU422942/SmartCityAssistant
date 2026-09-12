import { Router } from 'express';
import { railwayController } from '../controllers/railwayController';

const router = Router();

router.get('/live/:trainNumber', railwayController.getLiveStatus);
router.get('/pnr/:pnr', railwayController.getPnrStatus);
router.get('/availability', railwayController.getSeatAvailability);
router.get('/trains/:trainNumber', railwayController.getTrainInfo);
router.get('/trains/between/:from/:to', railwayController.getTrainsBetween);
router.get('/stations/search', railwayController.searchStations);
router.get('/trains/search', railwayController.searchTrains);
router.get('/aqi', railwayController.getAqi);

export default router;
