package com.moblin.android.various.subtitles

import android.util.Log
import com.moblin.android.platform.translation.TranslationError
import com.moblin.android.platform.translation.TranslationSession
import com.moblin.android.platform.translation.localeLanguage
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "Translator"

interface TranslatorDelegate {
    fun translatorTranslated(languageIdentifier: String, text: String)
}

class Translator(private val targetIdentifier: String) {
    companion object {
        var translators: MutableList<Translator> = mutableListOf()
    }

    private val session: TranslationSession = TranslationSession(
        installedSource = Locale.getDefault(),
        target = localeLanguage(identifier = targetIdentifier),
    )
    private var ready = true
    private var latestText: String? = null
    var delegate: TranslatorDelegate? = null
    private val mainScope = CoroutineScope(Dispatchers.Main)

    fun translate(text: String) {
        latestText = text
        if (!ready) {
            return
        }
        ready = false
        mainScope.launch {
            while (true) {
                val text = latestText ?: break
                latestText = null
                try {
                    val response = session.translate(text)
                    delegate?.translatorTranslated(languageIdentifier = targetIdentifier, text = response.targetText)
                } catch (error: TranslationError) {
                    val message = error.failureReason ?: error.localizedDescription
                    delegate?.translatorTranslated(languageIdentifier = targetIdentifier, text = message)
                } catch (error: Exception) {
                    Log.i(TAG, "speech-to-text: Translation error: $error")
                }
            }
            ready = true
        }
    }
}
