package com.synesthesia.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synesthesia.mobile.ui.theme.glassSurface

@Composable
fun SynesthesiaExtendedPlayer(
    currentTitle: String,
    currentArtist: String,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onCloseClick: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor = if (isDarkTheme) Color(0xFFF0F0F8) else Color(0xFF141424)
    val mutedColor = if (isDarkTheme) Color(0xFF9595B2) else Color(0xFF5A5A78)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .systemBarsPadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .glassSurface(
                    shape = RoundedCornerShape(36.dp),
                    isDarkTheme = isDarkTheme
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar (Close & Options)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(textColor.copy(alpha = 0.1f))
                        .clickable { onCloseClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "NOW PLAYING",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = mutedColor
                )

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(textColor.copy(alpha = 0.1f))
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Options",
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Large Immersive Album Art Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .glassSurface(
                        shape = RoundedCornerShape(24.dp),
                        isDarkTheme = isDarkTheme
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = textColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(80.dp)
                )
            }

            // Track Info & Metadata
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = currentTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentArtist,
                    fontSize = 15.sp,
                    color = mutedColor,
                    maxLines = 1
                )
            }

            // Scrubbing Progress Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = 0.25f,
                    onValueChange = {},
                    colors = SliderDefaults.colors(
                        thumbColor = textColor,
                        activeTrackColor = Color(0xFFFF3D8B),
                        inactiveTrackColor = textColor.copy(alpha = 0.15f)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "0:19", fontSize = 11.sp, color = mutedColor)
                    Text(text = "-2:39", fontSize = 11.sp, color = mutedColor)
                }
            }

            // Playback Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}) {
                    Icon(Icons.Rounded.Repeat, contentDescription = "Loop", tint = textColor.copy(alpha = 0.7f))
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", tint = textColor, modifier = Modifier.size(32.dp))
                }
                
                // Main Play/Pause Button
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(textColor)
                        .clickable { onPlayPauseClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = if (isDarkTheme) Color(0xFF05050D) else Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(onClick = {}) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = textColor, modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Rounded.Share, contentDescription = "Share", tint = textColor.copy(alpha = 0.7f))
                }
            }
        }
    }
}
