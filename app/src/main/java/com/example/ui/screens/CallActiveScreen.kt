package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.WhatsAppViewModel

@Composable
fun CallActiveScreen(
    callState: WhatsAppViewModel.CallState,
    onAnswer: () -> Unit,
    onReject: () -> Unit,
    onHangUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pulse animation for ringing
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }
    var isVideoOn by remember { mutableStateOf(callState.isVideo) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B141A)), // Deep WhatsApp Dark
        contentAlignment = Alignment.Center
    ) {
        // Main call info container
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .safeDrawingPadding()
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Call Type Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (callState.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                    contentDescription = null,
                    tint = Color(0xFF00A884),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (callState.isVideo) "Chamada de Vídeo" else "Chamada de Voz",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Contact Name
            Text(
                text = callState.name,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Connection Status / Timer
            val statusText = when {
                !callState.isConnected && callState.isIncoming -> "Recebendo chamada..."
                !callState.isConnected -> "Chamando..."
                else -> {
                    val minutes = callState.durationSec / 60
                    val seconds = callState.durationSec % 60
                    String.format("%02d:%02d", minutes, seconds)
                }
            }
            Text(
                text = statusText,
                color = if (callState.isConnected) Color(0xFF00A884) else Color.White.copy(alpha = 0.6f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.weight(1f))

            // Avatar Centerpiece
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(240.dp)
            ) {
                if (!callState.isConnected) {
                    // Outer pulsing ring
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(Color(0xFF00A884).copy(alpha = 0.15f))
                    )
                }

                AvatarView(
                    avatarKey = callState.avatar,
                    name = callState.name,
                    size = 130.dp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Incoming Actions Panel vs. Connected Call Panel
            if (callState.isIncoming && !callState.isConnected) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reject Button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = onReject,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF15C6D))
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Recusar",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Recusar", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }

                    // Accept Button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = onAnswer,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00A884))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Atender",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Atender", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
            } else {
                // Connected / Outgoing Call Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute Toggle
                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (isMuted) Color.White else Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mudo",
                            tint = if (isMuted) Color.Black else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Video Camera Toggle
                    IconButton(
                        onClick = { isVideoOn = !isVideoOn },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (isVideoOn) Color.White else Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = if (isVideoOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = "Vídeo",
                            tint = if (isVideoOn) Color.Black else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Speakerphone Toggle
                    IconButton(
                        onClick = { isSpeakerOn = !isSpeakerOn },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (isSpeakerOn) Color.White else Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                            contentDescription = "Alto-falante",
                            tint = if (isSpeakerOn) Color.Black else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Red Hang Up Button
                    IconButton(
                        onClick = onHangUp,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF15C6D))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Desligar",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
