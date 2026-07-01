package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StatusUpdate
import kotlinx.coroutines.isActive

@Composable
fun StatusViewScreen(
    statusUpdates: List<StatusUpdate>,
    currentIndex: Int,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onClose: () -> Unit,
    onReplySent: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUpdate = statusUpdates.getOrNull(currentIndex) ?: return
    var replyText by remember { mutableStateOf("") }

    // Progress bar animation
    val progress = remember { Animatable(0f) }
    
    // Key-effect to reset and restart animation when currentIndex changes
    LaunchedEffect(currentIndex) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 4000,
                easing = LinearEasing
            )
        )
        onNext()
    }

    // Modern styled background gradients based on user's name to look stylish
    val gradientColors = when (currentUpdate.contactAvatar) {
        "mother" -> listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))
        "carlos" -> listOf(Color(0xFF3B82F6), Color(0xFF10B981))
        "user_avatar" -> listOf(Color(0xFFF59E0B), Color(0xFFEF4444))
        else -> listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        // If click is on the left 30% of screen, go to previous
                        if (offset.x < size.width * 0.3f) {
                            onPrev()
                        } else {
                            // Go to next
                            onNext()
                        }
                    }
                )
            }
    ) {
        // Main Visual Status Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(gradientColors)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = currentUpdate.textContent ?: "",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            )
        }

        // Overlay Header & Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .safeDrawingPadding()
                .align(Alignment.TopCenter)
        ) {
            // Segmented Progress Bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                statusUpdates.forEachIndexed { idx, _ ->
                    val barProgress = when {
                        idx < currentIndex -> 1f
                        idx > currentIndex -> 0f
                        else -> progress.value
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(100))
                            .background(Color.White.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(barProgress)
                                .fillMaxHeight()
                                .background(Color.White)
                        )
                    }
                }
            }

            // User Info Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AvatarView(
                    avatarKey = currentUpdate.contactAvatar,
                    name = currentUpdate.contactName,
                    size = 40.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentUpdate.contactName,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Status • recente",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }

                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = Color.White
                    )
                }
            }
        }

        // Reply Input Bottom Panel (Preventing closing on keyboard clicking)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
                .pointerInput(Unit) {
                    // Swallow taps inside the bottom bar so it doesn't trigger screen transition
                    detectTapGestures { }
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                TextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = { Text("Responder", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                if (replyText.isNotBlank()) {
                    IconButton(
                        onClick = {
                            onReplySent(replyText)
                            replyText = ""
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Enviar resposta",
                            tint = Color(0xFF00A884)
                        )
                    }
                }
            }
        }
    }
}
