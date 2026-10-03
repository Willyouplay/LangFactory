package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.DictionaryEntryEntity
import com.example.data.local.entity.ModelWeightDeltaEntity
import com.example.data.local.entity.TrainingLogEntity
import com.example.data.local.entity.TranslationHistoryEntity
import com.example.data.repository.TranslatorRepository
import com.example.engine.OfflineNeuralEngine
import com.example.engine.TranslationResult
import com.example.model.OfflineModelPack
import com.example.model.SupportedLanguage
import com.example.speech.VoiceSpeakerHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    ONBOARDING,
    HOME,
    TRANSLATE,
    HISTORY,
    NEURAL_TRAINING,
    DICTIONARY,
    SETTINGS
}

data class SpeechSubtitle(
    val id: Long = System.currentTimeMillis(),
    val speakerIndex: Int,
    val speakerName: String,
    val sourceLang: SupportedLanguage,
    val targetLang: SupportedLanguage,
    val originalText: String,
    val translatedText: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AppThemeMode(val title: String) {
    LIGHT("Светлая"),
    DARK("Тёмная"),
    SYSTEM("Системная")
}

data class TrainingUiState(
    val isTraining: Boolean = false,
    val currentEpoch: Int = 0,
    val totalEpochs: Int = 5,
    val currentLoss: Float = 1.45f,
    val initialLoss: Float = 1.45f,
    val finalLoss: Float = 0.0f,
    val processedSamples: Int = 0,
    val totalSamples: Int = 0,
    val statusMessage: String = "Готов к локальному обучению",
    val completedResult: String? = null
)

class TranslatorViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("langfactory_prefs", Context.MODE_PRIVATE)

    private val database = AppDatabase.getInstance(application)
    private val neuralEngine = OfflineNeuralEngine(database.translatorDao())
    private val repository = TranslatorRepository(database.translatorDao(), neuralEngine)
    private val geminiEngine = com.example.engine.gemini.GeminiNeuralEngine()
    val voiceHelper = VoiceSpeakerHelper(application)

    // --- Onboarding Persistence ---
    // User requested: "Стартовое окно должно открываться только при самом первом включении приложения,
    // при повторном входе оно не будет показываться, мы сделаем все необходимые настройки уже внутри приложения."
    private val _hasCompletedOnboarding = MutableStateFlow(
        prefs.getBoolean("has_completed_onboarding", false)
    )
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    // --- Navigation State ---
    private val _currentScreen = MutableStateFlow(
        if (_hasCompletedOnboarding.value) AppScreen.HOME else AppScreen.ONBOARDING
    )
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // --- App Interface Language (ru, en, ko) ---
    private val _appLanguage = MutableStateFlow(
        prefs.getString("app_ui_language", "ru") ?: "ru"
    )
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    fun setAppLanguage(langCode: String) {
        _appLanguage.value = langCode
        prefs.edit().putString("app_ui_language", langCode).apply()
    }

    // --- Industrial Settings State ---
    private val _factoryProfile = MutableStateFlow(
        prefs.getString("factory_profile", "Станкостроение и ЧПУ") ?: "Станкостроение и ЧПУ"
    )
    val factoryProfile: StateFlow<String> = _factoryProfile.asStateFlow()

    private val _noiseSuppressionEnabled = MutableStateFlow(
        prefs.getBoolean("noise_suppression", true)
    )
    val noiseSuppressionEnabled: StateFlow<Boolean> = _noiseSuppressionEnabled.asStateFlow()

    // --- Translation State with Pair Persistence ---
    // Rule: We remember the last pair for future launches. If not chosen, target can be unselected.
    private val _sourceLang = MutableStateFlow<SupportedLanguage?>(
        prefs.getString("last_source_lang", null)?.let { SupportedLanguage.fromCode(it) } ?: SupportedLanguage.RUSSIAN
    )
    val sourceLang: StateFlow<SupportedLanguage?> = _sourceLang.asStateFlow()

    private val _targetLang = MutableStateFlow<SupportedLanguage?>(
        prefs.getString("last_target_lang", null)?.let { SupportedLanguage.fromCode(it) } ?: SupportedLanguage.ENGLISH
    )
    val targetLang: StateFlow<SupportedLanguage?> = _targetLang.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _translatedText = MutableStateFlow("")
    val translatedText: StateFlow<String> = _translatedText.asStateFlow()

    private val _translationConfidence = MutableStateFlow(0.98f)
    val translationConfidence: StateFlow<Float> = _translationConfidence.asStateFlow()

    private val _appliedGlossariesCount = MutableStateFlow(2)
    val appliedGlossariesCount: StateFlow<Int> = _appliedGlossariesCount.asStateFlow()

    private val _appliedDeltasCount = MutableStateFlow(1)
    val appliedDeltasCount: StateFlow<Int> = _appliedDeltasCount.asStateFlow()

    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()

    // --- Semantic Bridge State ---
    private val _isBridgeTranslation = MutableStateFlow(
        valSrc() != null && valTgt() != null && SupportedLanguage.requiresEnglishBridge(valSrc()!!, valTgt()!!)
    )
    val isBridgeTranslation: StateFlow<Boolean> = _isBridgeTranslation.asStateFlow()

    private fun valSrc(): SupportedLanguage? = _sourceLang.value
    private fun valTgt(): SupportedLanguage? = _targetLang.value

    private val _bridgeIntermediateText = MutableStateFlow<String?>(null)
    val bridgeIntermediateText: StateFlow<String?> = _bridgeIntermediateText.asStateFlow()

    private val _bridgeAutoDownloadNotice = MutableStateFlow<String?>(null)
    val bridgeAutoDownloadNotice: StateFlow<String?> = _bridgeAutoDownloadNotice.asStateFlow()

    fun dismissBridgeNotice() {
        _bridgeAutoDownloadNotice.value = null
    }

    // --- Offline Language Model Packs Manager ---
    private val _offlineModelPacks = MutableStateFlow(loadInitialModelPacks())
    val offlineModelPacks: StateFlow<List<OfflineModelPack>> = _offlineModelPacks.asStateFlow()

    val downloadedLanguages: StateFlow<List<SupportedLanguage>> = _offlineModelPacks
        .map { packs -> packs.filter { it.isDownloaded }.map { it.language } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(SupportedLanguage.RUSSIAN, SupportedLanguage.ENGLISH, SupportedLanguage.FRENCH, SupportedLanguage.SPANISH, SupportedLanguage.KOREAN))

    private fun loadInitialModelPacks(): List<OfflineModelPack> = listOf(
        OfflineModelPack(
            language = SupportedLanguage.RUSSIAN,
            name = "Русский промышленный пакет",
            nativeName = "Русский (RU)",
            sizeMb = 42,
            isCoreBridge = false,
            isBridgeDependent = false,
            isDownloaded = prefs.getBoolean("model_dl_ru", true),
            description = "Прямой перевод с EN, FR, ES. Базовая терминология ГОСТ и станков."
        ),
        OfflineModelPack(
            language = SupportedLanguage.ENGLISH,
            name = "Английский промышленный пакет",
            nativeName = "English (EN)",
            sizeMb = 58,
            isCoreBridge = true,
            isBridgeDependent = false,
            isDownloaded = prefs.getBoolean("model_dl_en", true),
            description = "Центральное смысловое ядро. Переводит напрямую на все языки и служит мостом."
        ),
        OfflineModelPack(
            language = SupportedLanguage.FRENCH,
            name = "Французский промышленный пакет",
            nativeName = "Français (FR)",
            sizeMb = 38,
            isCoreBridge = false,
            isBridgeDependent = false,
            isDownloaded = prefs.getBoolean("model_dl_fr", true),
            description = "Прямой перевод с RU, ES и EN; промышленная механика и регламенты."
        ),
        OfflineModelPack(
            language = SupportedLanguage.SPANISH,
            name = "Испанский промышленный пакет",
            nativeName = "Español (ES)",
            sizeMb = 36,
            isCoreBridge = false,
            isBridgeDependent = false,
            isDownloaded = prefs.getBoolean("model_dl_es", true),
            description = "Прямой перевод с RU, FR и EN; технологические и энергетические протоколы."
        ),
        OfflineModelPack(
            language = SupportedLanguage.KOREAN,
            name = "Корейский промышленный пакет",
            nativeName = "한국어 (KO)",
            sizeMb = 48,
            isCoreBridge = false,
            isBridgeDependent = true,
            isDownloaded = prefs.getBoolean("model_dl_ko", true),
            description = "Паспорта Doosan, Hyundai WIA. Требует Английский мост (EN)."
        ),
        OfflineModelPack(
            language = SupportedLanguage.CHINESE,
            name = "Китайский промышленный пакет",
            nativeName = "中文 (ZH)",
            sizeMb = 52,
            isCoreBridge = false,
            isBridgeDependent = true,
            isDownloaded = prefs.getBoolean("model_dl_zh", false),
            description = "Станкостроение, логистика и документация КНР. Требует Английский мост (EN)."
        )
    )

    fun isModelDownloaded(lang: SupportedLanguage): Boolean {
        return _offlineModelPacks.value.firstOrNull { it.language == lang }?.isDownloaded ?: false
    }

    fun getMissingModelsForPair(source: SupportedLanguage?, target: SupportedLanguage?): List<SupportedLanguage> {
        if (source == null || target == null) return emptyList()
        val missing = mutableListOf<SupportedLanguage>()
        if (!isModelDownloaded(source)) missing.add(source)
        if (!isModelDownloaded(target)) missing.add(target)
        if (SupportedLanguage.requiresEnglishBridge(source, target) && !isModelDownloaded(SupportedLanguage.ENGLISH)) {
            missing.add(SupportedLanguage.ENGLISH)
        }
        return missing.distinct()
    }

    fun downloadMissingModelsForCurrentPair() {
        val s = _sourceLang.value ?: return
        val t = _targetLang.value ?: return
        val missing = getMissingModelsForPair(s, t)
        missing.forEach { lang ->
            downloadModelPack(lang)
        }
    }

    fun downloadAllModels() {
        viewModelScope.launch {
            if (!isModelDownloaded(SupportedLanguage.ENGLISH)) {
                downloadModelPack(SupportedLanguage.ENGLISH)
                delay(300)
            }
            val pending = _offlineModelPacks.value.filter { !it.isDownloaded && it.language != SupportedLanguage.ENGLISH }
            for (pack in pending) {
                downloadModelPack(pack.language)
                delay(200)
            }
        }
    }

    fun downloadModelPack(targetLangToDownload: SupportedLanguage) {
        viewModelScope.launch {
            val packs = _offlineModelPacks.value.toMutableList()
            val packIndex = packs.indexOfFirst { it.language == targetLangToDownload }
            if (packIndex == -1) return@launch

            val targetPack = packs[packIndex]
            val isBridgeDep = targetPack.isBridgeDependent
            val englishBridgeDownloaded = isModelDownloaded(SupportedLanguage.ENGLISH)

            // User requirement: "При выборе, например, тех языков, которые идут через промежуточный мост
            // автоматически будет скачиваться и промежуточная языковая модель."
            if (isBridgeDep && !englishBridgeDownloaded) {
                val enIndex = packs.indexOfFirst { it.language == SupportedLanguage.ENGLISH }
                if (enIndex != -1) {
                    _bridgeAutoDownloadNotice.value = "Для ${targetPack.nativeName} требуется смысловой мост: English (EN) скачивается автоматически."
                    // 1. Download English Bridge pack first
                    packs[enIndex] = packs[enIndex].copy(isDownloading = true, downloadProgress = 0.1f)
                    _offlineModelPacks.value = packs.toList()

                    for (p in listOf(0.35f, 0.70f, 0.95f, 1.0f)) {
                        delay(220)
                        packs[enIndex] = packs[enIndex].copy(downloadProgress = p)
                        _offlineModelPacks.value = packs.toList()
                    }
                    packs[enIndex] = packs[enIndex].copy(isDownloading = false, isDownloaded = true, downloadProgress = 1.0f)
                    prefs.edit().putBoolean("model_dl_en", true).apply()
                    _offlineModelPacks.value = packs.toList()
                }
            }

            // 2. Download requested language model
            packs[packIndex] = packs[packIndex].copy(isDownloading = true, downloadProgress = 0.1f)
            _offlineModelPacks.value = packs.toList()

            for (p in listOf(0.35f, 0.7f, 0.95f, 1.0f)) {
                delay(250)
                packs[packIndex] = packs[packIndex].copy(downloadProgress = p)
                _offlineModelPacks.value = packs.toList()
            }

            packs[packIndex] = packs[packIndex].copy(isDownloading = false, isDownloaded = true, downloadProgress = 1.0f)
            prefs.edit().putBoolean("model_dl_${targetLangToDownload.code.take(2).lowercase()}", true).apply()
            _offlineModelPacks.value = packs.toList()

            triggerTranslation()
        }
    }

    fun deleteModelPack(language: SupportedLanguage) {
        val packs = _offlineModelPacks.value.toMutableList()
        val index = packs.indexOfFirst { it.language == language }
        if (index != -1) {
            packs[index] = packs[index].copy(isDownloaded = false, downloadProgress = 0f, isDownloading = false)
            prefs.edit().putBoolean("model_dl_${language.code.take(2).lowercase()}", false).apply()
            _offlineModelPacks.value = packs.toList()
        }
    }

    // --- Home Quick Translation State ---
    private val _homeInputText = MutableStateFlow("")
    val homeInputText: StateFlow<String> = _homeInputText.asStateFlow()

    private val _homeTranslatedText = MutableStateFlow("")
    val homeTranslatedText: StateFlow<String> = _homeTranslatedText.asStateFlow()

    // --- Voice Recording State (Shop Floor Walkie-Talkie) ---
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _waveformPlaying = MutableStateFlow(false)
    val waveformPlaying: StateFlow<Boolean> = _waveformPlaying.asStateFlow()

    // --- Training Studio State ---
    private val _trainingUiState = MutableStateFlow(TrainingUiState())
    val trainingUiState: StateFlow<TrainingUiState> = _trainingUiState.asStateFlow()

    private val _selectedEpochs = MutableStateFlow(5)
    val selectedEpochs: StateFlow<Int> = _selectedEpochs.asStateFlow()

    private val _learningRate = MutableStateFlow(0.05f)
    val learningRate: StateFlow<Float> = _learningRate.asStateFlow()

    private val _showFactoryResetDialog = MutableStateFlow(false)
    val showFactoryResetDialog: StateFlow<Boolean> = _showFactoryResetDialog.asStateFlow()

    // --- Database Flows ---
    val dictionaryEntries: StateFlow<List<DictionaryEntryEntity>> = repository.getAllDictionaryEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val translationHistory: StateFlow<List<TranslationHistoryEntity>> = repository.getTranslationHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val learnedDeltasCount: StateFlow<Int> = repository.getWeightDeltasCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val learnedDeltasList: StateFlow<List<ModelWeightDeltaEntity>> = repository.getAllWeightDeltas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trainingLogs: StateFlow<List<TrainingLogEntity>> = repository.getTrainingLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var translateDebounceJob: Job? = null
    private var translateHomeDebounceJob: Job? = null
    private var activeTranslateJob: Job? = null

    init {
        // Pre-populate with essential industrial seed glossaries if DB is empty
        viewModelScope.launch {
            val count = repository.getInitialDeltasCount()
            if (count == 0) {
                repository.insertDictionaryEntry(
                    DictionaryEntryEntity(
                        sourceLang = "ko",
                        targetLang = "ru",
                        sourceWord = "CNC 공작기계",
                        targetWord = "Станок с ЧПУ",
                        category = "Станкостроение",
                        priorityWeight = 3.5f
                    )
                )
                repository.insertDictionaryEntry(
                    DictionaryEntryEntity(
                        sourceLang = "ko",
                        targetLang = "ru",
                        sourceWord = "비상 정지",
                        targetWord = "Аварийный останов",
                        category = "Охрана труда и ТБ",
                        priorityWeight = 4.0f
                    )
                )
                repository.insertDictionaryEntry(
                    DictionaryEntryEntity(
                        sourceLang = "ko",
                        targetLang = "ru",
                        sourceWord = "유압",
                        targetWord = "Гидравлика",
                        category = "Гидравлика",
                        priorityWeight = 3.0f
                    )
                )
            }

            val history = repository.getTranslationHistory().firstOrNull() ?: emptyList()
            val latest = history.firstOrNull()
            if (latest != null && latest.sourceText.isNotBlank()) {
                _inputText.value = latest.sourceText
                _sourceLang.value = SupportedLanguage.fromCode(latest.sourceLang)
                _targetLang.value = SupportedLanguage.fromCode(latest.targetLang)
                triggerTranslation()
            } else {
                _inputText.value = ""
                _translatedText.value = ""
            }
        }
    }

    fun completeOnboarding() {
        prefs.edit().putBoolean("has_completed_onboarding", true).apply()
        _hasCompletedOnboarding.value = true
        _currentScreen.value = AppScreen.HOME
    }

    fun resetOnboardingForDemonstration() {
        prefs.edit().putBoolean("has_completed_onboarding", false).apply()
        _hasCompletedOnboarding.value = false
        _currentScreen.value = AppScreen.ONBOARDING
    }

    fun setFactoryProfile(profile: String) {
        _factoryProfile.value = profile
        prefs.edit().putString("factory_profile", profile).apply()
    }

    fun toggleNoiseSuppression() {
        val newVal = !_noiseSuppressionEnabled.value
        _noiseSuppressionEnabled.value = newVal
        prefs.edit().putBoolean("noise_suppression", newVal).apply()
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setSourceLang(lang: SupportedLanguage) {
        if (_isOfflineModelMode.value && !isModelDownloaded(lang)) {
            return
        }
        val currentTarget = _targetLang.value
        if (currentTarget == lang) {
            // Cannot translate to the same language! Automatically pick the last target used for this source:
            val lastTarget = getLastTargetForSource(lang)
            if (lastTarget != null && lastTarget != lang && (!_isOfflineModelMode.value || isModelDownloaded(lastTarget))) {
                _targetLang.value = lastTarget
            } else {
                // If absent, language is not selected at all and user must choose manually
                _targetLang.value = null
            }
        }
        _sourceLang.value = lang
        savePair(_sourceLang.value, _targetLang.value)
        triggerTranslation()
    }

    fun setTargetLang(lang: SupportedLanguage?) {
        if (lang != null && _isOfflineModelMode.value && !isModelDownloaded(lang)) {
            return
        }
        val currentSource = _sourceLang.value
        if (lang != null && currentSource == lang) {
            val lastSource = getLastSourceForTarget(lang)
            if (lastSource != null && lastSource != lang && (!_isOfflineModelMode.value || isModelDownloaded(lastSource))) {
                _sourceLang.value = lastSource
            } else {
                _sourceLang.value = null
            }
        }
        _targetLang.value = lang
        savePair(_sourceLang.value, _targetLang.value)
        triggerTranslation()
    }

    private fun getLastTargetForSource(source: SupportedLanguage): SupportedLanguage? {
        val code = prefs.getString("last_target_for_source_${source.code}", null) ?: return null
        return SupportedLanguage.fromCode(code)
    }

    private fun getLastSourceForTarget(target: SupportedLanguage): SupportedLanguage? {
        val code = prefs.getString("last_source_for_target_${target.code}", null) ?: return null
        return SupportedLanguage.fromCode(code)
    }

    private fun savePair(source: SupportedLanguage?, target: SupportedLanguage?) {
        val editor = prefs.edit()
        if (source != null) {
            editor.putString("last_source_lang", source.code)
        } else {
            editor.remove("last_source_lang")
        }
        if (target != null) {
            editor.putString("last_target_lang", target.code)
        } else {
            editor.remove("last_target_lang")
        }
        if (source != null && target != null) {
            editor.putString("last_target_for_source_${source.code}", target.code)
            editor.putString("last_source_for_target_${target.code}", source.code)
        }
        editor.apply()
    }

    fun swapLanguages() {
        val s = _sourceLang.value ?: return
        val t = _targetLang.value ?: return
        _sourceLang.value = t
        _targetLang.value = s

        // Check if current translated text is an error message
        val currentTranslated = _translatedText.value.trim()
        val isErrorMessage = SupportedLanguage.entries.any {
            currentTranslated == SupportedLanguage.getTranslationErrorMessage(it).trim()
        } || currentTranslated.startsWith("Ошибка") || currentTranslated.startsWith("Error") || currentTranslated.startsWith("Translation error")

        if (isErrorMessage) {
            // Do not copy error message into input
            _translatedText.value = ""
        } else if (currentTranslated.isNotEmpty()) {
            val prevInput = _inputText.value
            _inputText.value = currentTranslated
            _translatedText.value = prevInput
        } else {
            // No translation yet: retain input so it translates in swapped direction
            _translatedText.value = ""
        }

        // Also sync home inputs if active
        val currentHomeTrans = _homeTranslatedText.value.trim()
        val isHomeError = SupportedLanguage.entries.any {
            currentHomeTrans == SupportedLanguage.getTranslationErrorMessage(it).trim()
        } || currentHomeTrans.startsWith("Ошибка") || currentHomeTrans.startsWith("Error")

        if (isHomeError) {
            _homeTranslatedText.value = ""
        } else if (currentHomeTrans.isNotEmpty()) {
            val prevHomeInput = _homeInputText.value
            _homeInputText.value = currentHomeTrans
            _homeTranslatedText.value = prevHomeInput
        } else {
            _homeTranslatedText.value = ""
        }

        savePair(_sourceLang.value, _targetLang.value)
        triggerTranslation()
    }

    fun setPair(source: SupportedLanguage?, target: SupportedLanguage?, sampleText: String? = null) {
        _sourceLang.value = source
        _targetLang.value = target
        if (sampleText != null) {
            _inputText.value = sampleText
        }
        savePair(source, target)
        triggerTranslation()
    }

    fun onInputTextChanged(newText: String) {
        _inputText.value = newText
        translateDebounceJob?.cancel()
        activeTranslateJob?.cancel()

        if (newText.isBlank()) {
            _translatedText.value = ""
            _bridgeIntermediateText.value = null
            _isTranslating.value = false
            return
        }

        translateDebounceJob = viewModelScope.launch {
            delay(350) // Smooth debounce for neural matrix
            triggerTranslation()
        }
    }

    fun onHomeInputTextChanged(newText: String) {
        _homeInputText.value = newText
        translateHomeDebounceJob?.cancel()
        if (newText.isBlank()) {
            _homeTranslatedText.value = ""
            _bridgeIntermediateText.value = null
            return
        }

        val source = _sourceLang.value
        val target = _targetLang.value
        if (source == null || target == null) {
            _homeTranslatedText.value = ""
            _bridgeIntermediateText.value = null
            return
        }

        translateHomeDebounceJob = viewModelScope.launch {
            delay(350) // Debounce rapid keystrokes to preserve quota and bandwidth
            if (!_isOfflineModelMode.value) {
                // Online Gemini translation with offline fallback
                when (val res = geminiEngine.translate(newText, source, target)) {
                    is com.example.engine.gemini.GeminiTranslationResult.Success -> {
                        _homeTranslatedText.value = res.translatedText
                        _isBridgeTranslation.value = SupportedLanguage.requiresEnglishBridge(source, target)
                        _bridgeIntermediateText.value = res.intermediateText
                    }
                    is com.example.engine.gemini.GeminiTranslationResult.Error -> {
                        val offlineRes = repository.translateOffline(newText, source, target)
                        _homeTranslatedText.value = offlineRes.translatedText
                        _isBridgeTranslation.value = offlineRes.isBridgeTranslation
                        _bridgeIntermediateText.value = offlineRes.bridgeIntermediateText
                    }
                }
            } else {
                val res = repository.translateOffline(newText, source, target)
                _homeTranslatedText.value = res.translatedText
                _isBridgeTranslation.value = res.isBridgeTranslation
                _bridgeIntermediateText.value = res.bridgeIntermediateText
            }
        }
    }

    fun triggerTranslation() {
        val text = _inputText.value
        if (text.isBlank()) {
            activeTranslateJob?.cancel()
            _translatedText.value = ""
            _bridgeIntermediateText.value = null
            _isTranslating.value = false
            return
        }

        val source = _sourceLang.value
        val target = _targetLang.value

        if (source == null || target == null) {
            val sLang = source ?: SupportedLanguage.RUSSIAN
            _translatedText.value = SupportedLanguage.getTranslationErrorMessage(sLang)
            _isBridgeTranslation.value = false
            _bridgeIntermediateText.value = null
            return
        }

        activeTranslateJob?.cancel()
        activeTranslateJob = viewModelScope.launch {
            _isTranslating.value = true
            try {
                if (_inputText.value.isBlank()) {
                    _translatedText.value = ""
                    _bridgeIntermediateText.value = null
                    return@launch
                }

                if (!_isOfflineModelMode.value) {
                    // Online Translation with Gemini Neural MT
                    when (val res = geminiEngine.translate(text, source, target)) {
                        is com.example.engine.gemini.GeminiTranslationResult.Success -> {
                            if (_inputText.value.isBlank()) {
                                _translatedText.value = ""
                                _bridgeIntermediateText.value = null
                                return@launch
                            }
                            _translatedText.value = res.translatedText
                            _translationConfidence.value = 0.99f
                            _appliedGlossariesCount.value = 0
                            _appliedDeltasCount.value = 0
                            _isBridgeTranslation.value = SupportedLanguage.requiresEnglishBridge(source, target)
                            _bridgeIntermediateText.value = res.intermediateText
                            // Save to Room history
                            repository.insertHistoryItem(
                                TranslationHistoryEntity(
                                    sourceLang = source.code,
                                    targetLang = target.code,
                                    sourceText = text,
                                    translatedText = res.translatedText,
                                    timestamp = System.currentTimeMillis(),
                                    isOfflineEngine = false
                                )
                            )
                        }
                        is com.example.engine.gemini.GeminiTranslationResult.Error -> {
                            // Fallback to offline on-device neural engine
                            val offlineRes = repository.translateOffline(text, source, target)
                            if (_inputText.value.isBlank()) {
                                _translatedText.value = ""
                                _bridgeIntermediateText.value = null
                                return@launch
                            }
                            _translatedText.value = offlineRes.translatedText
                            _translationConfidence.value = offlineRes.confidence
                            _appliedGlossariesCount.value = offlineRes.appliedGlossariesCount
                            _appliedDeltasCount.value = offlineRes.appliedNeuralDeltasCount
                            _isBridgeTranslation.value = offlineRes.isBridgeTranslation
                            _bridgeIntermediateText.value = offlineRes.bridgeIntermediateText
                            // Save fallback translation to Room history
                            repository.insertHistoryItem(
                                TranslationHistoryEntity(
                                    sourceLang = source.code,
                                    targetLang = target.code,
                                    sourceText = text,
                                    translatedText = offlineRes.translatedText,
                                    timestamp = System.currentTimeMillis(),
                                    isOfflineEngine = true
                                )
                            )
                        }
                    }
                } else {
                    // Offline On-Device Neural Engine
                    val res = repository.translateOffline(text, source, target)
                    if (_inputText.value.isBlank()) {
                        _translatedText.value = ""
                        _bridgeIntermediateText.value = null
                        return@launch
                    }
                    _translatedText.value = res.translatedText
                    _translationConfidence.value = res.confidence
                    _appliedGlossariesCount.value = res.appliedGlossariesCount
                    _appliedDeltasCount.value = res.appliedNeuralDeltasCount
                    _isBridgeTranslation.value = res.isBridgeTranslation
                    _bridgeIntermediateText.value = res.bridgeIntermediateText
                    // Save to Room history
                    repository.insertHistoryItem(
                        TranslationHistoryEntity(
                            sourceLang = source.code,
                            targetLang = target.code,
                            sourceText = text,
                            translatedText = res.translatedText,
                            timestamp = System.currentTimeMillis(),
                            isOfflineEngine = true
                        )
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Cancellation must never produce a translation error
                throw e
            } catch (e: Exception) {
                if (_inputText.value.isNotBlank()) {
                    _translatedText.value = SupportedLanguage.getTranslationErrorMessage(source)
                } else {
                    _translatedText.value = ""
                }
                _isBridgeTranslation.value = false
                _bridgeIntermediateText.value = null
            } finally {
                _isTranslating.value = false
            }
        }
    }

    // --- Voice & TTS Functions ---
    fun speakSource() {
        _sourceLang.value?.let { voiceHelper.speak(_inputText.value, it) }
    }

    fun speakTarget() {
        _targetLang.value?.let { voiceHelper.speak(_translatedText.value, it) }
    }

    fun speakIntermediate() {
        val intermediate = _bridgeIntermediateText.value
        if (!intermediate.isNullOrBlank()) {
            voiceHelper.speak(intermediate, SupportedLanguage.ENGLISH)
        }
    }

    fun toggleVoiceListening() {
        if (_isListening.value) {
            _isListening.value = false
            _waveformPlaying.value = false
        } else {
            _isListening.value = true
            _waveformPlaying.value = true
            // Industrial radio communication capture simulation
            viewModelScope.launch {
                delay(1800)
                if (_isListening.value) {
                    val samplePhrases = when (_sourceLang.value) {
                        SupportedLanguage.KOREAN -> "비상 정지 버튼을 누르고 유압 펌프 압력을 확인하십시오."
                        SupportedLanguage.RUSSIAN -> "Внимание цеху: снизить обороты шпинделя и проверить подачу СОЖ."
                        SupportedLanguage.FRENCH -> "Arrêt d'urgence déclenché. Contrôlez immédiatement la pression."
                        SupportedLanguage.SPANISH -> "Parada de emergencia activada. Verifique la presión hidráulica."
                        SupportedLanguage.CHINESE -> "紧急停机并检查主轴冷却系统。"
                        else -> "Emergency stop triggered. Check spindle pressure immediately."
                    }
                    _inputText.value = samplePhrases
                    _homeInputText.value = samplePhrases
                    _isListening.value = false
                    _waveformPlaying.value = false
                    triggerTranslation()
                }
            }
        }
    }

    fun toggleWaveformPlay() {
        _waveformPlaying.value = !_waveformPlaying.value
        if (_waveformPlaying.value) {
            speakTarget()
        } else {
            voiceHelper.stop()
        }
    }

    // --- Speech-to-Speech (Голос в голос) с субтитрами через Gemini API ---
    private val _speechSubtitles = MutableStateFlow<List<SpeechSubtitle>>(
        listOf(
            SpeechSubtitle(
                id = 1L,
                speakerIndex = 1,
                speakerName = "Собеседник 1",
                sourceLang = SupportedLanguage.RUSSIAN,
                targetLang = SupportedLanguage.ENGLISH,
                originalText = "Внимание цеху: снизить обороты шпинделя и проверить подачу СОЖ.",
                translatedText = "Attention shop floor: reduce spindle speed and check coolant supply."
            ),
            SpeechSubtitle(
                id = 2L,
                speakerIndex = 2,
                speakerName = "Собеседник 2",
                sourceLang = SupportedLanguage.ENGLISH,
                targetLang = SupportedLanguage.RUSSIAN,
                originalText = "Spindle speed reduced to 1200 RPM, coolant pressure is nominal.",
                translatedText = "Обороты снижены до 1200 об/мин, давление СОЖ в норме."
            )
        )
    )
    val speechSubtitles: StateFlow<List<SpeechSubtitle>> = _speechSubtitles.asStateFlow()

    private val _speechActiveSpeaker = MutableStateFlow<Int?>(null)
    val speechActiveSpeaker: StateFlow<Int?> = _speechActiveSpeaker.asStateFlow()

    private val _isSpeechProcessing = MutableStateFlow(false)
    val isSpeechProcessing: StateFlow<Boolean> = _isSpeechProcessing.asStateFlow()

    private val _currentLiveSubtitle = MutableStateFlow<SpeechSubtitle?>(null)
    val currentLiveSubtitle: StateFlow<SpeechSubtitle?> = _currentLiveSubtitle.asStateFlow()

    fun triggerSpeechToSpeech(speakerIndex: Int, customPhrase: String? = null) {
        val sLang = _sourceLang.value ?: SupportedLanguage.RUSSIAN
        val tLang = _targetLang.value ?: SupportedLanguage.ENGLISH
        val speakerSrcLang = if (speakerIndex == 1) sLang else tLang
        val speakerTgtLang = if (speakerIndex == 1) tLang else sLang

        val phrase = customPhrase ?: when (speakerSrcLang) {
            SupportedLanguage.RUSSIAN -> listOf(
                "Проверьте показания датчика вибрации на редукторе привода.",
                "Смена инструмента завершена, запускаем автоматический цикл ЧПУ.",
                "Температура подшипников шпинделя стабильна, начинаем фрезеровку."
            ).random()
            SupportedLanguage.ENGLISH -> listOf(
                "Hydraulic pressure stabilized at nominal level, ready for operation.",
                "Emergency stop protocol verified. Spindle calibration complete.",
                "Component tolerance within zero point zero two millimeters."
            ).random()
            SupportedLanguage.FRENCH -> listOf(
                "Vérifiez la pression hydraulique du module d'usinage principal.",
                "Arrêt d'urgence réinitialisé, reprise du protocole de production."
            ).random()
            SupportedLanguage.SPANISH -> listOf(
                "Compruebe la lubricación del cabezal de fresado antes de iniciar.",
                "Parámetros de corte verificados, procediendo con la secuencia."
            ).random()
            SupportedLanguage.KOREAN -> listOf(
                "메인 스핀들 냉각 펌프 압력을 확인하고 사이클을 시작하십시오.",
                "유압 센서 이상 없음, 작업 지침에 따라 가공을 재개합니다."
            ).random()
            SupportedLanguage.CHINESE -> listOf(
                "请检查主轴切削液压力并确认安全防护装置。",
                "数控机床换刀完成，准备启动自动加工程序。"
            ).random()
        }

        _speechActiveSpeaker.value = speakerIndex
        _isSpeechProcessing.value = true
        _waveformPlaying.value = true
        _currentLiveSubtitle.value = null // Окно самоочищается при начале записи

        viewModelScope.launch {
            try {
                // Speech recognition delay simulation for natural conversational turn
                delay(700)

                // Translate using Gemini API (or fallback if offline/error)
                val translationText = if (!_isOfflineModelMode.value) {
                    when (val res = geminiEngine.translate(phrase, speakerSrcLang, speakerTgtLang)) {
                        is com.example.engine.gemini.GeminiTranslationResult.Success -> res.translatedText
                        is com.example.engine.gemini.GeminiTranslationResult.Error -> {
                            val offRes = repository.translateOffline(phrase, speakerSrcLang, speakerTgtLang)
                            offRes.translatedText
                        }
                    }
                } else {
                    val offRes = repository.translateOffline(phrase, speakerSrcLang, speakerTgtLang)
                    offRes.translatedText
                }

                val subtitleItem = SpeechSubtitle(
                    id = System.currentTimeMillis(),
                    speakerIndex = speakerIndex,
                    speakerName = speakerSrcLang.getSpeakerLabel(speakerIndex),
                    sourceLang = speakerSrcLang,
                    targetLang = speakerTgtLang,
                    originalText = phrase,
                    translatedText = translationText
                )

                _currentLiveSubtitle.value = subtitleItem
                _speechSubtitles.value = _speechSubtitles.value + subtitleItem

                // Speech-to-Speech audio output: Speak translated speech aloud!
                voiceHelper.speak(translationText, speakerTgtLang)

                // Save to history
                repository.insertHistoryItem(
                    TranslationHistoryEntity(
                        sourceLang = speakerSrcLang.code,
                        targetLang = speakerTgtLang.code,
                        sourceText = phrase,
                        translatedText = translationText,
                        timestamp = System.currentTimeMillis(),
                        isOfflineEngine = _isOfflineModelMode.value
                    )
                )
            } catch (e: Exception) {
                // Ignore
            } finally {
                _isSpeechProcessing.value = false
                _speechActiveSpeaker.value = null
                _waveformPlaying.value = false
            }
        }
    }

    fun playSubtitleAudio(subtitle: SpeechSubtitle) {
        voiceHelper.speak(subtitle.translatedText, subtitle.targetLang)
    }

    fun playOriginalAudio(subtitle: SpeechSubtitle) {
        voiceHelper.speak(subtitle.originalText, subtitle.sourceLang)
    }

    fun clearSpeechSubtitles() {
        _speechSubtitles.value = emptyList()
        _currentLiveSubtitle.value = null
    }

    // --- On-Device Neural Training ---
    fun setSelectedEpochs(epochs: Int) {
        _selectedEpochs.value = epochs
    }

    fun setLearningRate(lr: Float) {
        _learningRate.value = lr
    }

    fun startLocalNeuralTraining() {
        if (_trainingUiState.value.isTraining) return

        viewModelScope.launch {
            _trainingUiState.value = TrainingUiState(
                isTraining = true,
                currentEpoch = 1,
                totalEpochs = _selectedEpochs.value,
                statusMessage = "Калибровка весов производственной нейросети..."
            )

            try {
                val result = repository.trainOfflineNeuralModel(
                    epochs = _selectedEpochs.value,
                    learningRate = _learningRate.value
                ) { progress ->
                    _trainingUiState.value = _trainingUiState.value.copy(
                        currentEpoch = progress.currentEpoch,
                        totalEpochs = progress.totalEpochs,
                        currentLoss = progress.currentLoss,
                        processedSamples = progress.sampleProcessed,
                        totalSamples = progress.totalSamples,
                        statusMessage = progress.statusMessage
                    )
                }

                _trainingUiState.value = _trainingUiState.value.copy(
                    isTraining = false,
                    finalLoss = result.finalLoss,
                    statusMessage = "Калибровка завершена! Обновлено ${result.updatedDeltasCount} нейронных весов.",
                    completedResult = "Модель откалибрована: ${result.newModelVersion} (Loss: ${"%.3f".format(result.finalLoss)})"
                )
                triggerTranslation()
            } catch (e: Exception) {
                _trainingUiState.value = _trainingUiState.value.copy(
                    isTraining = false,
                    statusMessage = "Ошибка обучения: ${e.localizedMessage}"
                )
            }
        }
    }

    // --- Factory Reset ---
    fun openFactoryResetDialog() {
        _showFactoryResetDialog.value = true
    }

    fun closeFactoryResetDialog() {
        _showFactoryResetDialog.value = false
    }

    fun performFactoryReset() {
        viewModelScope.launch {
            repository.factoryReset()
            _showFactoryResetDialog.value = false
            _trainingUiState.value = TrainingUiState(
                statusMessage = "Модель сброшена до заводских настроек. Все цеховые данные очищены."
            )
            _inputText.value = ""
            _translatedText.value = ""
            _homeInputText.value = ""
            _homeTranslatedText.value = ""
        }
    }

    // --- Custom Dictionary CRUD ---
    fun addDictionaryEntry(
        sourceWord: String,
        targetWord: String,
        sourceLangCode: String = _sourceLang.value?.code ?: "ru",
        targetLangCode: String = _targetLang.value?.code ?: "en",
        category: String = "Общий",
        priority: Float = 2.0f
    ) {
        if (sourceWord.isBlank() || targetWord.isBlank()) return
        viewModelScope.launch {
            repository.insertDictionaryEntry(
                DictionaryEntryEntity(
                    sourceLang = sourceLangCode,
                    targetLang = targetLangCode,
                    sourceWord = sourceWord.trim(),
                    targetWord = targetWord.trim(),
                    category = category.trim(),
                    priorityWeight = priority
                )
            )
            triggerTranslation()
        }
    }

    fun deleteDictionaryEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteDictionaryEntryById(id)
            triggerTranslation()
        }
    }

    // --- Model Mode & CSV Onboarding Support ---
    private val _isOfflineModelMode = MutableStateFlow(
        prefs.getBoolean("is_offline_model", true)
    )
    val isOfflineModelMode: StateFlow<Boolean> = _isOfflineModelMode.asStateFlow()

    fun setIsOfflineModelMode(offline: Boolean) {
        _isOfflineModelMode.value = offline
        prefs.edit().putBoolean("is_offline_model", offline).apply()
        if (offline) {
            val downloaded = _offlineModelPacks.value.filter { it.isDownloaded }.map { it.language }
            val currentSource = _sourceLang.value
            val currentTarget = _targetLang.value
            if (currentSource == null || !downloaded.contains(currentSource)) {
                _sourceLang.value = downloaded.firstOrNull { it != currentTarget } ?: SupportedLanguage.RUSSIAN
            }
            if (currentTarget == null || !downloaded.contains(currentTarget)) {
                _targetLang.value = downloaded.firstOrNull { it != _sourceLang.value } ?: SupportedLanguage.ENGLISH
            }
            savePair(_sourceLang.value, _targetLang.value)
        }
        triggerTranslation()
    }

    // --- App Theme Mode (Light, Dark, System) ---
    private val _themeMode = MutableStateFlow(
        try {
            AppThemeMode.valueOf(prefs.getString("app_theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("app_theme_mode", mode.name).apply()
    }

    fun openTranslateFromNavbar() {
        viewModelScope.launch {
            val historyList = repository.getTranslationHistory().firstOrNull() ?: emptyList()
            val latest = historyList.firstOrNull()
            if (latest != null && latest.sourceText.isNotBlank()) {
                _inputText.value = latest.sourceText
                _sourceLang.value = SupportedLanguage.fromCode(latest.sourceLang)
                _targetLang.value = SupportedLanguage.fromCode(latest.targetLang)
                triggerTranslation()
            } else {
                _inputText.value = ""
                _translatedText.value = ""
            }
            _currentScreen.value = AppScreen.TRANSLATE
        }
    }

    // --- History Management with Period Support (Guaranteed 3 days storage) ---
    fun clearHistoryByPeriod(period: HistoryClearPeriod) {
        viewModelScope.launch {
            when (period) {
                HistoryClearPeriod.ALL_TIME -> {
                    repository.clearHistory()
                    _inputText.value = ""
                    _translatedText.value = ""
                }
                HistoryClearPeriod.LAST_HOUR -> repository.clearHistorySince(System.currentTimeMillis() - 3600_000L)
                HistoryClearPeriod.TODAY -> {
                    val cal = java.util.Calendar.getInstance()
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    cal.set(java.util.Calendar.MINUTE, 0)
                    cal.set(java.util.Calendar.SECOND, 0)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    repository.clearHistorySince(cal.timeInMillis)
                }
                HistoryClearPeriod.OLDER_THAN_3_DAYS -> {
                    val threeDaysAgo = System.currentTimeMillis() - (3L * 86_400_000L)
                    repository.clearHistoryOlderThan(threeDaysAgo)
                }
            }
            val remaining = repository.getTranslationHistory().firstOrNull() ?: emptyList()
            val latest = remaining.firstOrNull()
            if (latest != null && latest.sourceText.isNotBlank()) {
                _inputText.value = latest.sourceText
            } else {
                _inputText.value = ""
                _translatedText.value = ""
            }
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteHistoryById(id)
            val remaining = repository.getTranslationHistory().firstOrNull() ?: emptyList()
            val latest = remaining.firstOrNull()
            if (latest != null && latest.sourceText.isNotBlank()) {
                _inputText.value = latest.sourceText
            } else {
                _inputText.value = ""
                _translatedText.value = ""
            }
        }
    }

    fun getCsvTemplate(): String = """
# Langfactory CSV Glossary Template
# Format: source_term,target_term,category,priority
CNC 공작기계,Станок с ЧПУ,Станкостроение,4.0
비상 정지,Аварийный останов,Охрана труда и ТБ,4.5
유압 펌프,Гидравлический насос,Гидравлика,3.5
스핀들 속도,Скорость шпинделя,Станкостроение,3.5
냉각수 압력,Давление подачи СОЖ,Обслуживание,3.0
공차,Допуск размера,Метрология,3.0
표면 거칠기,Шероховатость поверхности,Контроль качества,3.0
""".trimIndent()

    suspend fun importCsvGlossary(csvText: String, sourceLang: String = "ko", targetLang: String = "ru"): Int {
        var count = 0
        val lines = csvText.lines()
        for (line in lines) {
            val clean = line.trim()
            if (clean.isBlank() || clean.startsWith("#") || clean.startsWith("source")) continue
            val parts = clean.split(",", ";")
            if (parts.size >= 2) {
                val src = parts[0].trim()
                val tgt = parts[1].trim()
                val cat = if (parts.size >= 3) parts[2].trim() else "Импорт CSV"
                val prio = if (parts.size >= 4) parts[3].trim().toFloatOrNull() ?: 3.0f else 3.0f
                if (src.isNotEmpty() && tgt.isNotEmpty()) {
                    repository.insertDictionaryEntry(
                        DictionaryEntryEntity(
                            sourceLang = sourceLang,
                            targetLang = targetLang,
                            sourceWord = src,
                            targetWord = tgt,
                            category = cat,
                            priorityWeight = prio
                        )
                    )
                    count++
                }
            }
        }
        triggerTranslation()
        return count
    }

    fun preloadIndustrialCncPack(onLoaded: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val count = importCsvGlossary(getCsvTemplate(), "ko", "ru")
            onLoaded(count)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceHelper.release()
    }
}

enum class HistoryClearPeriod(val title: String) {
    LAST_HOUR("За последний час"),
    TODAY("За сегодня"),
    OLDER_THAN_3_DAYS("Старше 3 дней (сохранить 3 дня)"),
    ALL_TIME("За всё время")
}

