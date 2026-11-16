package com.synesthesia.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.synesthesia.mobile.ui.components.DynamicIslandMiniPlayer
import com.synesthesia.mobile.ui.components.Screen
import com.synesthesia.mobile.ui.components.SynesthesiaBottomNavBar
import com.synesthesia.mobile.ui.theme.SynesthesiaAtmosphere
import com.synesthesia.mobile.ui.theme.glassSurface

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }
            var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
            
            var isPlaying by remember { mutableStateOf(false) }
            val currentTitle by remember { mutableStateOf("Slime You Out [Demo]") }
            val currentArtist by remember { mutableStateOf("RIJELL") }

            val textColor = if (isDarkTheme) Color(0xFFF0F0F8) else Color(0xFF141424)
            val mutedColor = if (isDarkTheme) Color(0xFF9595B2) else Color(0xFF5A5A78)

            MaterialTheme {
                SynesthesiaAtmosphere(isDarkTheme = isDarkTheme) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                    ) {
                        // Main Content Area
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                                .padding(top = 2.dp, bottom = 135.dp), // Tucked right up under the status bar
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(6.dp) // Minimized gap so the card stretches up high
                        ) {
                            // Header Title
                            Text(
                                text = "SYNESTHESIA",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 3.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp)
                            )

                            // Taller Full-Screen Active Tab Container stretching upward
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .glassSurface(
                                        shape = RoundedCornerShape(28.dp),
                                        isDarkTheme = isDarkTheme
                                    )
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Active Tab: ${currentScreen.title}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textColor
                                    )
                                    Text(
                                        text = "UI Shell & Navigation Active",
                                        fontSize = 13.sp,
                                        color = mutedColor
                                    )
                                }
                            }
                        }

                        // Bottom Floating Elements (Mini-Player + Nav)
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                        ) {
                            DynamicIslandMiniPlayer(
                                currentTitle = currentTitle,
                                currentArtist = currentArtist,
                                isPlaying = isPlaying,
                                onPlayPauseClick = { isPlaying = !isPlaying },
                                isDarkTheme = isDarkTheme
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))

                            SynesthesiaBottomNavBar(
                                selectedScreen = currentScreen,
                                onScreenSelected = { screen -> currentScreen = screen },
                                isDarkTheme = isDarkTheme
                            )
                        }
                    }
                }
            }
        }
    }
}
