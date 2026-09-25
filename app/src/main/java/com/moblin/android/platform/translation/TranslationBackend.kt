package com.moblin.android.platform.translation

import android.content.Context
import android.icu.util.ULocale
import android.os.Build
import android.util.Log
import android.view.translation.TranslationContext
import android.view.translation.TranslationManager
import android.view.translation.TranslationRequest
import android.view.translation.TranslationRequestValue
import android.view.translation.TranslationResponse
import android.view.translation.TranslationResponseValue
import android.view.translation.TranslationSpec
import androidx.annotation.RequiresApi
import com.moblin.android.AppDelegate
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

private const val TAG = "Translation"

class TranslationPairCapability(val source: String, val target: String, val state: Int) {
    override fun toString(): String = "TranslationPairCapability($source -> $target, state $state)"
}

interface TranslationEngine {
    suspend fun translate(text: String): String?

    fun destroy()
}

interface TranslationBackend {
    val sdkInt: Int

    suspend fun capabilities(): List<TranslationPairCapability>?

    suspend fun createEngine(source: String, target: String): TranslationEngine?
}

object TranslationSystem {
    private var override: TranslationBackend? = null
    private val android by lazy { AndroidTranslationBackend(AppDelegate.context) }

    var backend: TranslationBackend
        get() = override ?: android
        set(value) {
            override = value
        }

    fun reset() {
        override = null
    }
}

internal class AndroidTranslationBackend(private val context: Context) : TranslationBackend {
    override val sdkInt: Int
        get() = Build.VERSION.SDK_INT

    override suspend fun capabilities(): List<TranslationPairCapability>? {
        if (sdkInt < Build.VERSION_CODES.S) {
            return null
        }
        return withContext(Dispatchers.IO) {
            onDeviceCapabilities()
        }
    }

    override suspend fun createEngine(source: String, target: String): TranslationEngine? {
        if (sdkInt < Build.VERSION_CODES.S) {
            return null
        }
        return createOnDeviceEngine(source, target)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun onDeviceCapabilities(): List<TranslationPairCapability>? {
        val manager = context.getSystemService(TranslationManager::class.java) ?: return null
        return try {
            manager.getOnDeviceTranslationCapabilities(TranslationSpec.DATA_FORMAT_TEXT, TranslationSpec.DATA_FORMAT_TEXT)
                .map {
                    TranslationPairCapability(
                        source = it.sourceSpec.locale.toLanguageTag(),
                        target = it.targetSpec.locale.toLanguageTag(),
                        state = it.state,
                    )
                }
        } catch (error: RuntimeException) {
            Log.i(TAG, "Failed to get translation capabilities: $error")
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private suspend fun createOnDeviceEngine(source: String, target: String): TranslationEngine? {
        val manager = context.getSystemService(TranslationManager::class.java) ?: return null
        val translationContext = TranslationContext.Builder(
            TranslationSpec(ULocale.forLanguageTag(source), TranslationSpec.DATA_FORMAT_TEXT),
            TranslationSpec(ULocale.forLanguageTag(target), TranslationSpec.DATA_FORMAT_TEXT),
        ).build()
        return suspendCancellableCoroutine { continuation ->
            try {
                manager.createOnDeviceTranslator(translationContext, { it.run() }) { translator ->
                    val engine = translator?.let { AndroidTranslationEngine(it) }
                    if (continuation.isActive) {
                        continuation.resume(engine)
                    } else {
                        engine?.destroy()
                    }
                }
            } catch (error: RuntimeException) {
                Log.i(TAG, "Failed to create translator: $error")
                continuation.resume(null)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private class AndroidTranslationEngine(private val translator: android.view.translation.Translator) : TranslationEngine {
    override suspend fun translate(text: String): String? {
        val request = TranslationRequest.Builder()
            .setFlags(TranslationRequest.FLAG_TRANSLATION_RESULT)
            .setTranslationRequestValues(listOf(TranslationRequestValue.forText(text)))
            .build()
        return suspendCancellableCoroutine { continuation ->
            try {
                translator.translate(request, null, { it.run() }) { response ->
                    if (continuation.isActive) {
                        continuation.resume(translatedText(response))
                    }
                }
            } catch (error: RuntimeException) {
                Log.i(TAG, "Failed to translate: $error")
                continuation.resume(null)
            }
        }
    }

    override fun destroy() {
        try {
            translator.destroy()
        } catch (error: RuntimeException) {
            Log.i(TAG, "Failed to destroy translator: $error")
        }
    }

    private fun translatedText(response: TranslationResponse): String? {
        if (response.translationStatus != TranslationResponse.TRANSLATION_STATUS_SUCCESS) {
            return null
        }
        val value = response.translationResponseValues.get(0) ?: return null
        if (value.statusCode != TranslationResponseValue.STATUS_SUCCESS) {
            return null
        }
        return value.text?.toString()
    }
}
