package com.example.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.viewmodel.TranslatorViewModel

data class LocalizedStrings(
    // App Header & Brand
    val appTitle: String,
    val appSubtitle: String,
    val skip: String,
    val continueBtn: String,
    val getStartedBtn: String,
    val back: String,

    // Onboarding Slide 1: UI Language
    val slide1Title: String,
    val slide1Subtitle: String,
    val langRussianTitle: String,
    val langRussianDesc: String,
    val langEnglishTitle: String,
    val langEnglishDesc: String,
    val langKoreanTitle: String,
    val langKoreanDesc: String,

    // Onboarding Slide 2: Mic Permission
    val slide2Title: String,
    val slide2Subtitle: String,
    val micAccessTitle: String,
    val micGrantedLabel: String,
    val micRequestLabel: String,
    val micGrantedNotice: String,
    val micRequestNotice: String,

    // Onboarding Slide 3: Model Choice
    val slide3Title: String,
    val slide3Subtitle: String,
    val modelOfflineTitle: String,
    val modelOfflineDesc: String,
    val modelOfflineBadge: String,
    val modelCloudTitle: String,
    val modelCloudDesc: String,

    // Onboarding Slide 4: CSV Training
    val slide4Title: String,
    val slide4Subtitle: String,
    val exportTemplateBtn: String,
    val uploadCsvBtn: String,
    val preloadPackBtn: String,
    val trainModelBtn: String,
    val trainingProgressText: String,
    val trainingDoneText: String,

    // Main App Navigation Tabs
    val tabHome: String,
    val tabTranslate: String,
    val tabNeural: String,
    val tabDictionary: String,
    val tabSettings: String,

    // Home Screen
    val searchPlaceholder: String,
    val heroTitle: String,
    val heroBtn: String,
    val quickTranslateTitle: String,
    val enterTextPlaceholder: String,
    val translationResultTitle: String,
    val voiceTranslatorTitle: String,
    val voiceTranslatorDesc: String,

    // Translate Screen
    val translateTitle: String,
    val sourcePlaceholder: String,
    val targetPlaceholder: String,
    val recommendedTitle: String,
    val showAll: String
)

val RussianStrings = LocalizedStrings(
    appTitle = "Langfactory",
    appSubtitle = "RU • KO Industrial AI",
    skip = "Пропустить",
    continueBtn = "Продолжить →",
    getStartedBtn = "Начать работу 🚀",
    back = "Назад",

    slide1Title = "Язык интерфейса",
    slide1Subtitle = "Выберите язык приложения. Все меню, подсказки и регламенты будут на выбранном языке.",
    langRussianTitle = "Русский",
    langRussianDesc = "Интерфейс на русском языке",
    langEnglishTitle = "English",
    langEnglishDesc = "English interface language",
    langKoreanTitle = "한국어",
    langKoreanDesc = "한국어 인터페이스",

    slide2Title = "Доступ к микрофону",
    slide2Subtitle = "Разрешение необходимо для того, чтобы у нас была возможность производить голосовой ввод.",
    micAccessTitle = "Доступ к микрофону",
    micGrantedLabel = "Разрешено",
    micRequestLabel = "Предоставить доступ",
    micGrantedNotice = "Доступ предоставлен ✓ Голосовой ввод активен",
    micRequestNotice = "Нажмите для вызова стандартного системного диалога Android.",

    slide3Title = "Выбор нейромодели",
    slide3Subtitle = "Выберите архитектуру обработки технического перевода.",
    modelOfflineTitle = "Офлайн NPU 1.2B (Локально)",
    modelOfflineDesc = "100% автономная работа без интернета и утечки данных",
    modelOfflineBadge = "Рекомендуется для цеха",
    modelCloudTitle = "Gemini Neural MT (Онлайн)",
    modelCloudDesc = "Передовая нейросетевая модель машинного перевода онлайн",

    slide4Title = "Обучение оффлайн-модели",
    slide4Subtitle = "Загрузите производственный глоссарий CSV или используйте готовый пакет терминов цеха.",
    exportTemplateBtn = "Шаблон CSV",
    uploadCsvBtn = "Загрузить CSV",
    preloadPackBtn = "Использовать пакет ЧПУ & ТО (K-RUS)",
    trainModelBtn = "Обучить модель",
    trainingProgressText = "Калибровка нейронных синапсов...",
    trainingDoneText = "Оффлайн-модель успешно дообучена!",

    tabHome = "Главная",
    tabTranslate = "Переводчик",
    tabNeural = "Нейросеть",
    tabDictionary = "Словари",
    tabSettings = "Параметры",

    searchPlaceholder = "Поиск регламентов, кодов ошибок ЧПУ...",
    heroTitle = "Технический перевод\nрегламентов станков",
    heroBtn = "Открыть цех перевода",
    quickTranslateTitle = "Быстрый перевод",
    enterTextPlaceholder = "Введите текст или регламент...",
    translationResultTitle = "Технический перевод",
    voiceTranslatorTitle = "Голосовой ввод и перевод",
    voiceTranslatorDesc = "Мгновенный голосовой перевод команд, регламентов и параметров оборудования.",

    translateTitle = "Технический цех",
    sourcePlaceholder = "Введите регламент, команду или текст ТО...",
    targetPlaceholder = "Технический перевод отобразится здесь...",
    recommendedTitle = "Производственные направления",
    showAll = "Все пары"
)

val EnglishStrings = LocalizedStrings(
    appTitle = "Langfactory",
    appSubtitle = "Industrial Voice & Neural AI",
    skip = "Skip",
    continueBtn = "Continue →",
    getStartedBtn = "Get Started 🚀",
    back = "Back",

    slide1Title = "Interface Language",
    slide1Subtitle = "Select the application language. All menus, guides, and manuals will adapt instantly.",
    langRussianTitle = "Русский",
    langRussianDesc = "Russian interface language",
    langEnglishTitle = "English",
    langEnglishDesc = "English interface language",
    langKoreanTitle = "한국어",
    langKoreanDesc = "Korean interface language",

    slide2Title = "Microphone Access",
    slide2Subtitle = "Permission is required to enable voice input for text and technical commands.",
    micAccessTitle = "Microphone Permission",
    micGrantedLabel = "Granted",
    micRequestLabel = "Grant Permission",
    micGrantedNotice = "Access granted ✓ Voice input is active",
    micRequestNotice = "Tap to prompt standard Android system dialog.",

    slide3Title = "Neural Engine Choice",
    slide3Subtitle = "Choose the execution architecture for technical translation.",
    modelOfflineTitle = "Offline NPU 1.2B (On-Device)",
    modelOfflineDesc = "100% on-device offline processing without data leaks",
    modelOfflineBadge = "Recommended for Plant",
    modelCloudTitle = "Cloud Engine (Online)",
    modelCloudDesc = "Requires stable Wi-Fi connection",

    slide4Title = "Offline Model Training",
    slide4Subtitle = "Export a CSV template or load custom plant terms to fine-tune the on-device AI.",
    exportTemplateBtn = "CSV Template",
    uploadCsvBtn = "Upload CSV",
    preloadPackBtn = "Use CNC & Maintenance Pack",
    trainModelBtn = "Train Model",
    trainingProgressText = "Calibrating neural synapses...",
    trainingDoneText = "Offline neural model calibrated successfully!",

    tabHome = "Home",
    tabTranslate = "Translate",
    tabNeural = "Neural AI",
    tabDictionary = "Glossary",
    tabSettings = "Settings",

    searchPlaceholder = "Search manuals, CNC alarm codes...",
    heroTitle = "Technical Translation\nfor Machine Tools",
    heroBtn = "Open Shop Floor Hub",
    quickTranslateTitle = "Quick Translation",
    enterTextPlaceholder = "Enter technical phrase...",
    translationResultTitle = "Technical Translation",
    voiceTranslatorTitle = "Voice Input & Translation",
    voiceTranslatorDesc = "Instant hands-free speech translation while operating machinery.",

    translateTitle = "Shop Floor Translator",
    sourcePlaceholder = "Enter manual, command, or maintenance text...",
    targetPlaceholder = "Translation will appear here...",
    recommendedTitle = "Industrial Pairs",
    showAll = "Show All"
)

val KoreanStrings = LocalizedStrings(
    appTitle = "Langfactory",
    appSubtitle = "산업용 양방향 음성 인공지능",
    skip = "건너뛰기",
    continueBtn = "계속하기 →",
    getStartedBtn = "시작하기 🚀",
    back = "뒤로",

    slide1Title = "인터페이스 언어",
    slide1Subtitle = "앱에서 사용할 인터페이스 언어를 선택하세요. 모든 메뉴와 안내가 즉시 변경됩니다.",
    langRussianTitle = "Русский",
    langRussianDesc = "러시아어 인터페이스",
    langEnglishTitle = "English",
    langEnglishDesc = "영어 인터페이스",
    langKoreanTitle = "한국어",
    langKoreanDesc = "한국어 인터페이스",

    slide2Title = "마이크 접근 권한",
    slide2Subtitle = "음성 입력을 통해 텍스트와 기술 명령어를 번역할 수 있도록 마이크 권한이 필요합니다.",
    micAccessTitle = "마이크 권한",
    micGrantedLabel = "허용됨",
    micRequestLabel = "권한 요청",
    micGrantedNotice = "마이크 접근 권한이 허용되었습니다 ✓",
    micRequestNotice = "안드로이드 표준 시스템 권한 창을 호출합니다.",

    slide3Title = "신경망 모델 선택",
    slide3Subtitle = "기술 번역을 처리할 신경망 아키텍처를 선택하세요.",
    modelOfflineTitle = "오프라인 NPU 1.2B (온디바이스)",
    modelOfflineDesc = "인터넷 없는 현장에서도 100% 보안 오프라인 구동",
    modelOfflineBadge = "공장 현장 권장",
    modelCloudTitle = "클라우드 번역 (온라인)",
    modelCloudDesc = "안정적인 Wi-Fi 네트워크가 필요합니다",

    slide4Title = "오프라인 모델 학습",
    slide4Subtitle = "CSV 템플릿을 내보내거나 공장 전용 용어 사전을 불러와 로컬 인공지능을 미세조정하세요.",
    exportTemplateBtn = "CSV 템플릿",
    uploadCsvBtn = "CSV 파일 업로드",
    preloadPackBtn = "공작기계 & 정비 패키지 사용",
    trainModelBtn = "모델 학습 시작",
    trainingProgressText = "신경망 가중치 최적화 중...",
    trainingDoneText = "오프라인 신경망 학습이 완료되었습니다!",

    tabHome = "홈",
    tabTranslate = "통역",
    tabNeural = "인공지능",
    tabDictionary = "단어장",
    tabSettings = "설정",

    searchPlaceholder = "공작기계 매뉴얼, 알람 코드 검색...",
    heroTitle = "공장 설비 및\n공작기계 기술 통역",
    heroBtn = "통역 작업장 열기",
    quickTranslateTitle = "빠른 번역",
    enterTextPlaceholder = "명령어 또는 정비 텍스트 입력...",
    translationResultTitle = "기술 번역 결과",
    voiceTranslatorTitle = "음성 입력 및 통역",
    voiceTranslatorDesc = "장비 운용 중에도 양방향 음성 즉시 통역.",

    translateTitle = "기술 통역 작업장",
    sourcePlaceholder = "매뉴얼, 지침 또는 정비 텍스트 입력...",
    targetPlaceholder = "기술 번역 결과가 여기에 표시됩니다...",
    recommendedTitle = "주요 산업 언어쌍",
    showAll = "전체 보기"
)

fun getStringsForLanguage(code: String): LocalizedStrings = when (code) {
    "en", "en-GB" -> EnglishStrings
    "ko" -> KoreanStrings
    else -> RussianStrings
}

@Composable
fun rememberAppStrings(viewModel: TranslatorViewModel): LocalizedStrings {
    val lang by viewModel.appLanguage.collectAsState()
    return getStringsForLanguage(lang)
}
