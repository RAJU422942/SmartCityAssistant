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

private val PrimaryNavy = Color(0xFF0D2B4E)
private val ScreenBackground = Color(0xFFF5F5F5)

@Composable
fun EmailOtpScreen(
    viewModel: AuthViewModel,
    onVerified: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var otp by remember { mutableStateOf("") }

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
                Text("Email Verification", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
                        text = "We sent a verification code to your registered email.",
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
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
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Verify Email", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    TextButton(
                        onClick = { viewModel.sendEmailOtp() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Resend Code", color = PrimaryNavy, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
