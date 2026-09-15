package com.example.smartcityassistant.data.emergency

class EmergencyServiceRepository {
    fun getEmergencyServices(): List<EmergencyService> {
        return listOf(
            EmergencyService(
                id = "s_112",
                title = "National Emergency Number",
                subtitle = "Unified response for police, fire, and health",
                phoneNumber = "112",
                category = "General",
                sourceName = "Government of India",
                isPrimary = true
            ),
            EmergencyService(
                id = "s_police",
                title = "Police",
                subtitle = "Immediate police assistance and crime reporting",
                phoneNumber = "100",
                category = "Safety",
                sourceName = "Ministry of Home Affairs"
            ),
            EmergencyService(
                id = "s_ambulance",
                title = "Ambulance",
                subtitle = "Emergency medical health transport",
                phoneNumber = "102",
                category = "Medical",
                sourceName = "Ministry of Health and Family Welfare"
            ),
            EmergencyService(
                id = "s_fire",
                title = "Fire & Rescue",
                subtitle = "Fire brigade and rescue operations",
                phoneNumber = "101",
                category = "Rescue",
                sourceName = "National Disaster Management Authority"
            ),
            EmergencyService(
                id = "s_women",
                title = "Women Support",
                subtitle = "24x7 women distress and safety assistance",
                phoneNumber = "181",
                category = "Safety",
                sourceName = "Ministry of Women and Child Development"
            ),
            EmergencyService(
                id = "s_child",
                title = "Child Helpline",
                subtitle = "Emergency protection and care for children",
                phoneNumber = "1098",
                category = "Safety",
                sourceName = "Ministry of Women and Child Development"
            ),
            EmergencyService(
                id = "s_cyber",
                title = "Cyber Crime",
                subtitle = "Report cyber financial fraud and cybercrime",
                phoneNumber = "1930",
                category = "Cyber",
                sourceName = "Indian Cyber Crime Coordination Centre (I4C)",
                websiteUrl = "https://cybercrime.gov.in"
            )
        )
    }
}
