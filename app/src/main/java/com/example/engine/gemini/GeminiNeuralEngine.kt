package com.example.engine.gemini

import android.util.Log
import com.example.BuildConfig
import com.example.model.SupportedLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class GeminiTranslationResult {
    data class Success(
        val translatedText: String,
        val intermediateText: String? = null,
        val isFallback: Boolean = false,
        val modelName: String = "Gemini Neural MT"
    ) : GeminiTranslationResult()

    data class Error(
        val message: String,
        val fallbackText: String? = null
    ) : GeminiTranslationResult()
}

class GeminiNeuralEngine {

    companion object {
        private const val TAG = "GeminiNeuralEngine"
        // Priority order of models allowed by gemini-api skill
        private val CANDIDATE_MODELS = listOf(
            "gemini-3.5-flash",
            "gemini-3.1-flash-lite-preview",
            "gemini-flash-latest"
        )
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    @Volatile
    private var quotaCooldownUntil: Long = 0L

    private val translationCache = object : LinkedHashMap<String, GeminiTranslationResult.Success>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, GeminiTranslationResult.Success>?): Boolean {
            return size > 150
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Translates [text] from [sourceLang] to [targetLang] using Gemini Neural MT.
     * When translating pairs requiring Semantic English Bridge (e.g. RU <-> KO),
     * it performs a 2-step translation through English so that the intermediate text
     * is 100% genuine, fluent English.
     */
    suspend fun translate(
        text: String,
        sourceLang: SupportedLanguage,
        targetLang: SupportedLanguage
    ): GeminiTranslationResult = withContext(Dispatchers.IO) {
        if (text.isBlank()) {
            return@withContext GeminiTranslationResult.Success("", null)
        }

        // Return cached result if available
        val cacheKey = "${sourceLang.code}->${targetLang.code}:$text"
        synchronized(translationCache) {
            translationCache[cacheKey]?.let { return@withContext it }
        }

        // If Semantic English Bridge is required (e.g. RU/FR/ES <-> KO/ZH or KO <-> ZH)
        if (SupportedLanguage.requiresEnglishBridge(sourceLang, targetLang)) {
            // Step 1: Translate Source -> English Bridge
            val step1 = executeSingleTranslation(text, sourceLang, SupportedLanguage.ENGLISH)
            if (step1 is GeminiTranslationResult.Success) {
                val englishText = step1.translatedText.trim()
                // Verify that intermediate text is genuine English (no foreign scripts from source)
                val hasForeignScript = englishText.any {
                    it in '\u0400'..'\u04FF' || // Cyrillic
                    it in '\uAC00'..'\uD7AF' || it in '\u1100'..'\u11FF' || it in '\u3130'..'\u318F' || // Hangul
                    it in '\u4E00'..'\u9FFF' || it in '\u3400'..'\u4DBF' || // Han
                    it in '\u0600'..'\u06FF' || // Arabic
                    it in '\u3040'..'\u30FF'    // Japanese
                }
                val isIdentical = englishText.equals(text.trim(), ignoreCase = true)
                if (hasForeignScript || isIdentical || englishText.isBlank()) {
                    return@withContext GeminiTranslationResult.Error(
                        SupportedLanguage.getTranslationErrorMessage(sourceLang)
                    )
                }

                // Step 2: Translate English Bridge -> Target
                val step2 = executeSingleTranslation(englishText, SupportedLanguage.ENGLISH, targetLang)
                if (step2 is GeminiTranslationResult.Success) {
                    val combinedResult = GeminiTranslationResult.Success(
                        translatedText = step2.translatedText,
                        intermediateText = englishText, // 100% genuine English translation
                        modelName = "Gemini Neural MT (Bridge EN)"
                    )
                    synchronized(translationCache) {
                        translationCache[cacheKey] = combinedResult
                    }
                    return@withContext combinedResult
                } else {
                    return@withContext GeminiTranslationResult.Error(
                        SupportedLanguage.getTranslationErrorMessage(sourceLang)
                    )
                }
            } else {
                return@withContext GeminiTranslationResult.Error(
                    SupportedLanguage.getTranslationErrorMessage(sourceLang)
                )
            }
        }

        // Direct translation path
        val directResult = executeSingleTranslation(text, sourceLang, targetLang)
        if (directResult is GeminiTranslationResult.Success) {
            synchronized(translationCache) {
                translationCache[cacheKey] = directResult
            }
        }
        directResult
    }

    private fun executeSingleTranslation(
        text: String,
        sourceLang: SupportedLanguage,
        targetLang: SupportedLanguage
    ): GeminiTranslationResult {
        // Check if under 429 rate-limit / quota cooldown
        val now = System.currentTimeMillis()
        if (now < quotaCooldownUntil) {
            val remainingSec = ((quotaCooldownUntil - now) / 1000).coerceAtLeast(1)
            Log.i(TAG, "Gemini quota active cooldown: $remainingSec s remaining. Returning local neural fallback.")
            return GeminiTranslationResult.Error(
                message = "Онлайн-квота исчерпана (429). Активен резервный локальный NPU-движок ($remainingSec с)."
            )
        }

        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        // Validate API Key
        val isKeyValid = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"
        if (!isKeyValid) {
            Log.w(TAG, "Gemini API key is not configured or is placeholder. Using offline fallback.")
            return GeminiTranslationResult.Error(
                message = "Ключ Gemini API не настроен в Secrets. Переход на автономный резерв.",
                fallbackText = null
            )
        }

        val systemInstructionText = """
            You are Gemini Neural MT, an industrial-grade neural machine translation engine.
            Task: Accurately and idiomatically translate the user text from ${sourceLang.displayName} (${sourceLang.nativeName}) to ${targetLang.displayName} (${targetLang.nativeName}).
            Domain: Industrial engineering, manufacturing, machinery, safety, and operational terminology.
            Format Requirements: Return ONLY the exact translated text in ${targetLang.displayName}. Do not provide greetings, notes, explanations, or quotes.
        """.trimIndent()

        val requestJson = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemInstructionText)
                    })
                })
            })

            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", text)
                        })
                    })
                })
            })

            put("generationConfig", JSONObject().apply {
                put("temperature", 0.2)
                put("topP", 0.95)
            })
        }

        val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        var hitQuotaLimit = false

        // Try candidate models in order (e.g. gemini-3.5-flash, gemini-3.1-flash-lite-preview, gemini-flash-latest)
        for (model in CANDIDATE_MODELS) {
            try {
                val url = "$BASE_URL/$model:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    if (response.code == 429) {
                        hitQuotaLimit = true
                        Log.w(TAG, "Gemini API quota/rate limit reached for $model (code 429). Checking next or setting cooldown.")
                        var retryMs = 60_000L
                        try {
                            val errJson = JSONObject(responseBody).optJSONObject("error")
                            val details = errJson?.optJSONArray("details")
                            if (details != null) {
                                for (i in 0 until details.length()) {
                                    val item = details.optJSONObject(i)
                                    val delayStr = item?.optString("retryDelay")
                                    if (!delayStr.isNullOrBlank()) {
                                        val sec = delayStr.replace("s", "").trim().toLongOrNull()
                                        if (sec != null && sec > 0) {
                                            retryMs = (sec + 2) * 1000L
                                        }
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                        quotaCooldownUntil = System.currentTimeMillis() + retryMs
                        continue
                    } else {
                        Log.w(TAG, "Gemini API response for $model: ${response.code}")
                        continue
                    }
                }

                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                val candidate = candidates?.optJSONObject(0)
                val content = candidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val translatedString = parts?.optJSONObject(0)?.optString("text")?.trim()

                if (!translatedString.isNullOrBlank()) {
                    return GeminiTranslationResult.Success(
                        translatedText = translatedString,
                        intermediateText = null,
                        modelName = "Gemini Neural MT"
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini model $model call failed: ${e.message}")
            }
        }

        // If all candidate models exceeded quota or failed, gracefully signal fallback to offline engine
        Log.i(TAG, "Online Gemini models exhausted quota (hitQuota=$hitQuotaLimit) or offline. Switching to offline neural engine.")
        return GeminiTranslationResult.Error(
            message = if (hitQuotaLimit) {
                "Онлайн-квота исчерпана (429). Автоматический переход на локальную NPU-модель."
            } else {
                "Сеть недоступна. Автоматический переход на локальную NPU-модель."
            }
        )
    }
}
