import { Router } from 'express';
import { governmentController } from '../controllers/governmentController';

const router = Router();

router.get('/schemes', governmentController.getSchemes);
router.get('/benefits', governmentController.getBenefits);
router.get('/notices', governmentController.getNotices);
router.get('/helplines', governmentController.getHelplines);

export default router;
