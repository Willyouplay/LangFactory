package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.SupportedLanguage
import com.example.viewmodel.TranslatorViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Langfactory", appName)
    }

    @Test
    fun `verify direct translation pairs do not require english bridge`() {
        // Direct between Russian, French, Spanish and English
        assertFalse(SupportedLanguage.requiresEnglishBridge(SupportedLanguage.RUSSIAN, SupportedLanguage.ENGLISH))
        assertFalse(SupportedLanguage.requiresEnglishBridge(SupportedLanguage.ENGLISH, SupportedLanguage.RUSSIAN))
        assertFalse(SupportedLanguage.requiresEnglishBridge(SupportedLanguage.FRENCH, SupportedLanguage.ENGLISH))
        assertFalse(SupportedLanguage.requiresEnglishBridge(SupportedLanguage.SPANISH, SupportedLanguage.ENGLISH))

        // Direct between Russian, French, Spanish with each other
        assertFalse(SupportedLanguage.requiresEnglishBridge(SupportedLanguage.RUSSIAN, SupportedLanguage.FRENCH))
        assertFalse(SupportedLanguage.requiresEnglishBridge(SupportedLanguage.FRENCH, SupportedLanguage.RUSSIAN))
        assertFalse(SupportedLanguage.requiresEnglishBridge(SupportedLanguage.RUSSIAN, SupportedLanguage.SPANISH))
        assertFalse(SupportedLanguage.requiresEnglishBridge(SupportedLanguage.SPANISH, SupportedLanguage.RUSSIAN))
        assertFalse(SupportedLanguage.requiresEnglishBridge(SupportedLanguage.FRENCH, SupportedLanguage.SPANISH))
        assertFalse(SupportedLanguage.requiresEnglishBridge(SupportedLanguage.SPANISH, SupportedLanguage.FRENCH))

        // Same language
        assertFalse(SupportedLanguage.requiresEnglishBridge(SupportedLanguage.RUSSIAN, SupportedLanguage.RUSSIAN))
    }

    @Test
    fun `verify english translates directly to any language`() {
        // English directly to/from Korean and Chinese
        assertFalse(SupportedLanguage.requiresEnglishBridge(SupportedLanguage.ENGLISH, SupportedLanguage.KOREAN))
        assertFalse(SupportedLanguage.requiresEnglishBridge(SupportedLanguage.KOREAN, SupportedLanguage.ENGLISH))
        assertFalse(SupportedLanguage.requiresEnglishBridge(SupportedLanguage.ENGLISH, SupportedLanguage.CHINESE))
        assertFalse(SupportedLanguage.requiresEnglishBridge(SupportedLanguage.CHINESE, SupportedLanguage.ENGLISH))
    }

    @Test
    fun `verify semantic english bridge is required for asian language pairs`() {
        // Russian to/from Korean and Chinese
        assertTrue(SupportedLanguage.requiresEnglishBridge(SupportedLanguage.RUSSIAN, SupportedLanguage.KOREAN))
        assertTrue(SupportedLanguage.requiresEnglishBridge(SupportedLanguage.KOREAN, SupportedLanguage.RUSSIAN))
        assertTrue(SupportedLanguage.requiresEnglishBridge(SupportedLanguage.RUSSIAN, SupportedLanguage.CHINESE))
        assertTrue(SupportedLanguage.requiresEnglishBridge(SupportedLanguage.CHINESE, SupportedLanguage.RUSSIAN))

        // French to/from Korean and Chinese
        assertTrue(SupportedLanguage.requiresEnglishBridge(SupportedLanguage.FRENCH, SupportedLanguage.KOREAN))
        assertTrue(SupportedLanguage.requiresEnglishBridge(SupportedLanguage.KOREAN, SupportedLanguage.FRENCH))
        assertTrue(SupportedLanguage.requiresEnglishBridge(SupportedLanguage.FRENCH, SupportedLanguage.CHINESE))
        assertTrue(SupportedLanguage.requiresEnglishBridge(SupportedLanguage.CHINESE, SupportedLanguage.FRENCH))

        // Spanish to/from Korean and Chinese
        assertTrue(SupportedLanguage.requiresEnglishBridge(SupportedLanguage.SPANISH, SupportedLanguage.KOREAN))
        assertTrue(SupportedLanguage.requiresEnglishBridge(SupportedLanguage.KOREAN, SupportedLanguage.SPANISH))
        assertTrue(SupportedLanguage.requiresEnglishBridge(SupportedLanguage.SPANISH, SupportedLanguage.CHINESE))
        assertTrue(SupportedLanguage.requiresEnglishBridge(SupportedLanguage.CHINESE, SupportedLanguage.SPANISH))

        // Between Asian languages (Korean <-> Chinese)
        assertTrue(SupportedLanguage.requiresEnglishBridge(SupportedLanguage.KOREAN, SupportedLanguage.CHINESE))
        assertTrue(SupportedLanguage.requiresEnglishBridge(SupportedLanguage.CHINESE, SupportedLanguage.KOREAN))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `verify 6 offline model packs loaded and bridge dependency handled in viewModel`() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = TranslatorViewModel(app)

        val packs = viewModel.offlineModelPacks.value
        assertEquals(6, packs.size)

        val expectedLangs = setOf(
            SupportedLanguage.RUSSIAN,
            SupportedLanguage.ENGLISH,
            SupportedLanguage.FRENCH,
            SupportedLanguage.SPANISH,
            SupportedLanguage.KOREAN,
            SupportedLanguage.CHINESE
        )
        assertEquals(expectedLangs, packs.map { it.language }.toSet())

        // English pack is marked as core bridge
        val enPack = packs.first { it.language == SupportedLanguage.ENGLISH }
        assertTrue(enPack.isCoreBridge)
        assertFalse(enPack.isBridgeDependent)

        // Korean and Chinese packs are marked as bridge dependent
        val koPack = packs.first { it.language == SupportedLanguage.KOREAN }
        assertTrue(koPack.isBridgeDependent)

        val zhPack = packs.first { it.language == SupportedLanguage.CHINESE }
        assertTrue(zhPack.isBridgeDependent)

        // Missing models for RU -> KO when EN is not downloaded includes EN
        viewModel.deleteModelPack(SupportedLanguage.ENGLISH)
        viewModel.deleteModelPack(SupportedLanguage.KOREAN)

        val missing = viewModel.getMissingModelsForPair(SupportedLanguage.RUSSIAN, SupportedLanguage.KOREAN)
        assertTrue(missing.contains(SupportedLanguage.KOREAN))
        assertTrue(missing.contains(SupportedLanguage.ENGLISH))
    }

    @Test
    fun `verify error messages are returned strictly in the source language`() {
        val ruError = SupportedLanguage.getTranslationErrorMessage(SupportedLanguage.RUSSIAN)
        assertTrue(ruError.contains("Ошибка перевода"))

        val enError = SupportedLanguage.getTranslationErrorMessage(SupportedLanguage.ENGLISH)
        assertTrue(enError.contains("Translation error"))

        val frError = SupportedLanguage.getTranslationErrorMessage(SupportedLanguage.FRENCH)
        assertTrue(frError.contains("Erreur de traduction"))

        val esError = SupportedLanguage.getTranslationErrorMessage(SupportedLanguage.SPANISH)
        assertTrue(esError.contains("Error de traducción"))

        val koError = SupportedLanguage.getTranslationErrorMessage(SupportedLanguage.KOREAN)
        assertTrue(koError.contains("번역 오류"))

        val zhError = SupportedLanguage.getTranslationErrorMessage(SupportedLanguage.CHINESE)
        assertTrue(zhError.contains("翻译错误"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `verify swapLanguages does not copy error message into input`() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = TranslatorViewModel(app)

        viewModel.setPair(SupportedLanguage.RUSSIAN, SupportedLanguage.KOREAN, "Неизвестное слово xyz")
        // Simulate error in translated text
        val ruError = SupportedLanguage.getTranslationErrorMessage(SupportedLanguage.RUSSIAN)
        // Accessing swapLanguages
        viewModel.swapLanguages()
        // Input should not be filled with the error text
        assertFalse(viewModel.inputText.value == ruError)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `verify swapLanguages properly exchanges language pair and triggers translation`() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = TranslatorViewModel(app)

        viewModel.setPair(SupportedLanguage.RUSSIAN, SupportedLanguage.ENGLISH, "Привет")
        assertEquals(SupportedLanguage.RUSSIAN, viewModel.sourceLang.value)
        assertEquals(SupportedLanguage.ENGLISH, viewModel.targetLang.value)

        viewModel.swapLanguages()

        assertEquals(SupportedLanguage.ENGLISH, viewModel.sourceLang.value)
        assertEquals(SupportedLanguage.RUSSIAN, viewModel.targetLang.value)
        // The input text is preserved for translation in the reverse direction
        assertEquals("Привет", viewModel.inputText.value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `verify clearing or deleting input text immediately resets translatedText with no error displayed`() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = TranslatorViewModel(app)

        viewModel.setPair(SupportedLanguage.RUSSIAN, SupportedLanguage.ENGLISH, "Привет")
        viewModel.onInputTextChanged("Привет")

        // User deletes all characters (backspace / clear button)
        viewModel.onInputTextChanged("")

        assertEquals("", viewModel.inputText.value)
        assertEquals("", viewModel.translatedText.value)
        assertFalse(viewModel.isTranslating.value)
        assertEquals(null, viewModel.bridgeIntermediateText.value)

        // Ensure no error message was produced when input is cleared
        val ruError = SupportedLanguage.getTranslationErrorMessage(SupportedLanguage.RUSSIAN)
        assertFalse(viewModel.translatedText.value.contains(ruError))
    }
}
