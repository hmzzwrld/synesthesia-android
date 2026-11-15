package com.synesthesia.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synesthesia.mobile.ui.theme.SynesthesiaAtmosphere
import com.synesthesia.mobile.ui.theme.glassSurface

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }
            val textColor = if (isDarkTheme) Color(0xFFF0F0F8) else Color(0xFF141424)
            val mutedColor = if (isDarkTheme) Color(0xFF9595B2) else Color(0xFF5A5A78)

            MaterialTheme {
                SynesthesiaAtmosphere(isDarkTheme = isDarkTheme) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassSurface(
                                    shape = RoundedCornerShape(28.dp),
                                    isDarkTheme = isDarkTheme
                                )
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "SYNESTHESIA",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 3.sp,
                                color = textColor
                            )
                            Text(
                                text = "Liquid Glass Foundation Active",
                                fontSize = 14.sp,
                                color = mutedColor
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .glassSurface(
                                        shape = RoundedCornerShape(50),
                                        isDarkTheme = isDarkTheme
                                    )
                                    .clickable { isDarkTheme = !isDarkTheme }
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = if (isDarkTheme) "Switch to Bright Mode" else "Switch to Dark Mode",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
