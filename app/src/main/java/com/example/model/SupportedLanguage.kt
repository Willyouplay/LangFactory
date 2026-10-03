package com.example.model

import java.util.Locale

/**
 * 6 Supported Languages for Langfactory Industrial Neural Translator:
 * - Direct group: Russian (RU), French (FR), Spanish (ES) - direct with each other and with English.
 * - Bridge core: English (EN) - translates directly to ANY language and serves as the semantic bridge.
 * - Asian bridge group: Korean (KO), Chinese (ZH) - connect with RU, FR, ES and each other via English Bridge.
 */
enum class SupportedLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flagEmoji: String,
    val locale: Locale,
    val isRtl: Boolean = false
) {
    RUSSIAN("ru", "Русский", "Русский", "🇷🇺", Locale("ru", "RU")),
    ENGLISH("en", "Английский", "English", "🇬🇧", Locale.UK),
    FRENCH("fr", "Французский", "Français", "🇫🇷", Locale.FRANCE),
    SPANISH("es", "Испанский", "Español", "🇪🇸", Locale("es", "ES")),
    KOREAN("ko", "Корейский", "한국어", "🇰🇷", Locale.KOREAN),
    CHINESE("zh", "Китайский", "中文", "🇨🇳", Locale.SIMPLIFIED_CHINESE);

    val isCoreBridge: Boolean get() = this == ENGLISH
    val isBridgeDependent: Boolean get() = this == KOREAN || this == CHINESE

    fun getSpeakerLabel(index: Int): String {
        val num = index.toString()
        return when (this) {
            RUSSIAN -> "$flagEmoji Собеседник $num"
            ENGLISH -> "$flagEmoji Speaker $num"
            FRENCH -> "$flagEmoji Interlocuteur $num"
            SPANISH -> "$flagEmoji Interlocutor $num"
            KOREAN -> "$flagEmoji 대화자 $num"
            CHINESE -> "$flagEmoji 对话者 $num"
        }
    }

    fun getSpeakerButtonTitle(index: Int): String {
        return "$flagEmoji $nativeName"
    }

    fun getSpeakerButtonAction(index: Int): String {
        return when (this) {
            RUSSIAN -> "Говорить"
            ENGLISH -> "Speak"
            FRENCH -> "Parler"
            SPANISH -> "Hablar"
            KOREAN -> "말하기"
            CHINESE -> "说话"
        }
    }

    fun getSpeakerActiveAction(): String {
        return when (this) {
            RUSSIAN -> "● Идет запись..."
            ENGLISH -> "● Recording..."
            FRENCH -> "● Enregistrement..."
            SPANISH -> "● Grabando..."
            KOREAN -> "● 녹음 중..."
            CHINESE -> "● 正在录音..."
        }
    }

    companion object {
        // Alias for backward compatibility if referenced
        val ENGLISH_UK: SupportedLanguage get() = ENGLISH

        fun fromCode(code: String): SupportedLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) || it.code.startsWith(code) }
                ?: RUSSIAN
        }

        /**
         * Determines if translation between source and target requires the Semantic English Bridge.
         * User rules:
         * 1. Direct translation between Russian, French, Spanish and English.
         * 2. Direct translation between Russian, French, Spanish with each other.
         * 3. English translates directly to ANY of the supported languages.
         * 4. Russian, French, Spanish to/from Korean and Chinese go ONLY through Semantic English Bridge.
         * 5. Korean <-> Chinese also goes through Semantic English Bridge.
         */
        fun requiresEnglishBridge(source: SupportedLanguage, target: SupportedLanguage): Boolean {
            if (source == target) return false
            // English translates directly to and from all languages
            if (source == ENGLISH || target == ENGLISH) return false

            val directGroup = setOf(RUSSIAN, FRENCH, SPANISH)
            val asianBridgeGroup = setOf(KOREAN, CHINESE)

            // Direct group to direct group: direct translation
            if (source in directGroup && target in directGroup) return false

            // Direct group to/from Asian bridge group: Semantic English Bridge
            if (source in directGroup && target in asianBridgeGroup) return true
            if (source in asianBridgeGroup && target in directGroup) return true

            // Between Asian languages (Korean <-> Chinese): Semantic English Bridge
            if (source in asianBridgeGroup && target in asianBridgeGroup) return true

            return false
        }

        /**
         * Formatted routing string, e.g. "RU ➔ ES" or "RU ➔ EN ➔ KO"
         */
        fun getRouteDescription(source: SupportedLanguage, target: SupportedLanguage): String {
            return if (requiresEnglishBridge(source, target)) {
                "${source.code.uppercase()} ➔ ${ENGLISH.code.uppercase()} ➔ ${target.code.uppercase()}"
            } else {
                "${source.code.uppercase()} ➔ ${target.code.uppercase()}"
            }
        }

        /**
         * Returns an error message strictly in the SOURCE language from which text is being translated.
         */
        fun getTranslationErrorMessage(sourceLang: SupportedLanguage): String {
            return when (sourceLang) {
                RUSSIAN -> "Ошибка перевода: не удалось выполнить перевод на выбранный язык."
                ENGLISH, ENGLISH_UK -> "Translation error: could not translate to the selected language."
                FRENCH -> "Erreur de traduction : impossible de traduire vers la langue sélectionnée."
                SPANISH -> "Error de traducción: no se pudo traducir al idioma seleccionado."
                KOREAN -> "번역 오류: 선택한 언어로 번역할 수 없습니다."
                CHINESE -> "翻译错误：无法翻译为所选语言。"
            }
        }
    }
}

data class OfflineModelPack(
    val language: SupportedLanguage,
    val name: String,
    val nativeName: String,
    val sizeMb: Int,
    val isCoreBridge: Boolean,
    val isBridgeDependent: Boolean,
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val description: String
)

data class LanguagePair(
    val source: SupportedLanguage,
    val target: SupportedLanguage,
    val title: String,
    val description: String,
    val isBridge: Boolean = false
)
