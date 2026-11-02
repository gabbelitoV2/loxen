package com.moblin.android.various.model

import com.moblin.android.various.settings.SettingsWidget
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
    startSpeechToText(drawnWidgets = drawnWidgetsInSelectedScene())
}

private fun Model.startSpeechToText(drawnWidgets: List<SettingsWidget>) {
    val newSpeechToText = SpeechToText()
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
    for (widget in drawnWidgets) {
        when (widget.type) {
            SettingsWidgetType.text -> {
                val languageIdentifiers = widget.text.subtitles.map { it.identifier }.toSet()
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
        textEffect.clearSubtitles()
    }
    for (browserEffect in browserEffects.values) {
        browserEffect.sendSpeechToTextClear()
    }
    speechToTextTextAligners.clear()
}

fun Model.updateSpeechToText() {
    updateSpeechToText(drawnWidgets = drawnWidgetsInSelectedScene())
}

fun Model.updateSpeechToText(drawnWidgets: List<SettingsWidget>) {
    if (isSpeechToTextNeeded(drawnWidgets = drawnWidgets)) {
        if (speechToText == null) {
            startSpeechToText(drawnWidgets = drawnWidgets)
        }
    } else if (speechToText != null) {
        stopSpeechToText()
    }
}

fun Model.isSpeechToTextNeeded(): Boolean {
    return isSpeechToTextNeeded(drawnWidgets = drawnWidgetsInSelectedScene())
}

private fun Model.isSpeechToTextNeeded(drawnWidgets: List<SettingsWidget>): Boolean {
    for (widget in drawnWidgets) {
        when (widget.type) {
            SettingsWidgetType.text -> {
                if (widget.text.needsSubtitles) {
                    return true
                }
            }
            SettingsWidgetType.alerts -> {
                if (widget.alerts.needsSubtitles) {
                    return true
                }
            }
            SettingsWidgetType.browser -> {
                if (widget.browser.moblinAccess && widget.browser.speechToText) {
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
        textEffect.clearSubtitles()
    }
    for (browserEffect in browserEffects.values) {
        browserEffect.sendSpeechToTextClear()
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
        textEffect.updateSubtitles(position = position, text = text, languageIdentifier = languageIdentifier)
    }
}

private fun Model.speechToTextPartialResultBrowserWidgets(position: Int, text: String) {
    for (browserEffect in browserEffects.values) {
        browserEffect.sendSpeechToText(position = position, text = text)
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
            playAlert(com.moblin.android.videoeffects.alerts.AlertsEffectAlert.SpeechToTextString(string.id))
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
