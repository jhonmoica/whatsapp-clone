package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AvatarView(
    avatarKey: String,
    name: String,
    size: Dp = 50.dp,
    modifier: Modifier = Modifier
) {
    val brush = when (avatarKey) {
        "gemini_bot" -> Brush.radialGradient(
            colors = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
        )
        "mother" -> Brush.verticalGradient(
            colors = listOf(Color(0xFFF472B6), Color(0xFFBE185D))
        )
        "carlos" -> Brush.horizontalGradient(
            colors = listOf(Color(0xFFF59E0B), Color(0xFFD97706))
        )
        "group_fam" -> Brush.linearGradient(
            colors = listOf(Color(0xFF10B981), Color(0xFF047857))
        )
        else -> Brush.radialGradient(
            colors = listOf(Color(0xFF6B7280), Color(0xFF374151))
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(brush),
        contentAlignment = Alignment.Center
    ) {
        when (avatarKey) {
            "gemini_bot" -> {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Gemini",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.5f)
                )
            }
            "group_fam" -> {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = "Grupo",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.55f)
                )
            }
            "mother" -> {
                Text(
                    text = "Mãe",
                    color = Color.White,
                    fontSize = (size.value * 0.32f).sp,
                    fontWeight = FontWeight.Bold
                )
            }
            "carlos" -> {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Carlos",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.6f)
                )
            }
            else -> {
                // First 2 letters as initials
                val initials = if (name.length >= 2) name.substring(0, 2).uppercase() else name.uppercase()
                Text(
                    text = initials,
                    color = Color.White,
                    fontSize = (size.value * 0.35f).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
