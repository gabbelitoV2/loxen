package com.moblin.android.platform.translation

import android.view.translation.TranslationCapability

internal class FakeTranslationEngine(
    val source: String,
    val target: String,
    private val backend: FakeTranslationBackend,
) : TranslationEngine {
    val requests = mutableListOf<String>()
    var destroyed = false

    override suspend fun translate(text: String): String? {
        requests.add(text)
        val hook = backend.translateHook ?: return "[$target] $text"
        return hook(text)
    }

    override fun destroy() {
        destroyed = true
    }
}

internal class FakeTranslationBackend(
    override var sdkInt: Int = 35,
    var capabilities: List<TranslationPairCapability>? = emptyList(),
) : TranslationBackend {
    val engines = mutableListOf<FakeTranslationEngine>()
    var createFails = false
    var translateHook: (suspend (String) -> String?)? = null

    val engine: FakeTranslationEngine
        get() = engines.last()

    override suspend fun capabilities(): List<TranslationPairCapability>? = capabilities

    override suspend fun createEngine(source: String, target: String): TranslationEngine? {
        if (createFails) {
            return null
        }
        val engine = FakeTranslationEngine(source, target, this)
        engines.add(engine)
        return engine
    }
}

internal fun onDevice(source: String, target: String) =
    TranslationPairCapability(source, target, TranslationCapability.STATE_ON_DEVICE)

internal fun downloadable(source: String, target: String) =
    TranslationPairCapability(source, target, TranslationCapability.STATE_AVAILABLE_TO_DOWNLOAD)

internal fun downloading(source: String, target: String) =
    TranslationPairCapability(source, target, TranslationCapability.STATE_DOWNLOADING)

internal fun notAvailable(source: String, target: String) =
    TranslationPairCapability(source, target, TranslationCapability.STATE_NOT_AVAILABLE)
