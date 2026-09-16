package com.example.smartcityassistant.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PrimaryNavy = Color(0xFF0D2B4E)
private val ScreenBackground = Color(0xFFF5F5F5)

@Composable
fun VerificationScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onNavigateToEmailOtp: () -> Unit,
    onNavigateToMobileOtp: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = PrimaryNavy,
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
                Text("Verification", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Verify your account details",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryNavy
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Email", fontSize = 13.sp, color = Color.Gray)
                    Text(currentUser?.email ?: "user@example.com", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                    Spacer(modifier = Modifier.height(8.dp))

                    val emailVerified = currentUser?.emailVerified == true
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (emailVerified) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (emailVerified) Color(0xFF2E7D32) else Color(0xFFED6C02),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (emailVerified) "Verified" else "Not Verified",
                                color = if (emailVerified) Color(0xFF2E7D32) else Color(0xFFED6C02),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (!emailVerified) {
                            Button(
                                onClick = {
                                    viewModel.sendEmailOtp()
                                    onNavigateToEmailOtp()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text("Verify Email", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Mobile Number", fontSize = 13.sp, color = Color.Gray)
                    Text(currentUser?.phone ?: "+91 XXXXX XXXXX", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                    Spacer(modifier = Modifier.height(8.dp))

                    val phoneVerified = currentUser?.phoneVerified == true
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (phoneVerified) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (phoneVerified) Color(0xFF2E7D32) else Color(0xFFED6C02),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (phoneVerified) "Verified" else "Not Verified",
                                color = if (phoneVerified) Color(0xFF2E7D32) else Color(0xFFED6C02),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (!phoneVerified) {
                            Button(
                                onClick = {
                                    viewModel.sendPhoneOtp()
                                    onNavigateToMobileOtp()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text("Verify Mobile", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Account Security\nVerified contact details help secure your account and help you recover access.",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 13.sp,
                    color = PrimaryNavy
                )
            }
        }
    }
}
