package com.example.smartcityassistant.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val PrimaryNavy = Color(0xFF0D2B4E)
private val ScreenBackground = Color(0xFFF5F5F5)

@Composable
fun EmailOtpScreen(
    viewModel: AuthViewModel,
    onVerified: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    var otp by remember { mutableStateOf("") }

    var countdown by remember { mutableStateOf(45) }
    var canResend by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = countdown) {
        if (countdown > 0) {
            delay(1000L)
            countdown -= 1
        } else {
            canResend = true
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.SuccessMessage && (uiState as AuthUiState.SuccessMessage).message.contains("verified", true)) {
            onVerified()
        }
    }

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
                Text("Verify Email", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "We sent a verification code to:",
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = currentUser?.email ?: "user@example.com",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNavy
                    )

                    OutlinedTextField(
                        value = otp,
                        onValueChange = { otp = it },
                        label = { Text("Enter 6-digit verification code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (uiState is AuthUiState.Error) {
                        Text(
                            text = (uiState as AuthUiState.Error).message,
                            color = Color(0xFFD32F2F),
                            fontSize = 13.sp
                        )
                    }
                    if (uiState is AuthUiState.SuccessMessage) {
                        Text(
                            text = (uiState as AuthUiState.SuccessMessage).message,
                            color = Color(0xFF2E7D32),
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.verifyEmail(otp)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                        shape = RoundedCornerShape(12.dp),
                        enabled = uiState !is AuthUiState.Loading
                    ) {
                        if (uiState is AuthUiState.Loading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Verify", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (canResend) {
                            TextButton(
                                onClick = {
                                    viewModel.sendEmailOtp()
                                    countdown = 45
                                    canResend = false
                                }
                            ) {
                                Text("Resend Code", color = PrimaryNavy, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text(
                                text = "Resend available in ${countdown}s",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
