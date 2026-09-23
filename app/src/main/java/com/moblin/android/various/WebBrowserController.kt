package com.moblin.android.various

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.moblin.android.platform.swiftui.Alert
import com.moblin.android.platform.swiftui.ButtonRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class WebBrowserPanel {
    Alert,
    Confirm,
    TextInput,
}

class WebBrowserController {
    val showAlert = MutableStateFlow(false)

    val panel = MutableStateFlow<WebBrowserPanel?>(null)

    val message = MutableStateFlow<String?>(null)

    val defaultText = MutableStateFlow<String?>(null)

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
        this.message.value = message
        defaultText.value = null
        panel.value = WebBrowserPanel.Alert
        showAlert.value = true
    }

    fun webViewRunJavaScriptConfirmPanelWithMessage(
        message: String,
        initiatedByFrame: Any?,
        completionHandler: (Boolean) -> Unit,
    ) {
        alertCompletionHandler = null
        confirmCompletionHandler = completionHandler
        textInputCompletionHandler = null
        this.message.value = message
        defaultText.value = null
        panel.value = WebBrowserPanel.Confirm
        showAlert.value = true
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
        message.value = prompt
        this.defaultText.value = defaultText
        panel.value = WebBrowserPanel.TextInput
        showAlert.value = true
    }

    fun completeAlert() {
        showAlert.value = false
        val handler = alertCompletionHandler
        alertCompletionHandler = null
        handler?.invoke()
    }

    fun completeConfirm(result: Boolean) {
        showAlert.value = false
        val handler = confirmCompletionHandler
        confirmCompletionHandler = null
        handler?.invoke(result)
    }

    fun completeTextInput(text: String?) {
        showAlert.value = false
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

    when (panel) {
        WebBrowserPanel.Confirm -> {
            Alert(
                title = "",
                isPresented = showAlert,
                onDismissRequest = { controller.completeConfirm(false) },
                message = message,
            ) {
                Button("Cancel", role = ButtonRole.cancel) {
                    controller.completeConfirm(false)
                }
                Button("Ok") {
                    controller.completeConfirm(true)
                }
            }
        }
        WebBrowserPanel.TextInput -> {
            var text by remember { mutableStateOf(defaultText ?: "") }
            LaunchedEffect(showAlert) {
                if (showAlert) {
                    text = controller.defaultText.value ?: ""
                }
            }
            Alert(
                title = "",
                isPresented = showAlert,
                onDismissRequest = { controller.completeTextInput(null) },
                message = message,
            ) {
                TextField("", text = text) { text = it }
                Button("Cancel") {
                    controller.completeTextInput(null)
                }
                Button("Ok") {
                    controller.completeTextInput(text)
                }
            }
        }
        else -> {
            Alert(
                title = "",
                isPresented = showAlert,
                onDismissRequest = { controller.completeAlert() },
                message = message,
            ) {
                Button("Ok") {
                    controller.completeAlert()
                }
            }
        }
    }
}
