package com.moblin.android.platform.translation

import android.icu.util.ULocale
import android.os.Build
import android.view.translation.TranslationCapability
import java.util.Locale
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

private const val translateTimeoutMs = 10_000L

fun localeLanguage(identifier: String): Locale = Locale.forLanguageTag(identifier.replace('_', '-'))

val Locale.minimalIdentifier: String
    get() = ULocale.minimizeSubtags(ULocale.forLocale(this)).toLanguageTag()

val Locale.maximalIdentifier: String
    get() = ULocale.addLikelySubtags(ULocale.forLocale(this)).toLanguageTag()

private val Locale.maximalScript: String
    get() = ULocale.addLikelySubtags(ULocale.forLocale(this)).script

private val Locale.englishName: String
    get() = Locale.forLanguageTag(minimalIdentifier).getDisplayName(Locale.ENGLISH).ifEmpty { minimalIdentifier }

class TranslationError private constructor(
    private val kind: Kind,
    val sourceLanguage: Locale? = null,
    val targetLanguage: Locale? = null,
) : Exception() {
    private enum class Kind {
        internalError,
        notInstalled,
        alreadyCancelled,
        nothingToTranslate,
        unableToIdentifyLanguage,
        unsupportedSourceLanguage,
        unsupportedTargetLanguage,
        unsupportedLanguagePairing,
    }

    val errorDescription: String?
        get() = when (kind) {
            Kind.nothingToTranslate -> "Translation Request Empty"
            else -> "Unable to Translate"
        }

    val failureReason: String?
        get() = when (kind) {
            Kind.internalError -> "Something went wrong. Please try again later."
            Kind.notInstalled -> "Languages must be downloaded on-device."
            Kind.alreadyCancelled -> "Translation was already cancelled."
            Kind.nothingToTranslate -> "Please provide text to translate and try again."
            Kind.unableToIdentifyLanguage -> "The language could not be automatically detected."
            Kind.unsupportedSourceLanguage -> sourceLanguage?.let {
                "Translation from ${it.englishName} is not supported."
            } ?: "Translation from this language is not supported. Please try another language."
            Kind.unsupportedTargetLanguage -> targetLanguage?.let {
                "Translation into ${it.englishName} is not supported."
            } ?: "Translation into this language is not supported. Please try another language."
            Kind.unsupportedLanguagePairing -> "This language pairing is not supported."
        }

    val localizedDescription: String
        get() = errorDescription ?: "Unable to Translate"

    override val message: String
        get() = localizedDescription

    override fun equals(other: Any?): Boolean = other is TranslationError && other.kind == kind

    override fun hashCode(): Int = kind.hashCode()

    override fun toString(): String = "TranslationError(${kind.name}, ${failureReason ?: ""})"

    companion object {
        val internalError: TranslationError
            get() = TranslationError(Kind.internalError)
        val notInstalled: TranslationError
            get() = TranslationError(Kind.notInstalled)
        val alreadyCancelled: TranslationError
            get() = TranslationError(Kind.alreadyCancelled)
        val nothingToTranslate: TranslationError
            get() = TranslationError(Kind.nothingToTranslate)
        val unableToIdentifyLanguage: TranslationError
            get() = TranslationError(Kind.unableToIdentifyLanguage)
        val unsupportedSourceLanguage: TranslationError
            get() = TranslationError(Kind.unsupportedSourceLanguage)
        val unsupportedTargetLanguage: TranslationError
            get() = TranslationError(Kind.unsupportedTargetLanguage)
        val unsupportedLanguagePairing: TranslationError
            get() = TranslationError(Kind.unsupportedLanguagePairing)

        internal fun unsupportedSourceLanguage(language: Locale) =
            TranslationError(Kind.unsupportedSourceLanguage, sourceLanguage = language)

        internal fun unsupportedTargetLanguage(language: Locale) =
            TranslationError(Kind.unsupportedTargetLanguage, targetLanguage = language)
    }
}

class TranslationSession(installedSource: Locale, target: Locale? = null) {
    val sourceLanguage: Locale? = installedSource
    val targetLanguage: Locale? = target
    private val backend = TranslationSystem.backend
    private val mutex = Mutex()
    private var engine: TranslationEngine? = null
    private var cancelled = false

    class Response internal constructor(
        val sourceLanguage: Locale,
        val targetLanguage: Locale,
        val sourceText: String,
        val targetText: String,
        val clientIdentifier: String? = null,
    )

    suspend fun translate(string: String): Response {
        return mutex.withLock {
            translateLocked(string)
        }
    }

    fun cancel() {
        cancelled = true
        engine?.destroy()
        engine = null
    }

    protected fun finalize() {
        if (backend.sdkInt >= Build.VERSION_CODES.TIRAMISU) {
            engine?.destroy()
        }
    }

    private suspend fun translateLocked(string: String): Response {
        if (cancelled) {
            throw TranslationError.alreadyCancelled
        }
        if (string.isEmpty()) {
            throw TranslationError.nothingToTranslate
        }
        val source = sourceLanguage ?: Locale.getDefault()
        val target = targetLanguage ?: Locale.getDefault()
        val engine = engine ?: openEngine(source, target)
        val targetText = withTimeoutOrNull(translateTimeoutMs) {
            engine.translate(string)
        }
        if (targetText == null) {
            engine.destroy()
            this.engine = null
            throw TranslationError.internalError
        }
        return Response(
            sourceLanguage = source,
            targetLanguage = target,
            sourceText = string,
            targetText = targetText,
        )
    }

    private suspend fun openEngine(source: Locale, target: Locale): TranslationEngine {
        val capability = installedCapability(backend, source, target)
        val engine = withTimeoutOrNull(translateTimeoutMs) {
            backend.createEngine(capability.source, capability.target)
        } ?: throw TranslationError.internalError
        if (cancelled) {
            engine.destroy()
            throw TranslationError.alreadyCancelled
        }
        this.engine = engine
        return engine
    }
}

class LanguageAvailability {
    enum class Status {
        installed,
        supported,
        unsupported,
    }

    private val backend = TranslationSystem.backend
    private var cachedCapabilities: List<TranslationPairCapability>? = null

    suspend fun supportedLanguages(): List<Locale> {
        val capabilities = capabilities() ?: return emptyList()
        val languages = mutableListOf<Locale>()
        for (capability in capabilities) {
            for (identifier in listOf(capability.source, capability.target)) {
                val language = localeLanguage(identifier = identifier)
                if (languages.none { it.maximalIdentifier == language.maximalIdentifier }) {
                    languages.add(language)
                }
            }
        }
        return languages
    }

    suspend fun status(from: Locale, to: Locale?): Status {
        val capabilities = capabilities() ?: return Status.unsupported
        val capability = bestCapability(capabilities, from, to ?: Locale.getDefault()) ?: return Status.unsupported
        return when (capability.state) {
            TranslationCapability.STATE_ON_DEVICE -> Status.installed
            TranslationCapability.STATE_NOT_AVAILABLE -> Status.unsupported
            else -> Status.supported
        }
    }

    private suspend fun capabilities(): List<TranslationPairCapability>? {
        cachedCapabilities?.let { return it }
        val capabilities = loadCapabilities(backend)
        cachedCapabilities = capabilities
        return capabilities
    }
}

private suspend fun loadCapabilities(backend: TranslationBackend): List<TranslationPairCapability>? {
    if (backend.sdkInt < Build.VERSION_CODES.S) {
        return null
    }
    return backend.capabilities()
}

private suspend fun installedCapability(
    backend: TranslationBackend,
    source: Locale,
    target: Locale,
): TranslationPairCapability {
    val capabilities = loadCapabilities(backend)
    if (capabilities.isNullOrEmpty()) {
        throw TranslationError.unsupportedLanguagePairing
    }
    val capability = bestCapability(capabilities, source, target)
    if (capability == null) {
        if (capabilities.none { matchScore(it.source, source) >= 0 }) {
            throw TranslationError.unsupportedSourceLanguage(source)
        }
        if (capabilities.none { matchScore(it.target, target) >= 0 }) {
            throw TranslationError.unsupportedTargetLanguage(target)
        }
        throw TranslationError.unsupportedLanguagePairing
    }
    return when (capability.state) {
        TranslationCapability.STATE_ON_DEVICE -> capability
        TranslationCapability.STATE_NOT_AVAILABLE -> throw TranslationError.unsupportedLanguagePairing
        else -> throw TranslationError.notInstalled
    }
}

private fun bestCapability(
    capabilities: List<TranslationPairCapability>,
    source: Locale,
    target: Locale,
): TranslationPairCapability? {
    var best: TranslationPairCapability? = null
    var bestScore = -1
    for (capability in capabilities) {
        val sourceScore = matchScore(capability.source, source)
        val targetScore = matchScore(capability.target, target)
        if (sourceScore < 0 || targetScore < 0) {
            continue
        }
        var score = 4 * (sourceScore + targetScore)
        score += when (capability.state) {
            TranslationCapability.STATE_ON_DEVICE -> 3
            TranslationCapability.STATE_NOT_AVAILABLE -> 0
            TranslationCapability.STATE_DOWNLOADING -> 2
            else -> 1
        }
        if (score > bestScore) {
            best = capability
            bestScore = score
        }
    }
    return best
}

private fun matchScore(identifier: String, language: Locale): Int {
    val candidate = localeLanguage(identifier = identifier)
    if (candidate.language.isEmpty() || candidate.language != language.language) {
        return -1
    }
    if (candidate.maximalScript != language.maximalScript) {
        return -1
    }
    if (candidate.toLanguageTag() == language.toLanguageTag()) {
        return 2
    }
    if (candidate.country.isEmpty() || candidate.country == language.country) {
        return 1
    }
    return 0
}
