import { GovScheme, WelfareBenefit, GovNotice, GovHelpline } from '../models/governmentModels';

export class GovernmentService {
  async getSchemes(): Promise<GovScheme[]> {
    return [
      { id: 's1', title: 'Pradhan Mantri Awas Yojana (PMAY)', description: 'Credit linked subsidy scheme for affordable housing for urban and rural poor.', category: 'Housing', eligibility: 'Families belonging to EWS, LIG, and MIG categories who do not own a pucca house.', officialSource: 'Ministry of Housing and Urban Affairs', officialUrl: 'https://pmaymis.gov.in' },
      { id: 's2', title: 'Ayushman Bharat PM-JAY', description: "World's largest health insurance scheme fully financed by government providing coverage up to ₹5 lakhs per family.", category: 'Health', eligibility: 'Families identified as deprived based on SECC 2011 database.', officialSource: 'National Health Authority', officialUrl: 'https://pmjay.gov.in' },
      { id: 's3', title: 'PM Kisan Samman Nidhi', description: 'Income support of ₹6,000 per year to all landholding farmer families.', category: 'Agriculture', eligibility: 'All landholding farmer families with cultivable land.', officialSource: 'Ministry of Agriculture & Farmers Welfare', officialUrl: 'https://pmkisan.gov.in' },
      { id: 's4', title: 'National Scholarship Portal (NSP)', description: 'Centralized portal for various central and state government scholarship schemes.', category: 'Education', eligibility: 'Students meeting merit and income criteria specified by respective ministries.', officialSource: 'Ministry of Electronics and Information Technology', officialUrl: 'https://scholarships.gov.in' }
    ];
  }

  async getBenefits(): Promise<WelfareBenefit[]> {
    return [
      { id: 'b1', name: 'Maternity Benefit (PMMVY)', description: 'Cash incentive of ₹5,000 for pregnant women and lactating mothers for first live child.', eligibility: 'Pregnant women and lactating mothers (19 years and above).', requiredDocuments: ['Aadhaar Card', 'MCP Card', 'Bank Passbook'], howToApply: 'Apply through Anganwadi centres or Health facilities.', officialSource: 'Ministry of Women and Child Development' },
      { id: 'b2', name: 'Old Age Pension (NSAP)', description: 'Monthly financial assistance to senior citizens aged 60 years and above living below poverty line.', eligibility: 'Senior citizens aged 60+ belonging to BPL households.', requiredDocuments: ['BPL Card / Certificate', 'Age Proof', 'Bank Account Details'], howToApply: 'Apply through District Social Welfare Office or online portal.', officialSource: 'Ministry of Rural Development' },
      { id: 'b3', name: 'Disability Pension Scheme', description: 'Monthly financial assistance for persons with severe disabilities.', eligibility: 'Individuals with verified benchmark disability (40% or more).', requiredDocuments: ['Disability Certificate (UDID)', 'Income Certificate', 'Aadhaar Card'], howToApply: 'Apply via state social welfare portal.', officialSource: 'Department of Empowerment of Persons with Disabilities' }
    ];
  }

  async getNotices(): Promise<GovNotice[]> {
    return [
      { id: 'n1', title: 'Digital Census & Civic Survey Update', summary: 'Ministry announces nationwide digital enumeration guidelines for municipal planning.', date: '2026-09-01', authority: 'Ministry of Home Affairs', sourceUrl: 'https://india.gov.in' },
      { id: 'n2', title: 'Revised National Air Quality Safety Guidelines', summary: 'New industrial and vehicular emissions standards effective nationwide.', date: '2026-08-15', authority: 'Central Pollution Control Board', sourceUrl: 'https://cpcb.nic.in' }
    ];
  }

  async getHelplines(): Promise<GovHelpline[]> {
    return [
      { id: 'h1', name: 'National Emergency Number', number: '112', purpose: 'Unified emergency response for police, fire, and health.', category: 'Emergency' },
      { id: 'h2', name: 'Disaster Management Helpline', number: '1078', purpose: 'National Disaster Management Authority (NDMA) support.', category: 'Emergency' },
      { id: 'h3', name: 'Women Helpline', number: '1091', purpose: '24x7 safety and distress assistance for women.', category: 'Women' },
      { id: 'h4', name: 'Child Helpline', number: '1098', purpose: 'Assistance for children in distress or need of care.', category: 'Child' },
      { id: 'h5', name: 'Cyber Crime Helpline', number: '1930', purpose: 'Report cyber financial fraud and cyber crimes immediately.', category: 'Cyber Crime' },
      { id: 'h6', name: 'Senior Citizen Helpline', number: '14567', purpose: 'Elderline national helpline for senior citizens.', category: 'Senior Citizens' }
    ];
  }
}

export const governmentService = new GovernmentService();
