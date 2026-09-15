package com.example.smartcityassistant.data.notices

class GovernmentNoticeRepository {
    fun getNotices(): List<GovernmentNotice> {
        return listOf(
            GovernmentNotice(
                id = "n1",
                title = "Digital Census & Civic Survey Update",
                category = NoticeCategory.CIVIC,
                date = "2026-09-01",
                shortDescription = "Ministry announces nationwide digital enumeration guidelines for municipal planning.",
                fullDescription = "The Ministry of Home Affairs has released comprehensive guidelines for conducting the upcoming digital census and civic survey. Municipal corporations and local authorities are instructed to update urban grid maps and digital enumeration blocks to facilitate efficient municipal planning and census data collection.",
                authority = "Ministry of Home Affairs",
                officialUrl = "https://india.gov.in",
                priority = NoticePriority.IMPORTANT,
                keywords = listOf("census", "civic survey", "digital enumeration", "municipal planning", "mha"),
                iconType = NoticeIconType.CIVIC
            ),
            GovernmentNotice(
                id = "n2",
                title = "Revised National Air Quality Safety Guidelines",
                category = NoticeCategory.ENVIRONMENT,
                date = "2026-08-15",
                shortDescription = "New industrial and vehicular emissions standards effective nationwide.",
                fullDescription = "The Central Pollution Control Board (CPCB) has enforced revised national air quality safety guidelines. Industrial units and commercial transport fleets must adhere to stricter particulate matter emission ceilings and real-time sensor monitoring protocols.",
                authority = "Central Pollution Control Board (CPCB)",
                officialUrl = "https://cpcb.nic.in",
                priority = NoticePriority.IMPORTANT,
                keywords = listOf("air quality", "pollution control", "emissions", "cpcb", "environment", "aqi"),
                iconType = NoticeIconType.ENVIRONMENT
            ),
            GovernmentNotice(
                id = "n3",
                title = "Monsoon Season Weather & Rainfall Advisory",
                category = NoticeCategory.WEATHER,
                date = "2026-09-10",
                shortDescription = "IMD issues heavy rainfall and flash flood warnings for coastal and low-lying regions.",
                fullDescription = "The India Meteorological Department (IMD) has issued a weather advisory warning of heavy to very heavy rainfall spells across multiple states. Fishermen and coastal residents are advised to suspend maritime activities until oceanic conditions stabilize.",
                authority = "India Meteorological Department (IMD)",
                officialUrl = "https://mausam.imd.gov.in",
                priority = NoticePriority.URGENT,
                keywords = listOf("weather", "rainfall", "imd", "monsoon", "flood warning", "storm advisory"),
                iconType = NoticeIconType.WEATHER
            ),
            GovernmentNotice(
                id = "n4",
                title = "National Disaster Preparedness & Mock Drill Directive",
                category = NoticeCategory.DISASTER,
                date = "2026-08-28",
                shortDescription = "NDMA directs district administrations to conduct seismic safety mock drills.",
                fullDescription = "The National Disaster Management Authority (NDMA) has issued a directive requiring all district disaster management cells to organize coordinated seismic safety mock drills across schools, hospitals, and high-rise commercial complexes.",
                authority = "National Disaster Management Authority (NDMA)",
                officialUrl = "https://ndma.gov.in",
                priority = NoticePriority.NORMAL,
                keywords = listOf("disaster management", "ndma", "mock drill", "seismic safety", "emergency preparedness"),
                iconType = NoticeIconType.DISASTER
            ),
            GovernmentNotice(
                id = "n5",
                title = "Indian Railways Festive Season Travel & Safety Advisory",
                category = NoticeCategory.TRANSPORT,
                date = "2026-09-05",
                shortDescription = "Railways announce special train services and enhanced platform security measures.",
                fullDescription = "The Ministry of Railways has announced special unreserved festival trains and strict anti-touting security checks at major railway junctions. Passengers are requested to book tickets in advance via IRCTC and avoid carrying unauthorized inflammable materials.",
                authority = "Ministry of Railways (Indian Railways)",
                officialUrl = "https://indianrailways.gov.in",
                priority = NoticePriority.NORMAL,
                keywords = listOf("railways", "trains", "travel advisory", "irctc", "festive specials", "station safety"),
                iconType = NoticeIconType.TRANSPORT
            ),
            GovernmentNotice(
                id = "n6",
                title = "National Consumer Helpline E-Daakhil Portal Upgrade",
                category = NoticeCategory.CONSUMER,
                date = "2026-08-20",
                shortDescription = "Consumer Affairs ministry integrates digital payment gateways for grievance filing.",
                fullDescription = "The Department of Consumer Affairs has upgraded the E-Daakhil consumer grievance portal with streamlined digital payment integration and automated hearing status tracking, making it easier for citizens to file consumer complaints online.",
                authority = "Department of Consumer Affairs",
                officialUrl = "https://consumerhelpline.gov.in",
                priority = NoticePriority.NORMAL,
                keywords = listOf("consumer", "grievance", "edakhil", "consumer court", "complaint filing"),
                iconType = NoticeIconType.CONSUMER
            ),
            GovernmentNotice(
                id = "n7",
                title = "CERT-In Critical Cybersecurity Advisory",
                category = NoticeCategory.CYBER_SAFETY,
                date = "2026-09-12",
                shortDescription = "Advisory issued for zero-day vulnerabilities in popular operating systems and browsers.",
                fullDescription = "The Indian Computer Emergency Response Team (CERT-In) has issued a critical advisory highlighting severe vulnerabilities in multiple operating systems. System administrators and individual users are urged to apply patch updates immediately.",
                authority = "CERT-In, Ministry of Electronics and IT",
                officialUrl = "https://www.cert-in.org.in",
                priority = NoticePriority.URGENT,
                keywords = listOf("cybersecurity", "cert-in", "vulnerability", "patch update", "cyber safety", "malware"),
                iconType = NoticeIconType.CYBER_SAFETY
            ),
            GovernmentNotice(
                id = "n8",
                title = "National Health Mission Vaccination & Immunization Update",
                category = NoticeCategory.HEALTH,
                date = "2026-09-02",
                shortDescription = "Ministry of Health launches updated universal immunization schedule guidelines.",
                fullDescription = "The Ministry of Health and Family Welfare has updated guidelines for the Universal Immunization Programme. District health officers are directed to ensure 100% vaccine cold-chain compliance and outreach coverage in remote districts.",
                authority = "Ministry of Health and Family Welfare",
                officialUrl = "https://www.mohfw.gov.in",
                priority = NoticePriority.NORMAL,
                keywords = listOf("health", "immunization", "vaccination", "mohfw", "national health mission"),
                iconType = NoticeIconType.HEALTH
            ),
            GovernmentNotice(
                id = "n9",
                title = "UGC Guidelines on Higher Education Examinations",
                category = NoticeCategory.EDUCATION,
                date = "2026-08-10",
                shortDescription = "University Grants Commission issues standardized academic calendar frameworks.",
                fullDescription = "The University Grants Commission (UGC) has issued structured guidelines for university academic calendars, semester credit frameworks, and transparent digital evaluation procedures across all recognized higher education institutions.",
                authority = "University Grants Commission (UGC)",
                officialUrl = "https://www.ugc.ac.in",
                priority = NoticePriority.NORMAL,
                keywords = listOf("ugc", "education", "university", "academic calendar", "higher education"),
                iconType = NoticeIconType.EDUCATION
            ),
            GovernmentNotice(
                id = "n10",
                title = "UIDAI Aadhaar Biometric Security Guidelines",
                category = NoticeCategory.CITIZEN_SERVICES,
                date = "2026-09-08",
                shortDescription = "UIDAI releases updated instructions for secure biometric authentication lockers.",
                fullDescription = "The Unique Identification Authority of India (UIDAI) has released updated technical guidelines for authorized enrollment and update centers to ensure maximum biometric encryption safety and prevent unauthorized data retention.",
                authority = "Unique Identification Authority of India (UIDAI)",
                officialUrl = "https://uidai.gov.in",
                priority = NoticePriority.IMPORTANT,
                keywords = listOf("aadhaar", "uidai", "biometric security", "identity", "citizen services"),
                iconType = NoticeIconType.CITIZEN_SERVICES
            ),
            GovernmentNotice(
                id = "n11",
                title = "PM-KISAN e-KYC and Land Seeding Update",
                category = NoticeCategory.AGRICULTURE,
                date = "2026-09-05",
                shortDescription = "Ministry of Agriculture issues guidelines for mandatory e-KYC and land seeding.",
                fullDescription = "The Ministry of Agriculture and Farmers Welfare has reiterated that completion of e-KYC and land seeding is mandatory for eligible farmer families to receive subsequent installments under the PM-KISAN scheme. Farmers can complete e-KYC via OTP authentication on the PM-KISAN portal or biometric verification at CSCs.",
                authority = "Ministry of Agriculture and Farmers Welfare",
                officialUrl = "https://pmkisan.gov.in",
                priority = NoticePriority.IMPORTANT,
                keywords = listOf("pm-kisan", "agriculture", "e-kyc", "farmer", "land seeding", "subsidy"),
                iconType = NoticeIconType.AGRICULTURE
            )
        )
    }
}
