package com.moblin.android.view.settings.debug

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.LogEntry
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsDebug
import com.moblin.android.view.utils.CloseToolbar
import com.moblin.android.view.utils.ShareSheetView
import java.util.UUID

private data class ShareItem(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
)

private fun isMessageVisible(logFilter: String, message: String): Boolean =
    logFilter.isEmpty() || message.lowercase().contains(logFilter.lowercase())

private fun formatLog(log: List<LogEntry>): String =
    log.joinToString("\n") { it.message }

@Composable
private fun DebugLogToolbarButton(
    systemImage: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val palette = formPalette()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    SystemImage(
        name = systemImage,
        fontSize = 17.sp,
        modifier = Modifier
            .alpha(if (pressed && enabled) 0.2f else 1f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        tint = if (enabled) palette.accent else palette.gray,
    )
}

@Composable
fun DebugLogSettingsView(
    model: Model = LocalModel.current,
    debug: SettingsDebug,
    log: List<LogEntry>,
    presentingLog: Boolean,
    onPresentingLogChange: (Boolean) -> Unit,
    reloadLog: () -> Unit,
    clearLog: () -> Unit,
    onLogChange: (List<LogEntry>) -> Unit,
) {
    val logFilter by debug.logFilter.collectAsState()
    val palette = formPalette()
    var shareItem by remember { mutableStateOf<ShareItem?>(null) }

    Form(
        title = "Log",
        toolbar = {
            DebugLogToolbarButton(
                systemImage = "square.and.arrow.up",
                enabled = log.isNotEmpty(),
            ) {
                shareItem = ShareItem(
                    url = formatLog(log.filter { isMessageVisible(logFilter, it.message) }),
                )
            }
            DebugLogToolbarButton(
                systemImage = "trash",
                enabled = log.isNotEmpty(),
            ) {
                onLogChange(emptyList())
                clearLog()
            }
            DebugLogToolbarButton(
                systemImage = "arrow.clockwise",
            ) {
                reloadLog()
            }
            CloseToolbar(
                presenting = presentingLog,
                onPresentingChange = onPresentingLogChange,
            )
        },
    ) {
        Section {
            FormRow {
                BasicTextField(
                    value = logFilter,
                    onValueChange = { debug.logFilter.value = it },
                    singleLine = true,
                    textStyle = formBodyStyle.copy(color = palette.label),
                    cursorBrush = SolidColor(palette.accent),
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                    decorationBox = { innerTextField ->
                        if (logFilter.isEmpty()) {
                            Text(
                                "Filter",
                                style = formBodyStyle.copy(color = palette.secondaryLabel),
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Section {
            if (log.isEmpty()) {
                FormRow {
                    Text(
                        "The log is empty.",
                        style = formBodyStyle.copy(color = palette.label),
                    )
                }
            } else {
                FormRow {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.weight(1f),
                    ) {
                        log.forEach { item ->
                            key(item.id) {
                                if (isMessageVisible(logFilter, item.message)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(
                                            item.message,
                                            style = formBodyStyle.copy(color = palette.label),
                                        )
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    shareItem?.let { item ->
        Sheet(onDismissRequest = { shareItem = null }) {
            ShareSheetView(activityItems = listOf(item.url))
        }
    }
}
