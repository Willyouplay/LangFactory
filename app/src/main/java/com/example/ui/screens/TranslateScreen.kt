package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.SupportedLanguage
import com.example.ui.components.LanguageDropdownModal
import com.example.ui.theme.SpeaksyPurple
import com.example.ui.theme.SpeaksyPurpleLight
import com.example.ui.theme.SpeaksyPurpleSoft
import com.example.viewmodel.TranslatorViewModel

/**
 * Форма верхней карточки перевода (Source) с плавным вогнутым вырезом снизу,
 * обтекающим круглую кнопку реверса языков в точности как на референсе.
 */
class TopScoopedCardShape(
    private val cornerRadius: Dp = 28.dp,
    private val scoopWidth: Dp = 40.dp,
    private val scoopDepth: Dp = 22.dp
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cr = with(density) { cornerRadius.toPx() }.coerceAtMost(size.minDimension / 2f)
        val sw = with(density) { scoopWidth.toPx() }
        val sd = with(density) { scoopDepth.toPx() }
        val w = size.width
        val h = size.height
        val cx = w / 2f

        val path = Path().apply {
            reset()
            moveTo(0f, cr)
            quadraticTo(0f, 0f, cr, 0f)
            lineTo(w - cr, 0f)
            quadraticTo(w, 0f, w, cr)
            lineTo(w, h - cr)
            quadraticTo(w, h, w - cr, h)
            lineTo(cx + sw, h)
            cubicTo(
                cx + sw * 0.52f, h,
                cx + sw * 0.35f, h - sd,
                cx, h - sd
            )
            cubicTo(
                cx - sw * 0.35f, h - sd,
                cx - sw * 0.52f, h,
                cx - sw, h
            )
            lineTo(cr, h)
            quadraticTo(0f, h, 0f, h - cr)
            lineTo(0f, cr)
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Форма нижней карточки перевода (Target) с плавным вогнутым вырезом сверху,
 * обтекающим круглую кнопку реверса языков в точности как на референсе.
 */
class BottomScoopedCardShape(
    private val cornerRadius: Dp = 28.dp,
    private val scoopWidth: Dp = 40.dp,
    private val scoopDepth: Dp = 22.dp
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cr = with(density) { cornerRadius.toPx() }.coerceAtMost(size.minDimension / 2f)
        val sw = with(density) { scoopWidth.toPx() }
        val sd = with(density) { scoopDepth.toPx() }
        val w = size.width
        val h = size.height
        val cx = w / 2f

        val path = Path().apply {
            reset()
            moveTo(0f, cr)
            quadraticTo(0f, 0f, cr, 0f)
            lineTo(cx - sw, 0f)
            cubicTo(
                cx - sw * 0.52f, 0f,
                cx - sw * 0.35f, sd,
                cx, sd
            )
            cubicTo(
                cx + sw * 0.35f, sd,
                cx + sw * 0.52f, 0f,
                cx + sw, 0f
            )
            lineTo(w - cr, 0f)
            quadraticTo(w, 0f, w, cr)
            lineTo(w, h - cr)
            quadraticTo(w, h, w - cr, h)
            lineTo(cr, h)
            quadraticTo(0f, h, 0f, h - cr)
            lineTo(0f, cr)
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Контейнер, объединяющий карточку ввода и результата с парящей кнопкой реверса в центре выреза,
 * обеспечивая пропорциональность и 100% отклик на нажатия по всей площади кнопки.
 */
@Composable
private fun ScoopedTranslateContainer(
    modifier: Modifier = Modifier,
    card1: @Composable () -> Unit,
    card2: @Composable () -> Unit,
    button: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = {
            card1()
            card2()
            button()
        },
        modifier = modifier
    ) { measurables, constraints ->
        val card1Placeable = measurables[0].measure(constraints)
        val card2Placeable = measurables[1].measure(constraints)
        val buttonPlaceable = measurables[2].measure(constraints.copy(minWidth = 0, minHeight = 0))

        val gap = 8.dp.roundToPx()
        val totalHeight = card1Placeable.height + gap + card2Placeable.height

        layout(constraints.maxWidth, totalHeight) {
            card1Placeable.placeRelative(0, 0)
            val card2Y = card1Placeable.height + gap
            card2Placeable.placeRelative(0, card2Y)

            val buttonX = (constraints.maxWidth - buttonPlaceable.width) / 2
            val buttonY = card1Placeable.height + (gap - buttonPlaceable.height) / 2
            buttonPlaceable.placeRelative(buttonX, buttonY, zIndex = 10f)
        }
    }
}

@Composable
fun TranslateScreen(
    viewModel: TranslatorViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sourceLang by viewModel.sourceLang.collectAsStateWithLifecycle()
    val targetLang by viewModel.targetLang.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val translatedText by viewModel.translatedText.collectAsStateWithLifecycle()
    val confidence by viewModel.translationConfidence.collectAsStateWithLifecycle()
    val glossariesCount by viewModel.appliedGlossariesCount.collectAsStateWithLifecycle()
    val deltasCount by viewModel.appliedDeltasCount.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val isTranslating by viewModel.isTranslating.collectAsStateWithLifecycle()
    val isBridgeTranslation by viewModel.isBridgeTranslation.collectAsStateWithLifecycle()
    val bridgeIntermediateText by viewModel.bridgeIntermediateText.collectAsStateWithLifecycle()
    val bridgeAutoDownloadNotice by viewModel.bridgeAutoDownloadNotice.collectAsStateWithLifecycle()
    val offlineModelPacks by viewModel.offlineModelPacks.collectAsStateWithLifecycle()
    val downloadedLanguages by viewModel.downloadedLanguages.collectAsStateWithLifecycle()
    val isOfflineModelMode by viewModel.isOfflineModelMode.collectAsStateWithLifecycle()

    var showSourcePicker by remember { mutableStateOf(false) }
    var showTargetPicker by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    val missingModels = remember(sourceLang, targetLang, offlineModelPacks, isOfflineModelMode) {
        if (isOfflineModelMode) viewModel.getMissingModelsForPair(sourceLang, targetLang) else emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 110.dp)
    ) {
        // --- 1. Top Bar: Назад, Заголовок «Перевод», Поделиться ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onBack() }
                    .testTag("translate_back_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = "Перевод",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp
                )
            )

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "$inputText\n\n$translatedText")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Поделиться переводом"))
                    }
                    .testTag("translate_share_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Поделиться",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 2. ВЫБОР МОДЕЛИ СВЕРХУ: ОФФЛАЙН (оранжевый) / ОНЛАЙН (зеленый) ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Оффлайн режим (кнопка переключения с оранжевым акцентом)
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { viewModel.setIsOfflineModelMode(true) }
                        .testTag("translate_offline_mode_toggle"),
                    color = if (isOfflineModelMode) Color(0xFFFEF3C7) else Color.Transparent,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (isOfflineModelMode) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Оффлайн модель",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isOfflineModelMode) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isOfflineModelMode) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                // Онлайн режим (кнопка переключения с зеленым акцентом)
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { viewModel.setIsOfflineModelMode(false) }
                        .testTag("translate_online_mode_toggle"),
                    color = if (!isOfflineModelMode) Color(0xFFD1FAE5) else Color.Transparent,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = if (!isOfflineModelMode) Color(0xFF065F46) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Gemini Neural MT",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (!isOfflineModelMode) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (!isOfflineModelMode) Color(0xFF065F46) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 3. Bridge Notice Banner (Автоскачивание EN моста) ---
        if (bridgeAutoDownloadNotice != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ℹ️", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = bridgeAutoDownloadNotice ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF1E40AF),
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.dismissBridgeNotice() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Закрыть",
                            tint = Color(0xFF1E40AF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // --- 4. Missing Model Warning & Auto-Download Prompt ---
        if (missingModels.isNotEmpty()) {
            val isDark = MaterialTheme.colorScheme.background.run { red < 0.2f && green < 0.2f && blue < 0.2f }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(18.dp),
                color = if (isDark) Color(0xFF2B1F0C) else Color(0xFFFFFBEB),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF5A3E15) else Color(0xFFFDE68A))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚠️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Требуется оффлайн-модель для перевода",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    val missingNames = missingModels.joinToString(", ") { lang ->
                        val pack = offlineModelPacks.firstOrNull { it.language == lang }
                        "${pack?.nativeName ?: lang.displayName} (${pack?.sizeMb ?: 45} МБ)"
                    }
                    Text(
                        text = "Не установлены: $missingNames. При скачивании языков через мост промежуточная модель EN скачивается автоматически.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.downloadMissingModelsForCurrentPair() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD97706)
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 14.dp,
                            vertical = 8.dp
                        )
                    ) {
                        Text("Скачать необходимые модели", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- 5. Карточки ввода и результата перевода с обтеканием и соразмерностью ---
        ScoopedTranslateContainer(
            modifier = Modifier.fillMaxWidth(),
            card1 = {
                // Карточка 1: Исходный текст (Source) со скошенным вырезом снизу
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 175.dp),
                    shape = remember { TopScoopedCardShape(cornerRadius = 24.dp, scoopWidth = 40.dp, scoopDepth = 22.dp) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 175.dp)
                            .padding(horizontal = 18.dp)
                            .padding(top = 16.dp, bottom = 26.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isListening) Color(0xFFFF7675) else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { viewModel.toggleVoiceListening() }
                                        .testTag("source_mic_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Голосовой ввод",
                                        tint = if (isListening) Color.White else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { viewModel.speakSource() }
                                        .testTag("source_speaker_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Озвучить оригинал",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Выбор исходного языка
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { showSourcePicker = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(sourceLang?.flagEmoji ?: "🌐", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = sourceLang?.displayName ?: "Выберите язык",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        softWrap = false,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color(0xFFA0AEC0)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Поле ввода текста
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { viewModel.onInputTextChanged(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("source_text_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            placeholder = { Text("Введите текст для перевода", color = Color(0xFFA0AEC0)) },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 24.sp
                            ),
                            trailingIcon = {
                                if (inputText.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onInputTextChanged("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Очистить ввод",
                                            tint = Color(0xFFA0AEC0)
                                        )
                                    }
                                }
                            },
                            minLines = 3
                        )
                    }
                }
            },
            card2 = {
                // Карточка 2: Результат перевода (Target) со скошенным вырезом сверху
                val hasInput = inputText.trim().isNotEmpty()
                val isErrorResult = hasInput && (
                    SupportedLanguage.entries.any {
                        translatedText.trim() == SupportedLanguage.getTranslationErrorMessage(it).trim()
                    } || translatedText.startsWith("Ошибка перевода") || translatedText.startsWith("Translation error")
                )

                val displayTranslatedText = when {
                    !hasInput -> "Перевод отобразится здесь..."
                    translatedText.isNotBlank() -> translatedText
                    isTranslating -> "Выполняется перевод..."
                    else -> "Перевод отобразится здесь..."
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 175.dp),
                    shape = remember { BottomScoopedCardShape(cornerRadius = 24.dp, scoopWidth = 40.dp, scoopDepth = 22.dp) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 175.dp)
                            .padding(horizontal = 18.dp)
                            .padding(top = 26.dp, bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Выбор целевого языка
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { showTargetPicker = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(targetLang?.flagEmoji ?: "🌐", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = targetLang?.displayName ?: "Выберите язык",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        softWrap = false,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color(0xFFA0AEC0)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable(enabled = hasInput && translatedText.isNotBlank() && !isErrorResult) {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Translation", translatedText))
                                            Toast.makeText(context, "Скопировано в буфер обмена", Toast.LENGTH_SHORT).show()
                                        }
                                        .testTag("target_copy_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Скопировать перевод",
                                        tint = if (hasInput && translatedText.isNotBlank() && !isErrorResult) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable(enabled = hasInput && translatedText.isNotBlank() && !isErrorResult) { viewModel.speakTarget() }
                                        .testTag("target_speaker_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Озвучить перевод",
                                        tint = if (hasInput && translatedText.isNotBlank() && !isErrorResult) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Текст перевода
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 64.dp)
                        ) {
                            Text(
                                text = displayTranslatedText,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isErrorResult) FontWeight.SemiBold else FontWeight.Normal,
                                    color = when {
                                        isErrorResult -> MaterialTheme.colorScheme.error
                                        hasInput && translatedText.isNotBlank() -> MaterialTheme.colorScheme.onSurface
                                        else -> Color(0xFFA0AEC0)
                                    },
                                    lineHeight = 26.sp
                                ),
                                textAlign = if (targetLang?.isRtl == true) TextAlign.End else TextAlign.Start,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("target_translation_output")
                            )
                        }

                        if (hasInput && translatedText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))

                            // Статус нейросети и примененных терминов
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val engineDetails = buildString {
                                        val engineLabel = if (isOfflineModelMode) "Оффлайн NPU Langfactory" else "Gemini Neural MT"
                                        append("$engineLabel (${"%.0f".format(confidence * 100)}%)")
                                        if (glossariesCount > 0) append(" • $glossariesCount терминов")
                                        if (deltasCount > 0) append(" • $deltasCount нейродельт")
                                    }
                                    Text(
                                        text = engineDetails,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            },
            button = {
                // Круглая кнопка реверса языков с обтеканием и физической анимацией вращения
                var swapRotation by remember { mutableFloatStateOf(0f) }
                val animatedRotation by animateFloatAsState(
                    targetValue = swapRotation,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "swapRotation"
                )

                Surface(
                    onClick = {
                        swapRotation += 180f
                        viewModel.swapLanguages()
                    },
                    enabled = sourceLang != null && targetLang != null,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp,
                    border = BorderStroke(3.5.dp, MaterialTheme.colorScheme.background),
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("swap_languages_floating_button")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primary, SpeaksyPurpleLight)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Поменять языки местами",
                            tint = if (sourceLang != null && targetLang != null) Color.White else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(26.dp)
                                .rotate(animatedRotation)
                        )
                    }
                }
            }
        )

        val hasStartedTranslating = inputText.isNotEmpty() || isListening

        // --- 6. ПРЯМОЙ ПЕРЕВОД (ВРЕМЕННОЕ УПОМИНАНИЕ - СКРЫВАЕТСЯ ПРИ ВВОДЕ ИЛИ ЗАПИСИ) ---
        if (!hasStartedTranslating && !isBridgeTranslation) {
            Spacer(modifier = Modifier.height(18.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⚡", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Прямой перевод",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 17.sp
                                )
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF22C55E).copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "ПРЯМОЙ",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF22C55E),
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val sName = sourceLang?.displayName ?: "—"
                    val tName = targetLang?.displayName ?: "—"
                    Text(
                        text = "Для пары $sName ➔ $tName перевод выполняется напрямую без промежуточного моста, обеспечивая минимальную задержку и максимальную естественность.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )
                    )
                }
            }
        }

        // --- 7. ПРОМЕЖУТОЧНЫЙ СМЫСЛОВОЙ МОСТ (ПОСТОЯННЫЙ ПЕРЕВОД, УПОМИНАНИЕ СКРЫВАЕТСЯ ПРИ ВВОДЕ ИЛИ ЗАПИСИ) ---
        if (isBridgeTranslation) {
            Spacer(modifier = Modifier.height(18.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Само упоминание / пояснение моста скрывается после начала ввода или записи
                    if (!hasStartedTranslating) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🔀", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Смысловой мост (EN)",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 17.sp
                                    )
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "МОСТ EN",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val sName = sourceLang?.displayName ?: "—"
                        val tName = targetLang?.displayName ?: "—"
                        Text(
                            text = "Перевод между $sName и $tName проходит через смысловой английский мост для максимальной точности.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 17.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Постоянное отображение конкретно промежуточного перевода
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🇬🇧", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Английский смысловой конструкт:",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!bridgeIntermediateText.isNullOrBlank()) {
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Bridge English", bridgeIntermediateText))
                                                Toast.makeText(context, "Английский конструкт скопирован", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Скопировать",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        IconButton(
                                            onClick = { viewModel.speakIntermediate() },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = "Озвучить английский",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            val bridgeTextDisplay = when {
                                !bridgeIntermediateText.isNullOrBlank() -> bridgeIntermediateText ?: ""
                                isTranslating -> "Выполняется построение промежуточного смыслового конструкта (EN)..."
                                inputText.isBlank() -> "Английский смысловой конструкт отобразится здесь после ввода..."
                                else -> "Не удалось сформировать промежуточный английский конструкт для данного текста"
                            }

                            Text(
                                text = bridgeTextDisplay,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = if (!bridgeIntermediateText.isNullOrBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    fontWeight = if (!bridgeIntermediateText.isNullOrBlank()) FontWeight.SemiBold else FontWeight.Normal,
                                    lineHeight = 22.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSourcePicker) {
        LanguageDropdownModal(
            selectedLanguage = sourceLang,
            onLanguageSelected = { viewModel.setSourceLang(it) },
            onDismissRequest = { showSourcePicker = false },
            disabledLanguage = null,
            title = "Исходный язык",
            isOfflineMode = isOfflineModelMode,
            downloadedLanguages = downloadedLanguages
        )
    }

    if (showTargetPicker) {
        LanguageDropdownModal(
            selectedLanguage = targetLang,
            onLanguageSelected = { viewModel.setTargetLang(it) },
            onDismissRequest = { showTargetPicker = false },
            disabledLanguage = sourceLang,
            title = "Целевой язык",
            isOfflineMode = isOfflineModelMode,
            downloadedLanguages = downloadedLanguages
        )
    }
}
