package com.moblin.android.various.model

import com.moblin.android.various.settings.SettingsWidgetType
import com.moblin.android.various.SpeechToText
import com.moblin.android.various.SpeechToTextDelegate
import com.moblin.android.various.subtitles.TextAligner
import com.moblin.android.various.subtitles.Translator
import com.moblin.android.various.subtitles.TranslatorDelegate
import com.moblin.android.AppDelegate

fun Model.reloadSpeechToText() {
    stopSpeechToText()
    if (isSpeechToTextNeeded()) {
        startSpeechToText()
    }
}

fun Model.startSpeechToText() {
    val newSpeechToText = SpeechToText(AppDelegate.context)
    speechToText = newSpeechToText
    newSpeechToText.delegate = object : SpeechToTextDelegate {
        override fun speechToTextPartialResult(position: Int, text: String) {
            this@startSpeechToText.speechToTextPartialResult(position = position, text = text)
        }

        override fun speechToTextClear() {
            this@startSpeechToText.speechToTextClear()
        }
    }
    newSpeechToText.start { message ->
        makeErrorToast(title = message)
    }
    for (widget in widgetsInCurrentScene(onlyEnabled = true)) {
        when (widget.widget.type) {
            SettingsWidgetType.text -> {
                val languageIdentifiers = widget.widget.text.subtitles.map { it.identifier }.toSet()
                for (languageIdentifier in languageIdentifiers) {
                    if (languageIdentifier != null) {
                        addTranslator(targetIdentifier = languageIdentifier)
                    }
                }
            }
            else -> {}
        }
    }
}

fun Model.stopSpeechToText() {
    removeAllTranslators()
    speechToText?.stop()
    speechToText = null
    for (textEffect in textEffects.values) {
        Unit
    }
    for (browserEffect in browserEffects.values) {
        Unit
    }
    speechToTextTextAligners.clear()
}

fun Model.updateSpeechToText() {
    if (isSpeechToTextNeeded()) {
        if (speechToText == null) {
            startSpeechToText()
        }
    } else if (speechToText != null) {
        stopSpeechToText()
    }
}

fun Model.isSpeechToTextNeeded(): Boolean {
    for (widget in widgetsInCurrentScene(onlyEnabled = true)) {
        when (widget.widget.type) {
            SettingsWidgetType.text -> {
                if (widget.widget.text.needsSubtitles) {
                    return true
                }
            }
            SettingsWidgetType.alerts -> {
                if (widget.widget.alerts.needsSubtitles) {
                    return true
                }
            }
            SettingsWidgetType.browser -> {
                if (widget.widget.browser.moblinAccess && widget.widget.browser.speechToText) {
                    return true
                }
            }
            else -> {}
        }
    }
    return false
}

fun Model.speechToTextClear() {
    for (textEffect in textEffects.values) {
        Unit
    }
    for (browserEffect in browserEffects.values) {
        Unit
    }
    speechToTextTextAligners.clear()
    speechToTextAlertMatchOffset = 0
}

private fun Model.removeAllTranslators() {
    Translator.translators.clear()
}

private fun Model.addTranslator(targetIdentifier: String) {
    val translator = Translator(targetIdentifier = targetIdentifier)
    translator.delegate = object : TranslatorDelegate {
        override fun translatorTranslated(languageIdentifier: String, text: String) {
            this@addTranslator.translatorTranslated(
                languageIdentifier = languageIdentifier,
                text = text,
            )
        }
    }
    Translator.translators.add(translator)
}

private fun Model.speechToTextPartialResultTextWidgets(
    position: Int,
    text: String,
    languageIdentifier: String?,
) {
    for (textEffect in textEffects.values) {
        Unit
    }
}

private fun Model.speechToTextPartialResultBrowserWidgets(position: Int, text: String) {
    for (browserEffect in browserEffects.values) {
        Unit
    }
}

private fun Model.speechToTextPartialResultAlertsWidget(text: String) {
    if (text.length <= speechToTextAlertMatchOffset) {
        return
    }
    val startMatchIndex = speechToTextAlertMatchOffset
    for (alertEffect in enabledAlertsEffects) {
        val settings = alertEffect.getSettings().speechToText
        for (string in settings.strings) {
            if (!string.alert.enabled) {
                continue
            }
            val matchIndex = text.indexOf(
                string = string.string,
                startIndex = startMatchIndex,
                ignoreCase = true,
            )
            if (matchIndex < 0) {
                continue
            }
            val offset = matchIndex + string.string.length
            if (offset > speechToTextAlertMatchOffset) {
                speechToTextAlertMatchOffset = offset
            }
            playAlert(TODO("Alert.speechToTextString(string.id)"))
        }
    }
}

fun Model.speechToTextPartialResult(position: Int, text: String) {
    speechToTextLatestPosition = position
    speechToTextLatestText = text
}

fun Model.speechToTextProcess() {
    val position = speechToTextLatestPosition ?: return
    val text = speechToTextLatestText ?: return
    speechToTextLatestPosition = null
    speechToTextLatestText = null
    for (translator in Translator.translators) {
        translator.translate(text = text.takeLast(150))
    }
    speechToTextPartialResultTextWidgets(
        position = position,
        text = text,
        languageIdentifier = null,
    )
    speechToTextPartialResultAlertsWidget(text = text)
    speechToTextPartialResultBrowserWidgets(position = position, text = text)
}

fun Model.translatorTranslated(languageIdentifier: String, text: String) {
    val position: Int
    val existingTextAligner = speechToTextTextAligners[languageIdentifier]
    if (existingTextAligner != null) {
        existingTextAligner.update(newText = text)
        position = existingTextAligner.position
    } else {
        val textAligner = TextAligner(text = text)
        speechToTextTextAligners[languageIdentifier] = textAligner
        position = textAligner.position
    }
    speechToTextPartialResultTextWidgets(
        position = position,
        text = text,
        languageIdentifier = languageIdentifier,
    )
}
