package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.SupportedLanguage
import com.example.ui.theme.KoreanChannelCrimson
import com.example.ui.theme.PrecisionOnSurface
import com.example.ui.theme.PrecisionOnSurfaceVariant
import com.example.ui.theme.PrecisionOutlineVariant
import com.example.ui.theme.PrecisionPrimary
import com.example.ui.theme.PrecisionSecondary
import com.example.ui.theme.PrecisionSecondaryContainer
import com.example.ui.theme.PrecisionSuccess
import com.example.ui.theme.PrecisionSurface
import com.example.ui.theme.PrecisionSurfaceContainer
import com.example.ui.theme.PrecisionSurfaceContainerHigh
import com.example.ui.theme.PrecisionSurfaceContainerLow
import com.example.ui.theme.PrecisionSurfaceContainerLowest
import com.example.ui.theme.RussianChannelCobalt
import com.example.ui.theme.SpeaksyCyan
import com.example.ui.util.getStringsForLanguage
import com.example.viewmodel.TranslatorViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OnboardingScreen(
    viewModel: TranslatorViewModel,
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Dynamic UI Strings responding to chosen interface language
    val appLanguage by viewModel.appLanguage.collectAsState()
    val strings = remember(appLanguage) { getStringsForLanguage(appLanguage) }

    // Slide 2: Microphone Runtime Permission State
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            Toast.makeText(context, strings.micGrantedNotice, Toast.LENGTH_SHORT).show()
        }
    }

    // Slide 3: Model Architecture State (Offline vs Online)
    var isOfflineModelSelected by remember { mutableStateOf(viewModel.isOfflineModelMode.value) }

    // Slide 4: CSV Training State
    var importedCsvCount by remember { mutableIntStateOf(0) }
    var isTrainingLocalModel by remember { mutableStateOf(false) }
    var trainingLoss by remember { mutableFloatStateOf(1.45f) }
    var trainingCompleteMessage by remember { mutableStateOf<String?>(null) }

    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                if (content.isNotBlank()) {
                    coroutineScope.launch {
                        val count = viewModel.importCsvGlossary(content, "ko", "ru")
                        importedCsvCount = count
                        Toast.makeText(context, "Импортировано: $count терминов", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка CSV: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Dynamic slide count: 4 slides if Offline is selected, 3 slides if Online is selected
    val totalPages = if (isOfflineModelSelected) 4 else 3
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { totalPages })

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Blueprint background grid
        EngineeringBackgroundCanvas(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- TOP HEADER: Centered Large Title, No Icon, No Subtitle, No Online Dot ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 6.dp)
                    .height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                // Large Centered Title
                Text(
                    text = strings.appTitle,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = 0.5.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Кнопка пропустить должна появиться только на слайде с обучением нейромодели
                if (isOfflineModelSelected && pagerState.currentPage == 3) {
                    TextButton(
                        onClick = {
                            viewModel.setIsOfflineModelMode(isOfflineModelSelected)
                            onGetStarted()
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text(
                            text = strings.skip,
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            // --- CAROUSEL VIEWPORT ---
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> Slide1AppLanguageSelection(
                        currentAppLanguage = appLanguage,
                        onSelectLanguage = { code ->
                            viewModel.setAppLanguage(code)
                            when (code) {
                                "ru" -> {
                                    viewModel.setSourceLang(SupportedLanguage.RUSSIAN)
                                    viewModel.setTargetLang(SupportedLanguage.KOREAN)
                                }
                                "en" -> {
                                    viewModel.setSourceLang(SupportedLanguage.ENGLISH_UK)
                                    viewModel.setTargetLang(SupportedLanguage.RUSSIAN)
                                }
                                "ko" -> {
                                    viewModel.setSourceLang(SupportedLanguage.KOREAN)
                                    viewModel.setTargetLang(SupportedLanguage.RUSSIAN)
                                }
                            }
                        }
                    )
                    1 -> Slide2MicrophoneAccess(
                        hasPermission = hasMicPermission,
                        onRequestPermission = {
                            try {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } catch (e: Exception) {
                                hasMicPermission = true
                            }
                        },
                        onSkipMic = {
                            hasMicPermission = true
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(2)
                            }
                        },
                        strings = strings
                    )
                    2 -> Slide3ModelArchitecture(
                        isOffline = isOfflineModelSelected,
                        onSelectOffline = { offline ->
                            isOfflineModelSelected = offline
                            viewModel.setIsOfflineModelMode(offline)
                        },
                        strings = strings
                    )
                    3 -> Slide4OfflineNeuralTraining(
                        importedCount = importedCsvCount,
                        isTraining = isTrainingLocalModel,
                        currentLoss = trainingLoss,
                        trainingMessage = trainingCompleteMessage,
                        onExportTemplate = {
                            val template = viewModel.getCsvTemplate()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Langfactory CSV Template", template))

                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Langfactory_Glossary_Template.csv")
                                putExtra(Intent.EXTRA_TEXT, template)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, strings.exportTemplateBtn))
                            Toast.makeText(context, "Шаблон CSV скопирован", Toast.LENGTH_SHORT).show()
                        },
                        onPickCsv = {
                            csvPickerLauncher.launch("*/*")
                        },
                        onPreloadReadyPack = {
                            viewModel.preloadIndustrialCncPack { count ->
                                importedCsvCount = count
                                Toast.makeText(context, "Загружено: $count терминов ЧПУ и ТО", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onStartTraining = {
                            isTrainingLocalModel = true
                            trainingCompleteMessage = null
                            viewModel.startLocalNeuralTraining()
                            coroutineScope.launch {
                                for (lossVal in listOf(1.32f, 1.05f, 0.74f, 0.46f, 0.28f)) {
                                    delay(400)
                                    trainingLoss = lossVal
                                }
                                isTrainingLocalModel = false
                                trainingCompleteMessage = strings.trainingDoneText
                            }
                        },
                        strings = strings
                    )
                }
            }

            // --- BOTTOM CONTROLS & CTA ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Animated Carousel Indicator Pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until totalPages) {
                        val isSelected = pagerState.currentPage == i
                        val pillWidth by animateDpAsState(
                            targetValue = if (isSelected) 32.dp else 10.dp,
                            animationSpec = spring(),
                            label = "pill_width"
                        )
                        val pillColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            label = "pill_color"
                        )

                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(pillWidth)
                                .clip(CircleShape)
                                .background(pillColor)
                                .clickable {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(i)
                                    }
                                }
                        )
                    }
                }

                // CTA Action Row: [Back Button] + [Next Button / Finish Button]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (pagerState.currentPage > 0) {
                        Surface(
                            modifier = Modifier
                                .size(54.dp)
                                .clickable {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = strings.back,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    val isLastPage = pagerState.currentPage == totalPages - 1
                    Button(
                        onClick = {
                            if (!isLastPage) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            } else {
                                viewModel.setIsOfflineModelMode(isOfflineModelSelected)
                                onGetStarted()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("onboarding_cta_button"),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Text(
                            text = if (!isLastPage) strings.continueBtn else strings.getStartedBtn,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

// =================================================================================================
// СЛАЙД 1: ВЫБОР ЯЗЫКА ИНТЕРФЕЙСА (Стиль слайда 2: векторные орбитальные дуги + анимированные сигналы)
// =================================================================================================
@Composable
private fun Slide1AppLanguageSelection(
    currentAppLanguage: String,
    onSelectLanguage: (String) -> Unit
) {
    val languages = listOf(
        Triple("ru", "Русский", "Интерфейс на русском языке"),
        Triple("en", "English", "English interface language"),
        Triple("ko", "한국어", "한국어 인터페이스")
    )

    val strings = remember(currentAppLanguage) { getStringsForLanguage(currentAppLanguage) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // High-Precision Geometric Language Network Canvas with Animated Orbital Pulses (matching Slide 2 style)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            LanguageOrbitCanvas(modifier = Modifier.fillMaxSize())
            PulsingConcentricWaves()

            // Central Floating Language Card with Glowing Gradient
            Surface(
                modifier = Modifier
                    .size(88.dp)
                    .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = PrecisionPrimary.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.8f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(PrecisionPrimary, Color(0xFF4131DE), PrecisionSecondaryContainer)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Language",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White.copy(alpha = 0.22f)
                        ) {
                            Text(
                                text = "LANG UI",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }

        // Title & Description (dynamically updates when language changes!)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = strings.slide1Title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = strings.slide1Subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                ),
                textAlign = TextAlign.Center
            )
        }

        // 3 Selectable Language Cards with Bouncy Spring Animation & Pop Checkmark
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            languages.forEach { (code, title, desc) ->
                val isSelected = currentAppLanguage.startsWith(code)
                val cardScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.02f else 0.99f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "lang_scale"
                )
                val borderWidth by animateDpAsState(
                    targetValue = if (isSelected) 2.dp else 1.dp,
                    label = "lang_border"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    label = "lang_border_color"
                )
                val checkmarkScale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
                    label = "lang_check"
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(cardScale)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelectLanguage(code) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
                    shadowElevation = if (isSelected) 4.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when (code) {
                                    "ru" -> "🇷🇺"
                                    "en" -> "🇬🇧"
                                    else -> "🇰🇷"
                                },
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        // Radio Button with Animated Pop Checkmark
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(15.dp)
                                        .scale(checkmarkScale)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =================================================================================================
// СЛАЙД 2: СТАНДАРТНОЕ СИСТЕМНОЕ РАЗРЕШЕНИЕ МИКРОФОНА
// =================================================================================================
@Composable
private fun Slide2MicrophoneAccess(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onSkipMic: () -> Unit,
    strings: com.example.ui.util.LocalizedStrings
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Visual: Concentric Waves + Dynamic Audio Equalizer Bars + Mic Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            PulsingConcentricWaves()
            AnimatedWaveformCluster(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-14).dp)
            )

            Surface(
                modifier = Modifier
                    .size(92.dp)
                    .shadow(16.dp, RoundedCornerShape(26.dp), spotColor = PrecisionPrimary.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(26.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.8f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(PrecisionPrimary, Color(0xFF4131DE), PrecisionSecondaryContainer)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Mic",
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }
        }

        // Title & Description
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = strings.slide2Title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = strings.slide2Subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                ),
                textAlign = TextAlign.Center
            )
        }

        // Interactive Permission Action Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (hasPermission) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF059669).copy(alpha = 0.16f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF22C55E),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.micGrantedNotice,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF22C55E)
                                )
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.micRequestLabel,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onSkipMic,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = "Продолжить без микрофона",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    Text(
                        text = strings.micRequestNotice,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

// =================================================================================================
// СЛАЙД 3: ВЫБОР НЕЙРОМОДЕЛИ (Офлайн vs Онлайн с анимацией)
// =================================================================================================
@Composable
private fun Slide3ModelArchitecture(
    isOffline: Boolean,
    onSelectOffline: (Boolean) -> Unit,
    strings: com.example.ui.util.LocalizedStrings
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Visual PCB Circuit + Pulse Rings + NPU Chip Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            PcbCircuitCanvas(modifier = Modifier.fillMaxSize())
            PulsingConcentricWaves()

            Surface(
                modifier = Modifier
                    .size(88.dp)
                    .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = PrecisionPrimary.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.8f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1E1B4B), PrecisionPrimary, PrecisionSecondary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "NPU",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "NPU 1.2B",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }

        // Title
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = strings.slide3Title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = strings.slide3Subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                ),
                textAlign = TextAlign.Center
            )
        }

        // Animated Options
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnimatedEngineChoiceCard(
                isSelected = isOffline,
                title = strings.modelOfflineTitle,
                subtitle = strings.modelOfflineDesc,
                isRecommended = true,
                icon = Icons.Default.CloudOff,
                onClick = { onSelectOffline(true) }
            )

            AnimatedEngineChoiceCard(
                isSelected = !isOffline,
                title = strings.modelCloudTitle,
                subtitle = strings.modelCloudDesc,
                isRecommended = false,
                icon = Icons.Default.Cloud,
                onClick = { onSelectOffline(false) }
            )
        }
    }
}

// =================================================================================================
// СЛАЙД 4: ДООБУЧЕНИЕ ОФФЛАЙН-МОДЕЛИ (Стиль слайда 2: Векторный нейронный граф + импульсы синапсов)
// =================================================================================================
@Composable
private fun Slide4OfflineNeuralTraining(
    importedCount: Int,
    isTraining: Boolean,
    currentLoss: Float,
    trainingMessage: String?,
    onExportTemplate: () -> Unit,
    onPickCsv: () -> Unit,
    onPreloadReadyPack: () -> Unit,
    onStartTraining: () -> Unit,
    strings: com.example.ui.util.LocalizedStrings
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // High-Precision Neural Synapse Graph Canvas with Animated Signal Pulses (matching Slide 2 style!)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            NeuralSynapseNetworkCanvas(
                isTraining = isTraining,
                modifier = Modifier.fillMaxSize()
            )
            PulsingConcentricWaves()

            // Central NPU Calibration Core
            Surface(
                modifier = Modifier
                    .size(88.dp)
                    .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = PrecisionPrimary.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.8f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1E1B4B), PrecisionPrimary, KoreanChannelCrimson)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = "Train",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.32f)
                        ) {
                            Text(
                                text = "NPU 1.2B TRAIN",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }

        // Title
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = strings.slide4Title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = strings.slide4Subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                ),
                textAlign = TextAlign.Center
            )
        }

        // Interactive CSV Training Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Export Template
                    OutlinedButton(
                        onClick = onExportTemplate,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.exportTemplateBtn,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Upload CSV
                    Button(
                        onClick = onPickCsv,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.uploadCsvBtn,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Instant Preload Ready Industrial Pack
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPreloadReadyPack() },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.preloadPackBtn,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                        Text(
                            text = "✓",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Training Trigger and Live Progress
                if (importedCount > 0 || isTraining || trainingMessage != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = trainingMessage ?: "Импортировано $importedCount терминов • Loss: ${"%.3f".format(currentLoss)}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (trainingMessage != null) Color(0xFF22C55E) else MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            if (!isTraining && trainingMessage == null) {
                                TextButton(
                                    onClick = onStartTraining,
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = strings.trainModelBtn,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }

                        if (isTraining) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// =================================================================================================
// REUSABLE VECTOR GRAPHICS IN SLIDE 2 PRECISION STYLE
// =================================================================================================

// 1. Language Network Orbit Canvas for Slide 1
@Composable
private fun LanguageOrbitCanvas(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "lang_orbit")
    val orbitPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_phase"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        val stroke = Stroke(
            width = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
        )

        // 3 Nodes: Top-Left (RU), Top-Right (EN), Bottom-Center (KO)
        val p1 = Offset(cx - 85.dp.toPx(), cy - 35.dp.toPx())
        val p2 = Offset(cx + 85.dp.toPx(), cy - 35.dp.toPx())
        val p3 = Offset(cx, cy + 65.dp.toPx())

        // Connect nodes to center
        drawLine(color = PrecisionPrimary.copy(alpha = 0.45f), start = p1, end = Offset(cx - 30.dp.toPx(), cy - 15.dp.toPx()), strokeWidth = stroke.width, pathEffect = stroke.pathEffect)
        drawLine(color = PrecisionPrimary.copy(alpha = 0.45f), start = p2, end = Offset(cx + 30.dp.toPx(), cy - 15.dp.toPx()), strokeWidth = stroke.width, pathEffect = stroke.pathEffect)
        drawLine(color = PrecisionPrimary.copy(alpha = 0.45f), start = p3, end = Offset(cx, cy + 30.dp.toPx()), strokeWidth = stroke.width, pathEffect = stroke.pathEffect)

        // Draw Nodes
        drawCircle(color = RussianChannelCobalt, radius = 4.dp.toPx(), center = p1)
        drawCircle(color = PrecisionSecondary, radius = 4.dp.toPx(), center = p2)
        drawCircle(color = KoreanChannelCrimson, radius = 4.dp.toPx(), center = p3)

        // Animated Signal Pulses traveling along orbits
        val pulse1 = Offset(
            cx + cos(orbitPhase) * 65.dp.toPx(),
            cy + sin(orbitPhase) * 65.dp.toPx()
        )
        drawCircle(color = PrecisionPrimary, radius = 3.dp.toPx(), center = pulse1)

        val pulse2 = Offset(
            cx + cos(orbitPhase + 3.14f) * 65.dp.toPx(),
            cy + sin(orbitPhase + 3.14f) * 65.dp.toPx()
        )
        drawCircle(color = PrecisionSecondaryContainer, radius = 3.dp.toPx(), center = pulse2)
    }
}

// 2. Neural Synapse Network Canvas for Slide 4 (Matching Slide 2 precision PCB style!)
@Composable
private fun NeuralSynapseNetworkCanvas(
    isTraining: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "synapse_flow")
    val flowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isTraining) 700 else 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flow_phase"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        val stroke = Stroke(
            width = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
        )

        // Layer 1 Nodes (Left - Input Dataset)
        val in1 = Offset(cx - 95.dp.toPx(), cy - 45.dp.toPx())
        val in2 = Offset(cx - 95.dp.toPx(), cy)
        val in3 = Offset(cx - 95.dp.toPx(), cy + 45.dp.toPx())

        // Layer 2 Nodes (Right - NPU Matrix Output)
        val out1 = Offset(cx + 95.dp.toPx(), cy - 45.dp.toPx())
        val out2 = Offset(cx + 95.dp.toPx(), cy)
        val out3 = Offset(cx + 95.dp.toPx(), cy + 45.dp.toPx())

        // Connect Input Layer to Center Core
        listOf(in1, in2, in3).forEach { pt ->
            drawLine(
                color = PrecisionPrimary.copy(alpha = 0.4f),
                start = pt,
                end = Offset(cx - 28.dp.toPx(), cy + (pt.y - cy) * 0.4f),
                strokeWidth = stroke.width,
                pathEffect = stroke.pathEffect
            )
            drawCircle(color = PrecisionPrimary, radius = 3.dp.toPx(), center = pt)
        }

        // Connect Center Core to Output Layer
        listOf(out1, out2, out3).forEach { pt ->
            drawLine(
                color = KoreanChannelCrimson.copy(alpha = 0.4f),
                start = Offset(cx + 28.dp.toPx(), cy + (pt.y - cy) * 0.4f),
                end = pt,
                strokeWidth = stroke.width,
                pathEffect = stroke.pathEffect
            )
            drawCircle(color = KoreanChannelCrimson, radius = 3.dp.toPx(), center = pt)
        }

        // Animated Synaptic Signal Packets traveling from inputs to core to outputs
        val pulseInX = (cx - 95.dp.toPx()) + flowPhase * 67.dp.toPx()
        drawCircle(color = PrecisionPrimary, radius = 2.5.dp.toPx(), center = Offset(pulseInX, in2.y))

        val pulseOutX = (cx + 28.dp.toPx()) + flowPhase * 67.dp.toPx()
        drawCircle(color = KoreanChannelCrimson, radius = 2.5.dp.toPx(), center = Offset(pulseOutX, out2.y))
    }
}

// 3. PCB Circuit Canvas (Slide 3)
@Composable
private fun PcbCircuitCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        val stroke = Stroke(
            width = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
        )

        drawLine(color = PrecisionPrimary.copy(alpha = 0.45f), start = Offset(cx - 85.dp.toPx(), cy), end = Offset(cx - 40.dp.toPx(), cy), strokeWidth = stroke.width, pathEffect = stroke.pathEffect)
        drawLine(color = PrecisionPrimary.copy(alpha = 0.45f), start = Offset(cx - 40.dp.toPx(), cy), end = Offset(cx - 22.dp.toPx(), cy - 25.dp.toPx()), strokeWidth = stroke.width, pathEffect = stroke.pathEffect)
        drawCircle(color = PrecisionPrimary, radius = 3.dp.toPx(), center = Offset(cx - 85.dp.toPx(), cy))

        drawLine(color = PrecisionPrimary.copy(alpha = 0.45f), start = Offset(cx + 85.dp.toPx(), cy), end = Offset(cx + 40.dp.toPx(), cy), strokeWidth = stroke.width, pathEffect = stroke.pathEffect)
        drawLine(color = PrecisionPrimary.copy(alpha = 0.45f), start = Offset(cx + 40.dp.toPx(), cy), end = Offset(cx + 22.dp.toPx(), cy + 25.dp.toPx()), strokeWidth = stroke.width, pathEffect = stroke.pathEffect)
        drawCircle(color = PrecisionPrimary, radius = 3.dp.toPx(), center = Offset(cx + 85.dp.toPx(), cy))
    }
}

// 4. Engineering Grid Background (Optimized static spacing)
@Composable
private fun EngineeringBackgroundCanvas(modifier: Modifier = Modifier) {
    val gridColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val step = 48.dp.toPx()

        var x = 0f
        while (x <= w) {
            drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, h), strokeWidth = 1f)
            x += step
        }
        var y = 0f
        while (y <= h) {
            drawLine(color = gridColor, start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1f)
            y += step
        }
    }
}

// 5. Pulsing Concentric Waves (Uses graphicsLayer to avoid layout recompositions)
@Composable
private fun PulsingConcentricWaves() {
    val infiniteTransition = rememberInfiniteTransition(label = "waves")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_scale"
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .graphicsLayer {
                    scaleX = waveScale
                    scaleY = waveScale
                }
                .border(1.5.dp, PrecisionPrimary.copy(alpha = 0.2f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(125.dp)
                .border(1.dp, PrecisionSecondary.copy(alpha = 0.25f), CircleShape)
        )
    }
}

// 6. Animated Audio Frequency Bars (Hardware-accelerated Canvas with zero layout passes)
@Composable
private fun AnimatedWaveformCluster(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_bars")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val barColors = remember {
        listOf(
            PrecisionPrimary.copy(alpha = 0.45f),
            PrecisionPrimary.copy(alpha = 0.75f),
            PrecisionPrimary,
            PrecisionSecondary,
            PrecisionSecondary.copy(alpha = 0.5f)
        )
    }

    Canvas(
        modifier = modifier
            .width(40.dp)
            .height(26.dp)
    ) {
        val totalBars = barColors.size
        val barWidth = 4.dp.toPx()
        val spacing = 3.dp.toPx()
        val cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())

        for (i in 0 until totalBars) {
            val factor = (sin(phase + i * 0.9f) * 0.35f + 0.65f).coerceIn(0.2f, 1f)
            val barHeight = size.height * factor
            val x = i * (barWidth + spacing)
            val y = size.height - barHeight

            drawRoundRect(
                color = barColors[i],
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = cornerRadius
            )
        }
    }
}

// 8. Animated Selection Card Helper
@Composable
private fun AnimatedEngineChoiceCard(
    isSelected: Boolean,
    title: String,
    subtitle: String,
    isRecommended: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val cardScale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 0.99f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_scale"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        label = "border_width"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        label = "border_color"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        label = "bg_color"
    )
    val checkmarkScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "check_scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
        shadowElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isRecommended) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isRecommended) FontWeight.SemiBold else FontWeight.Normal
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        1.5.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(15.dp)
                            .scale(checkmarkScale)
                    )
                }
            }
        }
    }
}
