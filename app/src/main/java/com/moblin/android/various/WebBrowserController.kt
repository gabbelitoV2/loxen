package com.moblin.android.various

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.moblin.android.localized
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class WebBrowserPanel {
    Alert,
    Confirm,
    TextInput,
}

class WebBrowserController {
    private val _showAlert = MutableStateFlow(false)
    val showAlert: StateFlow<Boolean> = _showAlert.asStateFlow()

    private val _panel = MutableStateFlow<WebBrowserPanel?>(null)
    val panel: StateFlow<WebBrowserPanel?> = _panel.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _defaultText = MutableStateFlow<String?>(null)
    val defaultText: StateFlow<String?> = _defaultText.asStateFlow()

    private var alertCompletionHandler: (() -> Unit)? = null
    private var confirmCompletionHandler: ((Boolean) -> Unit)? = null
    private var textInputCompletionHandler: ((String?) -> Unit)? = null

    fun webViewRunJavaScriptAlertPanelWithMessage(
        message: String,
        initiatedByFrame: Any?,
        completionHandler: () -> Unit,
    ) {
        alertCompletionHandler = completionHandler
        confirmCompletionHandler = null
        textInputCompletionHandler = null
        _message.value = message
        _defaultText.value = null
        _panel.value = WebBrowserPanel.Alert
        _showAlert.value = true
    }

    fun webViewRunJavaScriptConfirmPanelWithMessage(
        message: String,
        initiatedByFrame: Any?,
        completionHandler: (Boolean) -> Unit,
    ) {
        alertCompletionHandler = null
        confirmCompletionHandler = completionHandler
        textInputCompletionHandler = null
        _message.value = message
        _defaultText.value = null
        _panel.value = WebBrowserPanel.Confirm
        _showAlert.value = true
    }

    fun webViewRunJavaScriptTextInputPanelWithPrompt(
        prompt: String,
        defaultText: String?,
        initiatedByFrame: Any?,
        completionHandler: (String?) -> Unit,
    ) {
        alertCompletionHandler = null
        confirmCompletionHandler = null
        textInputCompletionHandler = completionHandler
        _message.value = prompt
        _defaultText.value = defaultText
        _panel.value = WebBrowserPanel.TextInput
        _showAlert.value = true
    }

    fun completeAlert() {
        _showAlert.value = false
        val handler = alertCompletionHandler
        alertCompletionHandler = null
        handler?.invoke()
    }

    fun completeConfirm(result: Boolean) {
        _showAlert.value = false
        val handler = confirmCompletionHandler
        confirmCompletionHandler = null
        handler?.invoke(result)
    }

    fun completeTextInput(text: String?) {
        _showAlert.value = false
        val handler = textInputCompletionHandler
        textInputCompletionHandler = null
        handler?.invoke(text)
    }
}

@Composable
fun WebBrowserAlertDialog(controller: WebBrowserController) {
    val showAlert by controller.showAlert.collectAsState()
    val panel by controller.panel.collectAsState()
    val message by controller.message.collectAsState()
    val defaultText by controller.defaultText.collectAsState()

    if (!showAlert) {
        return
    }

    when (panel) {
        WebBrowserPanel.Confirm -> {
            AlertDialog(
                onDismissRequest = {
                    controller.completeConfirm(false)
                },
                text = {
                    message?.let { Text(it) }
                },
                confirmButton = {
                    TextButton(onClick = {
                        controller.completeConfirm(true)
                    }) {
                        Text(localized("Ok"))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        controller.completeConfirm(false)
                    }) {
                        Text(localized("Cancel"))
                    }
                },
            )
        }
        WebBrowserPanel.TextInput -> {
            var text by remember(defaultText) { mutableStateOf(defaultText ?: "") }
            AlertDialog(
                onDismissRequest = {
                    controller.completeTextInput(null)
                },
                text = {
                    Column {
                        message?.let { Text(it) }
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        controller.completeTextInput(text.ifEmpty { defaultText })
                    }) {
                        Text(localized("Ok"))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        controller.completeTextInput(null)
                    }) {
                        Text(localized("Cancel"))
                    }
                },
            )
        }
        else -> {
            AlertDialog(
                onDismissRequest = {
                    controller.completeAlert()
                },
                text = {
                    message?.let { Text(it) }
                },
                confirmButton = {
                    TextButton(onClick = {
                        controller.completeAlert()
                    }) {
                        Text(localized("Ok"))
                    }
                },
            )
        }
    }
}
