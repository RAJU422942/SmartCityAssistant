package com.example.smartcityassistant.ui.government

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GovernmentScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF0D2B4E),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column {
                    Text("Government", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Government Services", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Government Services",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0D2B4E),
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            val services = listOf(
                GovServiceItem("My Documents", "Store and manage your important documents", Icons.Default.Description, "my_documents", Color(0xFF1E88E5)),
                GovServiceItem("Government Schemes", "Discover government schemes and programs", Icons.Default.HomeWork, "government_schemes", Color(0xFF2E7D32)),
                GovServiceItem("Welfare & Benefits", "Explore benefits and eligibility information", Icons.Default.CardGiftcard, "welfare_benefits", Color(0xFFF57C00)),
                GovServiceItem("Government Offices", "Find nearby government offices", Icons.Default.LocationCity, "government_offices", Color(0xFF9C27B0)),
                GovServiceItem("Online Services", "Access official government services", Icons.Default.Language, "online_services", Color(0xFF00ACC1)),
                GovServiceItem("Government Notices", "View verified government updates", Icons.Default.Campaign, "government_notices", Color(0xFFD32F2F)),
                GovServiceItem("Important Helplines", "Important government contact numbers", Icons.Default.PhoneInTalk, "government_helplines", Color(0xFF3949AB))
            )

            items(services.size) { index ->
                val service = services[index]
                GovServiceCard(service = service) {
                    onNavigate(service.route)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

data class GovServiceItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String,
    val tintColor: Color
)

@Composable
fun GovServiceCard(service: GovServiceItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = service.tintColor.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(service.icon, contentDescription = service.title, tint = service.tintColor, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(service.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(service.subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        }
    }
}
