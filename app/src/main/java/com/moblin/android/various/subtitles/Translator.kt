package com.moblin.android.various.subtitles

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "Translator"

private val mainScope = CoroutineScope(Dispatchers.Main)

interface TranslatorDelegate {
    fun translatorTranslated(languageIdentifier: String, text: String)
}

class Translator(private val targetIdentifier: String) {
    private val session: suspend (String) -> String = { _ ->
        TODO("no Android counterpart for Translation")
    }

    private var ready = true
    private var latestText: String? = null

    var delegate: TranslatorDelegate? = null

    companion object {
        var translators: MutableList<Translator> = mutableListOf()
    }

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
                    val targetText = session(text)
                    delegate?.translatorTranslated(languageIdentifier = targetIdentifier, text = targetText)
                } catch (error: Throwable) {
                    val message = error.message ?: error.toString()
                    delegate?.translatorTranslated(languageIdentifier = targetIdentifier, text = message)
                }
            }
            ready = true
        }
    }
}
