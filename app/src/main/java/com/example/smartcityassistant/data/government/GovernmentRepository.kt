package com.example.smartcityassistant.data.government

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GovernmentRepository(private val context: Context) {
    private val api = GovernmentClient.service

    suspend fun getSchemes(): Result<List<GovScheme>> {
        return withContext(Dispatchers.IO) {
            try {
                Result.success(api.getSchemes())
            } catch (e: Exception) {
                Result.success(getFallbackSchemes())
            }
        }
    }

    suspend fun getBenefits(): Result<List<WelfareBenefit>> {
        return withContext(Dispatchers.IO) {
            try {
                Result.success(api.getBenefits())
            } catch (e: Exception) {
                Result.success(getFallbackBenefits())
            }
        }
    }

    suspend fun getNotices(): Result<List<GovNotice>> {
        return withContext(Dispatchers.IO) {
            try {
                Result.success(api.getNotices())
            } catch (e: Exception) {
                Result.success(getFallbackNotices())
            }
        }
    }

    suspend fun getHelplines(): Result<List<GovHelpline>> {
        return withContext(Dispatchers.IO) {
            try {
                Result.success(api.getHelplines())
            } catch (e: Exception) {
                Result.success(getFallbackHelplines())
            }
        }
    }

    fun getOfficialPortals(): List<OfficialPortal> {
        return listOf(
            OfficialPortal("p16", "Digital Police Citizen Portal", "Access nationwide police services, complaints tracking, and verification.", "Police & Complaints", null, "https://www.digitalpolice.gov.in", "Ministry of Home Affairs", "VERIFIED", "2026-09", listOf("police", "complaint", "fir", "verification", "police station")),
            OfficialPortal("p17", "Police Citizen Services (State Portals)", "Access state-specific police services for citizen grievances and character verification.", "Police & Complaints", null, "https://www.digitalpolice.gov.in", "Ministry of Home Affairs", "VERIFIED", "2026-09", listOf("police", "citizen services", "tenant verification", "clearance")),
            OfficialPortal("p1", "Cyber Crime Reporting Portal", "Report cybercrime and financial cyber fraud through official channels.", "Cyber Crime", null, "https://cybercrime.gov.in", "Ministry of Home Affairs", "VERIFIED", "2026-09", listOf("cyber", "fraud", "cybercrime", "online fraud", "hack")),
            OfficialPortal("p2", "CEIR - Block Lost Mobile", "Block a lost or stolen mobile device through the official CEIR service.", "Lost & Stolen", null, "https://ceir.sancharsaathi.gov.in", "Department of Telecommunications", "VERIFIED", "2026-09", listOf("lost mobile", "stolen phone", "ceir", "block phone", "imei")),
            OfficialPortal("p3", "Government Grievance (CPGRAMS)", "Submit a grievance to the concerned government authority through the official grievance system.", "Government Grievances", null, "https://pgportal.gov.in", "Department of Administrative Reforms", "VERIFIED", "2026-09", listOf("grievance", "complaint", "cpgrams", "appeal")),
            OfficialPortal("p4", "National Consumer Helpline (E-Daakhil)", "Submit a consumer complaint through the official government consumer services platform.", "Consumer Services", null, "https://edakhil.nic.in", "Ministry of Consumer Affairs", "VERIFIED", "2026-09", listOf("consumer", "complaint", "fraud", "edakhil")),
            OfficialPortal("p5", "National Results Portal", "Check results from participating examination bodies through the official national results gateway.", "Student Services", null, "https://results.gov.in", "National Informatics Centre", "VERIFIED", "2026-09", listOf("result", "exam", "board", "university", "cbse")),
            OfficialPortal("p6", "National Scholarship Portal (NSP)", "Apply for and manage eligible central and state government scholarship services.", "Scholarships", null, "https://scholarships.gov.in", "Ministry of Electronics and IT", "VERIFIED", "2026-09", listOf("scholarship", "student", "financial aid", "nsp")),
            OfficialPortal("p7", "DigiLocker", "Access your digital government and academic documents through official channels.", "Identity & Documents", null, "https://www.digilocker.gov.in", "Ministry of Electronics and IT", "VERIFIED", "2026-09", listOf("digilocker", "document", "certificates", "aadhaar")),
            OfficialPortal("p8", "Passport Seva", "Apply for new passport, renewal, and police clearance certificates.", "Identity & Documents", null, "https://www.passportindia.gov.in", "Ministry of External Affairs", "VERIFIED", "2026-09", listOf("passport", "visa", "travel", "police clearance")),
            OfficialPortal("p9", "UIDAI (Aadhaar)", "Manage Aadhaar card details, address update, and verification.", "Identity & Documents", null, "https://uidai.gov.in", "Unique Identification Authority of India", "VERIFIED", "2026-09", listOf("aadhaar", "uidai", "identity", "biometric")),
            OfficialPortal("p10", "Income Tax e-Filing Portal", "File income tax returns, verify tax status, and check refunds online.", "Finance & Tax", null, "https://www.incometax.gov.in", "Income Tax Department, India", "VERIFIED", "2026-09", listOf("tax", "income tax", "itrv", "refund", "pan")),
            OfficialPortal("p11", "Sarathi Parivahan (Driving Licence)", "Apply for learner licence, driving licence, and renewal services.", "Transport", null, "https://sarathi.parivahan.gov.in", "Ministry of Road Transport and Highways", "VERIFIED", "2026-09", listOf("driving licence", "dl", "learner licence", "sarathi")),
            OfficialPortal("p12", "Vahan (Vehicle Registration)", "Access vehicle registration details, transfer of ownership, and fitness.", "Transport", null, "https://vahan.parivahan.gov.in", "Ministry of Road Transport and Highways", "VERIFIED", "2026-09", listOf("vehicle", "rc", "registration", "vahan", "challan")),
            OfficialPortal("p13", "National Career Service (NCS)", "Government portal for job seekers and employers across India.", "Jobs & Exams", null, "https://www.ncs.gov.in", "Ministry of Labour and Employment", "VERIFIED", "2026-09", listOf("jobs", "career", "employment", "ncs")),
            OfficialPortal("p14", "EPFO Member Portal", "Check provident fund balance, passbook, and pension services.", "Finance & Tax", null, "https://www.epfindia.gov.in", "Employees' Provident Fund Organisation", "VERIFIED", "2026-09", listOf("epf", "pf", "pension", "provident fund")),
            OfficialPortal("p15", "Ayushman Bharat PM-JAY", "Access national health insurance scheme details and hospital empanelment.", "Health", null, "https://pmjay.gov.in", "National Health Authority", "VERIFIED", "2026-09", listOf("health", "insurance", "ayushman", "hospital"))
        )
    }

    private fun getFallbackSchemes(): List<GovScheme> {
        return listOf(
            GovScheme("s1", "Pradhan Mantri Awas Yojana (PMAY)", "Credit linked subsidy scheme for affordable housing for urban and rural poor.", "Housing", "Families belonging to EWS, LIG, and MIG categories who do not own a pucca house.", "Ministry of Housing and Urban Affairs", "https://pmaymis.gov.in"),
            GovScheme("s2", "Ayushman Bharat PM-JAY", "World's largest health insurance scheme fully financed by government providing coverage up to ₹5 lakhs per family.", "Health", "Families identified as deprived based on SECC 2011 database.", "National Health Authority", "https://pmjay.gov.in"),
            GovScheme("s3", "PM Kisan Samman Nidhi", "Income support of ₹6,000 per year to all landholding farmer families.", "Agriculture", "All landholding farmer families with cultivable land.", "Ministry of Agriculture & Farmers Welfare", "https://pmkisan.gov.in"),
            GovScheme("s4", "National Scholarship Portal (NSP)", "Centralized portal for various central and state government scholarship schemes.", "Education", "Students meeting merit and income criteria specified by respective ministries.", "Ministry of Electronics and Information Technology", "https://scholarships.gov.in")
        )
    }

    private fun getFallbackBenefits(): List<WelfareBenefit> {
        return listOf(
            WelfareBenefit("b1", "Maternity Benefit (PMMVY)", "Cash incentive of ₹5,000 for pregnant women and lactating mothers for first live child.", "Pregnant women and lactating mothers (19 years and above).", listOf("Aadhaar Card", "MCP Card", "Bank Passbook"), "Apply through Anganwadi centres or Health facilities.", "Ministry of Women and Child Development"),
            WelfareBenefit("b2", "Old Age Pension (NSAP)", "Monthly financial assistance to senior citizens aged 60 years and above living below poverty line.", "Senior citizens aged 60+ belonging to BPL households.", listOf("BPL Card / Certificate", "Age Proof", "Bank Account Details"), "Apply through District Social Welfare Office or online portal.", "Ministry of Rural Development"),
            WelfareBenefit("b3", "Disability Pension Scheme", "Monthly financial assistance for persons with severe disabilities.", "Individuals with verified benchmark disability (40% or more).", listOf("Disability Certificate (UDID)", "Income Certificate", "Aadhaar Card"), "Apply via state social welfare portal.", "Department of Empowerment of Persons with Disabilities")
        )
    }

    private fun getFallbackNotices(): List<GovNotice> {
        return listOf(
            GovNotice("n1", "Digital Census & Civic Survey Update", "Ministry announces nationwide digital enumeration guidelines for municipal planning.", "2026-09-01", "Ministry of Home Affairs", "https://india.gov.in"),
            GovNotice("n2", "Revised National Air Quality Safety Guidelines", "New industrial and vehicular emissions standards effective nationwide.", "2026-08-15", "Central Pollution Control Board", "https://cpcb.nic.in")
        )
    }

    private fun getFallbackHelplines(): List<GovHelpline> {
        return listOf(
            GovHelpline("h1", "National Emergency Number", "112", "Unified emergency response for police, fire, and health.", "Emergency"),
            GovHelpline("h2", "Disaster Management Helpline", "1078", "National Disaster Management Authority (NDMA) support.", "Emergency"),
            GovHelpline("h3", "Women Helpline", "1091", "24x7 safety and distress assistance for women.", "Women"),
            GovHelpline("h4", "Child Helpline", "1098", "Assistance for children in distress or need of care.", "Child"),
            GovHelpline("h5", "Cyber Crime Helpline", "1930", "Report cyber financial fraud and cyber crimes immediately.", "Cyber Crime"),
            GovHelpline("h6", "Senior Citizen Helpline", "14567", "Elderline national helpline for senior citizens.", "Senior Citizens")
        )
    }
}
