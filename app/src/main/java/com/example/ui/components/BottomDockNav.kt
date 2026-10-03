package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SpeaksyPurple
import com.example.ui.theme.SpeaksyPurpleLight
import com.example.viewmodel.AppScreen

import androidx.compose.material.icons.filled.Translate

@Composable
fun BottomDockNav(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
    onVoiceClick: () -> Unit = { onNavigate(AppScreen.TRANSLATE) },
    modifier: Modifier = Modifier
) {
    val isTranslateActive = currentScreen == AppScreen.TRANSLATE

    // Pulse animation for center voice button halo
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_halo")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Dock Bar Background Surface
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                    ambientColor = Color(0x18000000)
                ),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
                    .height(62.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Главная
                DockItemWithLabel(
                    icon = Icons.Default.Home,
                    label = "Главная",
                    isSelected = currentScreen == AppScreen.HOME,
                    testTag = "nav_home",
                    contentDescription = "Главная",
                    onClick = { onNavigate(AppScreen.HOME) }
                )

                // 2. История переводов (вместо обучения)
                DockItemWithLabel(
                    icon = Icons.Default.History,
                    label = "История",
                    isSelected = currentScreen == AppScreen.HISTORY,
                    testTag = "nav_history",
                    contentDescription = "История",
                    onClick = { onNavigate(AppScreen.HISTORY) }
                )

                // Зарезервированное место под крупную центральную кнопку
                Box(
                    modifier = Modifier.size(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Пространство для плавающей кнопки по центру
                }

                // 4. Словарь
                DockItemWithLabel(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    label = "Словарь",
                    isSelected = currentScreen == AppScreen.DICTIONARY,
                    testTag = "nav_dictionary",
                    contentDescription = "Словарь",
                    onClick = { onNavigate(AppScreen.DICTIONARY) }
                )

                // 5. Параметры
                DockItemWithLabel(
                    icon = Icons.Default.Settings,
                    label = "Параметры",
                    isSelected = currentScreen == AppScreen.SETTINGS,
                    testTag = "nav_settings",
                    contentDescription = "Параметры",
                    onClick = { onNavigate(AppScreen.SETTINGS) }
                )
            }
        }

        // 3. ЦЕНТР: КРУПНО ВЫДЕЛЕННАЯ ОСНОВНАЯ ФИЧА — ГОЛОСОВОЙ ПЕРЕВОД
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-18).dp),
            contentAlignment = Alignment.Center
        ) {
            // Мягкое пульсирующее свечение вокруг центральной кнопки
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .scale(if (isTranslateActive) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                SpeaksyPurple.copy(alpha = if (isTranslateActive) 0.40f else 0.20f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Основная крупная круглая кнопка
            Surface(
                modifier = Modifier
                    .size(68.dp)
                    .shadow(
                        elevation = 22.dp,
                        shape = CircleShape,
                        spotColor = SpeaksyPurple.copy(alpha = 0.55f)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onVoiceClick
                    )
                    .testTag("nav_voice_center"),
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(
                    width = if (isTranslateActive) 3.5.dp else 2.5.dp,
                    color = if (isTranslateActive) Color(0xFFFFD166) else MaterialTheme.colorScheme.surface
                )
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF6C5CE7),
                                    Color(0xFF8C7AE6),
                                    Color(0xFFA29BFE)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Перевод",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "ПЕРЕВОД",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 0.6.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DockItemWithLabel(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    testTag: String,
    contentDescription: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val tint by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
        label = "nav_tint"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = tint
            )
        )
    }
}
