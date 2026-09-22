package com.moblin.android.view.settings.scenes.widgets.widget.text

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.moblin.android.common.various.formatShortDuration
import com.moblin.android.common.various.iconWidth
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsFontDesign
import com.moblin.android.various.settings.SettingsFontWeight
import com.moblin.android.various.settings.SettingsHorizontalAlignment
import com.moblin.android.various.settings.SettingsLocation
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetText
import com.moblin.android.various.settings.SettingsWidgetTextCheckbox
import com.moblin.android.various.settings.SettingsWidgetTextLapTimes
import com.moblin.android.various.settings.SettingsWidgetTextRating
import com.moblin.android.various.settings.SettingsWidgetTextStopwatch
import com.moblin.android.various.settings.SettingsWidgetTextTimer
import com.moblin.android.various.utils.isPhone
import com.moblin.android.videoeffects.text.TextEffect
import com.moblin.android.videoeffects.text.TextFormatLengthUnit
import com.moblin.android.videoeffects.text.TextFormatSpeedUnit
import com.moblin.android.videoeffects.text.TextFormatTemperatureUnit
import com.moblin.android.videoeffects.text.loadTextFormat
import com.moblin.android.videoeffects.text.textEffectDateFormatter
import com.moblin.android.videoeffects.text.textEffectFullDateFormatter
import com.moblin.android.videoeffects.text.textEffectShortTimeFormat
import com.moblin.android.videoeffects.text.textEffectTimeFormat
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.MultiLineTextFieldDoneButtonView
import com.moblin.android.view.utils.MultiLineTextFieldView
import com.moblin.android.view.utils.RgbColorPickerView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.view.utils.TextItemLocalizedView
import java.time.Duration
import java.time.Instant
import kotlin.math.min
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

private data class Suggestion(val id: Int, val name: String, val text: String)

private const val suggestionCountry = "{countryFlag} {country}"
private const val suggestionCity = "{countryFlag} {city}"
private const val suggestionMovement = "📏 {distance} 💨 {speed} 🏔️ {altitude}"
private const val suggestionHeartRate = "♥️ {heartRate}"
private const val suggestionStepCount = "🚶{stepCount}"
private const val suggestionSubtitles = "{subtitles}"
private const val suggestionMuted = "{muted}"
private const val suggestionTime = "🕑 {shortTime}"
private const val suggestionDate = "📅 {date}"
private const val suggestionFullDate = "📅 {fullDate}"
private const val suggestionTimer = "⏳ {timer}"
private const val suggestionStopwatch = "⏱️ {stopwatch}"
private const val suggestionWeather = "{conditions} {temperature}"
private val suggestionTravel =
    "$suggestionWeather\n$suggestionTime\n$suggestionCity\n$suggestionMovement"
private val suggestionDebug = "{time}\n{bitrateAndTotal}\n{debugOverlay}"
private val suggestionTesla = "🚗 Tesla\n⚙️ {teslaDrive}\n🔋 {teslaBatteryLevel}\n🔈 {teslaMedia}"
private const val suggestionRacing = "🏎️ Racing 🏎️\n{lapTimes}"

private val suggestions: List<Suggestion> = createSuggestions()

private fun createSuggestions(): List<Suggestion> {
    val result = mutableListOf(
        Suggestion(id = 0, name = "Travel", text = suggestionTravel),
        Suggestion(id = 1, name = "Weather", text = suggestionWeather),
        Suggestion(id = 2, name = "Time", text = suggestionTime),
        Suggestion(id = 3, name = "Date", text = suggestionDate),
        Suggestion(id = 4, name = "Full date", text = suggestionFullDate),
        Suggestion(id = 5, name = "Timer", text = suggestionTimer),
        Suggestion(id = 6, name = "Stopwatch", text = suggestionStopwatch),
        Suggestion(id = 7, name = "City", text = suggestionCity),
        Suggestion(id = 8, name = "Country", text = suggestionCountry),
        Suggestion(id = 9, name = "Movement", text = suggestionMovement),
    )
    if (isPhone()) {
        result += Suggestion(id = 10, name = "Apple workout heart rate", text = suggestionHeartRate)
    }
    result += listOf(
        Suggestion(id = 11, name = "Apple workout step count", text = suggestionStepCount),
        Suggestion(id = 12, name = "Subtitles", text = suggestionSubtitles),
        Suggestion(id = 13, name = "Muted", text = suggestionMuted),
        Suggestion(id = 14, name = "Debug", text = suggestionDebug),
        Suggestion(id = 15, name = "Tesla", text = suggestionTesla),
        Suggestion(id = 16, name = "Racing", text = suggestionRacing),
    )
    return result
}

private val chatBotSuggestions: List<Suggestion> = createChatBotSuggestions()

private fun createChatBotSuggestions(): List<Suggestion> = listOf(
    Suggestion(id = 0, name = "Travel", text = suggestionTravel.replace("\n", " ")),
    Suggestion(id = 1, name = "Debug", text = suggestionDebug.replace("\n", " ")),
)

@Composable
private fun SuggestionView(
    suggestion: Suggestion,
    widget: Boolean,
    text: String,
    onChange: (String) -> Unit,
    dismiss: () -> Unit,
) {
    var presentingConfirmation by remember { mutableStateOf(false) }

    fun submit() {
        onChange(suggestion.text)
        dismiss()
    }

    fun confirmationTitle(): String = if (widget) {
        localized("Are you sure you want to replace the content of the current text widget?")
    } else {
        localized("Are you sure you want to replace the text of the current command?")
    }

    Column(horizontalAlignment = Alignment.Start) {
        TextButton(onClick = {
            if (text.isEmpty()) {
                submit()
            } else {
                presentingConfirmation = true
            }
        }) {
            Text(localized(suggestion.name), style = MaterialTheme.typography.titleMedium)
        }
        if (presentingConfirmation) {
            AlertDialog(
                onDismissRequest = { presentingConfirmation = false },
                title = { Text(confirmationTitle()) },
                confirmButton = {
                    TextButton(onClick = { submit() }) {
                        Text(localized("Yes"))
                    }
                },
            )
        }
        Text(suggestion.text)
    }
}

@Composable
private fun VariableView(
    model: Model = LocalModel.current,
    title: String,
    description: String,
    text: String,
    onChange: (String) -> Unit,
) {
    Column(horizontalAlignment = Alignment.Start) {
        TextButton(onClick = {
            onChange(text + title)
            model.makeToast(title = "Appended $title to text")
        }) {
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        Text(description)
    }
}

private data class Language(
    val identifier: String,
    val name: String,
    val status: Any?,
)

@Composable
private fun SubtitlesWithLanguageView(
    model: Model = LocalModel.current,
    text: String,
    onChange: (String) -> Unit,
) {
    TODO("no Android counterpart for Translation")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VariableWithUnitView(
    model: Model = LocalModel.current,
    description: String,
    variable: String,
    units: List<Pair<String, String>>,
    text: String,
    onChange: (String) -> Unit,
) {
    var presentingPicker by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.Start) {
        TextButton(onClick = { presentingPicker = true }) {
            Text("{$variable:<unit>}", style = MaterialTheme.typography.titleMedium)
        }
        Text(description)
    }
    if (presentingPicker) {
        ModalBottomSheet(onDismissRequest = { presentingPicker = false }) {
            Column {
                TopAppBar(
                    title = { Text(localized("Unit")) },
                    navigationIcon = {
                        IconButton(onClick = { presentingPicker = false }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    },
                )
                units.forEach { (name, symbol) ->
                    TextButton(onClick = {
                        val value = "{$variable:$symbol}"
                        onChange(text + value)
                        model.makeToast(title = "Appended $value to text")
                        presentingPicker = false
                    }) {
                        Text(name)
                    }
                }
            }
        }
    }
}

@Composable
private fun VariableWithLengthUnitView(
    model: Model = LocalModel.current,
    description: String,
    variable: String,
    text: String,
    onChange: (String) -> Unit,
) {
    fun units(): List<Pair<String, String>> =
        TextFormatLengthUnit.entries
            .filter { it != TextFormatLengthUnit.system }
            .map { it.toString() to it.symbol() }

    VariableWithUnitView(
        model = model,
        description = description,
        variable = variable,
        units = units(),
        text = text,
        onChange = onChange,
    )
}

@Composable
private fun VariableWithSpeedUnitView(
    model: Model = LocalModel.current,
    description: String,
    variable: String,
    text: String,
    onChange: (String) -> Unit,
) {
    fun units(): List<Pair<String, String>> =
        TextFormatSpeedUnit.entries
            .filter { it != TextFormatSpeedUnit.system }
            .map { it.toString() to it.symbol() }

    VariableWithUnitView(
        model = model,
        description = description,
        variable = variable,
        units = units(),
        text = text,
        onChange = onChange,
    )
}

@Composable
private fun VariableWithTemperatureUnitView(
    model: Model = LocalModel.current,
    description: String,
    variable: String,
    text: String,
    onChange: (String) -> Unit,
) {
    fun units(): List<Pair<String, String>> =
        TextFormatTemperatureUnit.entries
            .filter { it != TextFormatTemperatureUnit.system }
            .map { it.toString() to it.symbol() }

    VariableWithUnitView(
        model = model,
        description = description,
        variable = variable,
        units = units(),
        text = text,
        onChange = onChange,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> PickerRow(
    selection: T,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelectionChange: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = optionLabel(selection),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelectionChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun TimeComponentPickerView(
    title: String,
    range: IntRange,
    time: Int,
    onChange: (Int) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(localized(title))
        PickerRow(
            selection = time,
            options = range.toList(),
            optionLabel = { it.toString() },
            onSelectionChange = onChange,
            modifier = Modifier.size(width = 100.dp, height = 150.dp),
        )
    }
}

@Composable
fun TimeButtonView(
    text: String,
    action: () -> Unit,
) {
    Button(onClick = action, modifier = Modifier.size(width = 100.dp, height = 30.dp)) {
        Text(localized(text))
    }
}

@Composable
private fun TimePickerView(
    time: Double,
    onSet: (Double) -> Unit,
    onCancel: () -> Unit,
) {
    val timeInt = time.toInt()
    var seconds by remember { mutableStateOf(timeInt % 60) }
    var minutes by remember { mutableStateOf((timeInt / 60) % 60) }
    var hours by remember { mutableStateOf(min(timeInt / 3600, 23)) }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(modifier = Modifier.padding(16.dp)) {
            TimeComponentPickerView(
                title = "Hours",
                range = 0 until 24,
                time = hours,
                onChange = { hours = it },
            )
            TimeComponentPickerView(
                title = "Minutes",
                range = 0 until 60,
                time = minutes,
                onChange = { minutes = it },
            )
            TimeComponentPickerView(
                title = "Seconds",
                range = 0 until 60,
                time = seconds,
                onChange = { seconds = it },
            )
        }
        Row(modifier = Modifier.padding(16.dp)) {
            TimeButtonView(text = "Set") {
                onSet((hours * 3600 + minutes * 60 + seconds).toDouble())
            }
            TimeButtonView(text = "Cancel") {
                onCancel()
            }
        }
    }
}

@Composable
private fun TimerWidgetView(
    name: String,
    timer: SettingsWidgetTextTimer,
    index: Int,
    textEffects: List<TextEffect>,
    indented: Boolean,
) {
    var presentingSetTime by remember { mutableStateOf(false) }
    val delta by timer.delta.collectAsState()

    fun updateTextEffect() {
        for (effect in textEffects) {
            effect.setEndTime(index = index, endTime = timer.textEffectEndTime())
        }
    }

    Row {
        if (indented) {
            Text("")
            Text("", modifier = Modifier.width(iconWidth))
        }
        Column(horizontalAlignment = Alignment.Start) {
            Row {
                Text(name)
                Spacer(Modifier.weight(1f))
                Text(timer.format())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                PickerRow(
                    selection = delta,
                    options = listOf(1, 2, 5, 15, 60),
                    optionLabel = { formatShortDuration(60 * it) },
                    onSelectionChange = { timer.delta.value = it },
                )
                IconButton(onClick = {
                    timer.add(delta = -60.0 * delta)
                    updateTextEffect()
                }) {
                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = {
                    timer.add(delta = 60.0 * delta)
                    updateTextEffect()
                }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = { presentingSetTime = true }) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
    if (presentingSetTime) {
        Dialog(onDismissRequest = { presentingSetTime = false }) {
            Surface {
                TimePickerView(
                    time = timer.timeLeft(),
                    onSet = { time ->
                        timer.set(time = time)
                        updateTextEffect()
                        presentingSetTime = false
                    },
                    onCancel = { presentingSetTime = false },
                )
            }
        }
    }
}

@Composable
private fun StopwatchWidgetView(
    name: String,
    stopwatch: SettingsWidgetTextStopwatch,
    index: Int,
    textEffects: List<TextEffect>,
    indented: Boolean,
) {
    var presentingSetTime by remember { mutableStateOf(false) }
    val running by stopwatch.running.collectAsState()

    fun updateTextEffect() {
        for (effect in textEffects) {
            effect.setStopwatch(index = index, stopwatch = stopwatch.clone())
        }
    }

    Row {
        if (indented) {
            Text("")
            Text("", modifier = Modifier.width(iconWidth))
        }
        Column(horizontalAlignment = Alignment.Start) {
            Row {
                Text(name)
                Spacer(Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { presentingSetTime = true }) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = {
                    stopwatch.totalElapsed.value = 0.0
                    stopwatch.running.value = false
                    updateTextEffect()
                }) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = {
                    stopwatch.running.value = !running
                    if (stopwatch.running.value) {
                        stopwatch.playPressedTime.value = Instant.now()
                    } else {
                        stopwatch.totalElapsed.value += Duration
                            .between(stopwatch.playPressedTime.value, Instant.now())
                            .toMillis() / 1000.0
                    }
                    updateTextEffect()
                }) {
                    Icon(
                        imageVector = if (running) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(35.dp),
                    )
                }
            }
        }
    }
    if (presentingSetTime) {
        Dialog(onDismissRequest = { presentingSetTime = false }) {
            Surface {
                TimePickerView(
                    time = stopwatch.currentTime(),
                    onSet = { time ->
                        stopwatch.playPressedTime.value = Instant.now()
                        stopwatch.totalElapsed.value = time
                        updateTextEffect()
                        presentingSetTime = false
                    },
                    onCancel = { presentingSetTime = false },
                )
            }
        }
    }
}

@Composable
private fun CheckboxWidgetView(
    name: String,
    checkbox: SettingsWidgetTextCheckbox,
    index: Int,
    textEffects: List<TextEffect>,
    indented: Boolean,
) {
    val checked by checkbox.checked.collectAsState()
    var image by remember {
        mutableStateOf(if (checkbox.checked.value) "checkmark.square" else "square")
    }

    fun updateTextEffect() {
        for (effect in textEffects) {
            effect.setCheckbox(index = index, checked = checkbox.checked.value)
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (indented) {
            Text("")
            Text("", modifier = Modifier.width(iconWidth))
        }
        Text(name)
        Spacer(Modifier.weight(1f))
        IconButton(onClick = {
            checkbox.checked.value = !checked
            image = if (checkbox.checked.value) "checkmark.square" else "square"
            updateTextEffect()
        }) {
            Icon(
                imageVector = if (image == "checkmark.square") {
                    Icons.Default.CheckBox
                } else {
                    Icons.Default.CheckBoxOutlineBlank
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun RatingWidgetView(
    name: String,
    rating: SettingsWidgetTextRating,
    index: Int,
    textEffects: List<TextEffect>,
    indented: Boolean,
) {
    var ratingSelection by remember { mutableStateOf(rating.rating.value) }

    fun updateTextEffect() {
        for (effect in textEffects) {
            effect.setRating(index = index, rating = rating.rating.value)
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (indented) {
            Text("")
            Text("", modifier = Modifier.width(iconWidth))
        }
        PickerRow(
            selection = ratingSelection,
            options = (0 until 6).toList(),
            optionLabel = { it.toString() },
            onSelectionChange = {
                ratingSelection = it
                rating.rating.value = it
                updateTextEffect()
            },
        )
        Text(name)
    }
}

@Composable
private fun LapTimesWidgetView(
    name: String,
    lapTimes: SettingsWidgetTextLapTimes,
    index: Int,
    textEffects: List<TextEffect>,
    indented: Boolean,
) {
    fun updateTextEffect() {
        for (effect in textEffects) {
            effect.setLapTimes(index = index, lapTimes = lapTimes.lapTimes.value)
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (indented) {
            Text("")
            Text("", modifier = Modifier.width(iconWidth))
        }
        Text(name)
        Spacer(Modifier.weight(1f))
        IconButton(onClick = {
            lapTimes.currentLapStartTime.value = null
            lapTimes.lapTimes.value = mutableListOf()
            updateTextEffect()
        }) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(24.dp))
        }
        IconButton(onClick = {
            val now = Instant.now().toEpochMilli() / 1000.0
            val lastIndex = lapTimes.lapTimes.value.size - 1
            val currentLapStartTime = lapTimes.currentLapStartTime.value
            if (lastIndex >= 0 && currentLapStartTime != null) {
                lapTimes.lapTimes.value[lastIndex] = now - currentLapStartTime
            }
            lapTimes.currentLapStartTime.value = now
            lapTimes.lapTimes.value.add(0.0)
            updateTextEffect()
        }) {
            Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(24.dp))
        }
        IconButton(onClick = {
            val currentLapStartTime = lapTimes.currentLapStartTime.value
            if (currentLapStartTime != null) {
                val lastIndex = lapTimes.lapTimes.value.size - 1
                if (lastIndex >= 0) {
                    val now = Instant.now().toEpochMilli() / 1000.0
                    lapTimes.lapTimes.value[lastIndex] = now - currentLapStartTime
                }
                lapTimes.currentLapStartTime.value = null
                lapTimes.lapTimes.value.add(Double.POSITIVE_INFINITY)
            }
            updateTextEffect()
        }) {
            Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun TextWidgetSuggestionsInnerView(
    widget: Boolean,
    text: String,
    onChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Column {
        Text(localized("Suggestions"), style = MaterialTheme.typography.titleLarge)
        val items = if (widget) suggestions else chatBotSuggestions
        items.forEach { suggestion ->
            SuggestionView(
                suggestion = suggestion,
                widget = widget,
                text = text,
                onChange = onChange,
                dismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun GeneralVariablesView(
    model: Model = LocalModel.current,
    widget: Boolean,
    value: String,
    onChange: (String) -> Unit,
) {
    Column {
        Text(localized("General"), style = MaterialTheme.typography.titleLarge)
        if (widget) {
            VariableView(
                model = model,
                title = "{checkbox}",
                description = localized("Show a checkbox"),
                text = value,
                onChange = onChange,
            )
            VariableView(
                model = model,
                title = "{rating}",
                description = localized("Show a 0-5 rating"),
                text = value,
                onChange = onChange,
            )
        }
        VariableView(
            model = model,
            title = "{muted}",
            description = localized("Show muted"),
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{browserTitle}",
            description = localized("Show browser title"),
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{gForce}",
            description = localized("Show G-force"),
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{gForceRecentMax}",
            description = localized("Show recent max G-force"),
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{gForceMax}",
            description = localized("Show max G-force"),
            text = value,
            onChange = onChange,
        )
    }
}

@Composable
private fun TimeVariablesView(
    model: Model = LocalModel.current,
    widget: Boolean,
    value: String,
    onChange: (String) -> Unit,
) {
    Column {
        Text(localized("Time"), style = MaterialTheme.typography.titleLarge)
        val now = Instant.now()
        val time = textEffectTimeFormat.format(now)
        VariableView(
            model = model,
            title = "{time}",
            description = localized("Show time as $time"),
            text = value,
            onChange = onChange,
        )
        val shortTime = textEffectShortTimeFormat.format(now)
        VariableView(
            model = model,
            title = "{shortTime}",
            description = localized("Show time as $shortTime"),
            text = value,
            onChange = onChange,
        )
        val date = textEffectDateFormatter.format(now)
        VariableView(
            model = model,
            title = "{date}",
            description = localized("Show date as $date"),
            text = value,
            onChange = onChange,
        )
        val fullDate = textEffectFullDateFormatter.format(now)
        VariableView(
            model = model,
            title = "{fullDate}",
            description = localized("Show date as $fullDate"),
            text = value,
            onChange = onChange,
        )
        if (widget) {
            VariableView(
                model = model,
                title = "{timer}",
                description = localized("Show a timer"),
                text = value,
                onChange = onChange,
            )
            VariableView(
                model = model,
                title = "{stopwatch}",
                description = localized("Show a stopwatch"),
                text = value,
                onChange = onChange,
            )
            VariableView(
                model = model,
                title = "{lapTimes}",
                description = localized("Show lap times"),
                text = value,
                onChange = onChange,
            )
        }
    }
}

@Composable
private fun LocationVariablesView(
    model: Model = LocalModel.current,
    value: String,
    onChange: (String) -> Unit,
) {
    Column {
        Text(localized("Location"), style = MaterialTheme.typography.titleLarge)
        VariableView(
            model = model,
            title = "{country}",
            description = localized("Show country"),
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{countryFlag}",
            description = localized("Show country flag"),
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{state}",
            description = localized("Show state"),
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{area}",
            description = localized("Show area"),
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{city}",
            description = localized("Show city"),
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{neighborhood}",
            description = localized("Show neighborhood"),
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{speed}",
            description = localized("Show speed"),
            text = value,
            onChange = onChange,
        )
        VariableWithSpeedUnitView(
            model = model,
            description = localized("Show speed in given unit"),
            variable = "speed",
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{averageSpeed}",
            description = localized("Show average speed"),
            text = value,
            onChange = onChange,
        )
        VariableWithSpeedUnitView(
            model = model,
            description = localized("Show average speed in given unit"),
            variable = "averageSpeed",
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{altitude}",
            description = localized("Show altitude"),
            text = value,
            onChange = onChange,
        )
        VariableWithLengthUnitView(
            model = model,
            description = localized("Show altitude in given unit"),
            variable = "altitude",
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{distance}",
            description = localized("Show distance"),
            text = value,
            onChange = onChange,
        )
        VariableWithLengthUnitView(
            model = model,
            description = localized("Show distance in given unit"),
            variable = "distance",
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{splitDistance}",
            description = localized("Show split distance"),
            text = value,
            onChange = onChange,
        )
        VariableWithLengthUnitView(
            model = model,
            description = localized("Show split distance in given unit"),
            variable = "splitDistance",
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{altitudeAscent}",
            description = localized("Show altitude ascent"),
            text = value,
            onChange = onChange,
        )
        VariableWithLengthUnitView(
            model = model,
            description = localized("Show altitude ascent in given unit"),
            variable = "altitudeAscent",
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{altitudeDescent}",
            description = localized("Show altitude descent"),
            text = value,
            onChange = onChange,
        )
        VariableWithLengthUnitView(
            model = model,
            description = localized("Show altitude descent in given unit"),
            variable = "altitudeDescent",
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{splitAltitudeAscent}",
            description = localized("Show split altitude ascent"),
            text = value,
            onChange = onChange,
        )
        VariableWithLengthUnitView(
            model = model,
            description = localized("Show split altitude ascent in given unit"),
            variable = "splitAltitudeAscent",
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{splitAltitudeDescent}",
            description = localized("Show split altitude descent"),
            text = value,
            onChange = onChange,
        )
        VariableWithLengthUnitView(
            model = model,
            description = localized("Show split altitude descent in given unit"),
            variable = "splitAltitudeDescent",
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{slope}",
            description = localized("Show slope"),
            text = value,
            onChange = onChange,
        )
    }
}

@Composable
private fun WeatherVariablesView(
    model: Model = LocalModel.current,
    value: String,
    onChange: (String) -> Unit,
) {
    Column {
        Text(localized("Weather"), style = MaterialTheme.typography.titleLarge)
        VariableView(
            model = model,
            title = "{conditions}",
            description = localized("Show conditions"),
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{temperature}",
            description = localized("Show temperature"),
            text = value,
            onChange = onChange,
        )
        VariableWithTemperatureUnitView(
            model = model,
            description = localized("Show temperature in given unit"),
            variable = "temperature",
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{feelsLikeTemperature}",
            description = localized("Show feels like temperature"),
            text = value,
            onChange = onChange,
        )
        VariableWithTemperatureUnitView(
            model = model,
            description = localized("Show feels like temperature in given unit"),
            variable = "feelsLikeTemperature",
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{wind}",
            description = localized("Show wind"),
            text = value,
            onChange = onChange,
        )
        VariableWithSpeedUnitView(
            model = model,
            description = localized("Show wind in given unit"),
            variable = "wind",
            text = value,
            onChange = onChange,
        )
        Text(
            localized(
                "Weather data is provided by Apple Weather. " +
                    "[Legal information](https://weatherkit.apple.com/legal-attribution.html).",
            ),
        )
    }
}

@Composable
private fun LanguageVariablesView(
    model: Model = LocalModel.current,
    value: String,
    onChange: (String) -> Unit,
) {
    Column {
        Text(localized("Language"), style = MaterialTheme.typography.titleLarge)
        VariableView(
            model = model,
            title = "{subtitles}",
            description = localized("Show subtitles in app language"),
            text = value,
            onChange = onChange,
        )
        SubtitlesWithLanguageView(model = model, text = value, onChange = onChange)
    }
}

@Composable
private fun WorkoutVariablesView(
    model: Model = LocalModel.current,
    value: String,
    onChange: (String) -> Unit,
) {
    val database by model.database.collectAsState()
    Column {
        Text(localized("Workout"), style = MaterialTheme.typography.titleLarge)
        Text(localized("Apple workout"), style = MaterialTheme.typography.titleSmall)
        if (isPhone()) {
            VariableView(
                model = model,
                title = "{heartRate}",
                description = localized("Show heart rate."),
                text = value,
                onChange = onChange,
            )
        }
        VariableView(
            model = model,
            title = "{stepCount}",
            description = localized("Show step count."),
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{workoutDistance}",
            description = localized("Show distance."),
            text = value,
            onChange = onChange,
        )
        Text(localized("Other devices"), style = MaterialTheme.typography.titleSmall)
        database.workoutDevices.devices.forEach { device ->
            VariableView(
                model = model,
                title = "{heartRate:${device.name}}",
                description = localized(
                    "Show heart rate for heart rate device called \"${device.name}\"",
                ),
                text = value,
                onChange = onChange,
            )
            VariableView(
                model = model,
                title = "{runningPace:${device.name}}",
                description = localized("Show running pace"),
                text = value,
                onChange = onChange,
            )
            VariableView(
                model = model,
                title = "{runningCadence:${device.name}}",
                description = localized("Show running cadence"),
                text = value,
                onChange = onChange,
            )
            VariableView(
                model = model,
                title = "{runningDistance:${device.name}}",
                description = localized("Show running distance"),
                text = value,
                onChange = onChange,
            )
        }
        VariableView(
            model = model,
            title = "{cyclingPower}",
            description = localized("Show cycling power"),
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{cyclingCadence}",
            description = localized("Show cycling cadence"),
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{cyclingSpeed}",
            description = localized("Show cycling speed"),
            text = value,
            onChange = onChange,
        )
    }
}

@Composable
private fun TeslaVariablesView(
    model: Model = LocalModel.current,
    value: String,
    onChange: (String) -> Unit,
) {
    Column {
        Text(localized("Tesla"), style = MaterialTheme.typography.titleLarge)
        VariableView(
            model = model,
            title = "{teslaBatteryLevel}",
            description = localized("Show Tesla battery level"),
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{teslaDrive}",
            description = localized("Show Tesla drive information"),
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{teslaMedia}",
            description = localized("Show Tesla media information"),
            text = value,
            onChange = onChange,
        )
    }
}

@Composable
private fun StreamingVariablesView(
    model: Model = LocalModel.current,
    value: String,
    onChange: (String) -> Unit,
) {
    Column {
        Text(localized("Streaming"), style = MaterialTheme.typography.titleLarge)
        VariableView(
            model = model,
            title = "{latestSubscriber}",
            description = localized("Show latest subscriber"),
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{latestFollower}",
            description = localized("Show latest follower"),
            text = value,
            onChange = onChange,
        )
    }
}

@Composable
private fun DebugVariablesView(
    model: Model = LocalModel.current,
    value: String,
    onChange: (String) -> Unit,
) {
    Column {
        Text(localized("Debug"), style = MaterialTheme.typography.titleLarge)
        VariableView(
            model = model,
            title = "{bitrate}",
            description = localized("Show bitrate"),
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{bitrateAndTotal}",
            description = localized("Show bitrate and total number of bytes sent"),
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{bonding}",
            description = localized("Show bonding percentage split"),
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{resolution}",
            description = localized("Show resolution"),
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{fps}",
            description = localized("Show FPS"),
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{debugOverlay}",
            description = localized("Show debug overlay (if enabled)"),
            text = value,
            onChange = onChange,
        )
        VariableView(
            model = model,
            title = "{systemMonitor}",
            description = localized("Show system monitor (if enabled)"),
            text = value,
            onChange = onChange,
        )
    }
}

@Composable
fun TextFormatVariablesView(
    model: Model = LocalModel.current,
    widget: Boolean,
    value: String,
    onChange: (String) -> Unit,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Column {
        Text(localized("Variables"), style = MaterialTheme.typography.titleSmall)
        TextButton(onClick = { onNavigate("General") }) {
            Text(localized("General"))
        }
        TextButton(onClick = { onNavigate("Time") }) {
            Text(localized("Time"))
        }
        TextButton(onClick = { onNavigate("Location") }) {
            Text(localized("Location"))
        }
        TextButton(onClick = { onNavigate("Weather") }) {
            Text(localized("Weather"))
        }
        if (widget) {
            TextButton(onClick = { onNavigate("Language") }) {
                Text(localized("Language"))
            }
        }
        TextButton(onClick = { onNavigate("Workout") }) {
            Text(localized("Workout"))
        }
        TextButton(onClick = { onNavigate("Tesla") }) {
            Text(localized("Tesla"))
        }
        TextButton(onClick = { onNavigate("Streaming") }) {
            Text(localized("Streaming"))
        }
        TextButton(onClick = { onNavigate("Debug") }) {
            Text(localized("Debug"))
        }
    }
}

@Composable
fun TextSelectionView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    text: SettingsWidgetText,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val value by text.formatString.collectAsState()
    val database by model.database.collectAsState()
    val onValueChange: (String) -> Unit = { newValue ->
        text.formatString.value = newValue
        model.textWidgetTextChanged(widget = widget)
    }
    Column {
        Text(localized("Text"), style = MaterialTheme.typography.titleLarge)
        TextWidgetTextView(value = value, onChange = onValueChange)
        TextFormatWarningsView(
            model = model,
            location = database.location,
            value = value,
            onChange = onValueChange,
        )
        TextWidgetSuggestionsView(
            widget = true,
            text = value,
            onChange = onValueChange,
            onNavigate = onNavigate,
        )
        TextFormatVariablesView(
            model = model,
            widget = true,
            value = value,
            onChange = onValueChange,
            onNavigate = onNavigate,
        )
    }
}

@Composable
fun TextWidgetTextView(
    value: String,
    onChange: (String) -> Unit,
) {
    var editingText by remember { mutableStateOf(false) }
    Column {
        MultiLineTextFieldView(
            value = value,
            placeholder = localized("My text"),
            onChange = onChange,
        )
        MultiLineTextFieldDoneButtonView(
            editingText = editingText,
            onChange = { editingText = it },
        )
    }
}

@Composable
fun TextFormatWarningsView(
    model: Model = LocalModel.current,
    location: SettingsLocation,
    value: String,
    onChange: (String) -> Unit,
) {
    val textFormat = loadTextFormat(format = value)
    val enabled by location.enabled.collectAsState()
    val workoutType by model.workoutType.collectAsState()
    Column {
        if (textFormat.isWorkoutVariable() && workoutType == null) {
            Text(
                localized(
                    "⚠️ Start a workout using the Workout quick button or the Start workout " +
                        "button on your watch to update workout variables.",
                ),
            )
        }
        if (textFormat.isLocationVariable() && !enabled) {
            Text(localized("⚠️ Enable Location to update location variables."))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(localized("Location"))
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        location.enabled.value = it
                        model.reloadLocation()
                    },
                )
            }
        }
        if (textFormat.isWeatherVariable() && !enabled) {
            Text(localized("⚠️ Enable Location to update weather variables."))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(localized("Location"))
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        location.enabled.value = it
                        model.reloadLocation()
                    },
                )
            }
        }
    }
}

@Composable
fun WidgetTextQuickButtonControlsView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    text: SettingsWidgetText,
) {
    val textEffects = model.getTextEffects(id = widget.id)
    val timers by text.timers.collectAsState()
    val stopwatches by text.stopwatches.collectAsState()
    val checkboxes by text.checkboxes.collectAsState()
    val ratings by text.ratings.collectAsState()
    val lapTimes by text.lapTimes.collectAsState()
    val formatString by text.formatString.collectAsState()
    if (textEffects.isNotEmpty()) {
        val textFormat = loadTextFormat(format = formatString)
        Column {
            timers.forEach { timer ->
                val index = timers.indexOfFirst { it === timer }.takeIf { it >= 0 } ?: 0
                TimerWidgetView(
                    name = localized("Timer ${index + 1}"),
                    timer = timer,
                    index = index,
                    textEffects = textEffects,
                    indented = true,
                )
            }
            stopwatches.forEach { stopwatch ->
                val index = stopwatches.indexOfFirst { it === stopwatch }.takeIf { it >= 0 } ?: 0
                StopwatchWidgetView(
                    name = localized("Stopwatch ${index + 1}"),
                    stopwatch = stopwatch,
                    index = index,
                    textEffects = textEffects,
                    indented = true,
                )
            }
            checkboxes.forEach { checkbox ->
                val index = checkboxes.indexOfFirst { it === checkbox }.takeIf { it >= 0 } ?: 0
                CheckboxWidgetView(
                    name = textFormat.getCheckboxText(index = index),
                    checkbox = checkbox,
                    index = index,
                    textEffects = textEffects,
                    indented = true,
                )
            }
            ratings.forEach { rating ->
                val index = ratings.indexOfFirst { it === rating }.takeIf { it >= 0 } ?: 0
                RatingWidgetView(
                    name = localized("Rating ${index + 1}"),
                    rating = rating,
                    index = index,
                    textEffects = textEffects,
                    indented = true,
                )
            }
            lapTimes.forEach { lapTime ->
                val index = lapTimes.indexOfFirst { it === lapTime }.takeIf { it >= 0 } ?: 0
                LapTimesWidgetView(
                    name = localized("Lap times ${index + 1}"),
                    lapTimes = lapTime,
                    index = index,
                    textEffects = textEffects,
                    indented = true,
                )
            }
        }
    }
}

@Composable
fun TextWidgetSuggestionsView(
    widget: Boolean,
    text: String,
    onChange: (String) -> Unit,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Row(modifier = Modifier.clickable { onNavigate("Suggestions") }) {
        Text(localized("Suggestions"))
    }
}

@Composable
private fun FontFamilyPickerView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    text: SettingsWidgetText,
) {
    var fontFamilies by remember { mutableStateOf(listOf<String>()) }

    fun selectFamily(family: String?) {
        text.fontFamily.value = family
        if (family != null) {
            text.fontStyle.value = fontStyles(fontFamily = family).firstOrNull() ?: ""
        }
        for (effect in model.getTextEffects(id = widget.id)) {
            effect.setFontFamily(family = text.fontFamily.value)
            effect.setFontStyle(style = text.fontStyle.value)
        }
        model.remoteSceneSettingsUpdated()
    }

    LaunchedEffect(Unit) {
        if (fontFamilies.isEmpty()) {
            fontFamilies = loadFontFamilies()
        }
    }
    Column {
        Text(localized("Family"), style = MaterialTheme.typography.titleLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(localized("System"))
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { selectFamily(null) }) {
                if (text.fontFamily.value == null) {
                    Icon(Icons.Default.Check, contentDescription = null)
                }
            }
        }
        fontFamilies.forEach { family ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(family)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { selectFamily(family) }) {
                    if (text.fontFamily.value == family) {
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                }
            }
        }
    }
}

private fun loadFontFamilies(): List<String> =
    TODO("no Android counterpart for UIKit font enumeration")

fun fontStyleName(family: String, fontName: String): String {
    val prefix = family.replace(" ", "")
    val name = fontName.replace("-", "")
    if (name.startsWith(prefix)) {
        val suffix = name.drop(prefix.length)
        return if (suffix.isEmpty()) "Regular" else suffix
    }
    return fontName
}

private fun fontStyles(fontFamily: String): List<String> =
    TODO("no Android counterpart for UIKit font enumeration")

@Composable
private fun FontStylePickerView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    text: SettingsWidgetText,
    fontFamily: String,
) {
    Column {
        Text(localized("Style"), style = MaterialTheme.typography.titleLarge)
        fontStyles(fontFamily = fontFamily).forEach { style ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(fontStyleName(family = fontFamily, fontName = style))
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {
                    text.fontStyle.value = style
                    for (effect in model.getTextEffects(id = widget.id)) {
                        effect.setFontStyle(style = text.fontStyle.value)
                    }
                    model.remoteSceneSettingsUpdated()
                }) {
                    if (text.fontStyle.value == style) {
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetTextSettingsView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    text: SettingsWidgetText,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    fun changeWidth(value: String): String? {
        val width = value.toIntOrNull() ?: return localized("Not a number")
        if (width <= 0) {
            return localized("Too small")
        }
        if (width >= 4000) {
            return localized("Too big")
        }
        return null
    }

    fun submitWidth(value: String) {
        val width = value.toIntOrNull() ?: return
        text.width.value = width
        setLayout()
    }

    fun changeCornerRadius(value: String): String? {
        val cornerRadius = value.toIntOrNull() ?: return localized("Not a number")
        if (cornerRadius < 0) {
            return localized("Too small")
        }
        if (cornerRadius >= 1000) {
            return localized("Too big")
        }
        return null
    }

    fun submitCornerRadius(value: String) {
        val cornerRadius = value.toIntOrNull() ?: return
        text.cornerRadius.value = cornerRadius
        setLayout()
    }

    fun setLayout() {
        for (effect in model.getTextEffects(id = widget.id)) {
            effect.setLayout(
                alignment = text.horizontalAlignment.value.toSystem(),
                width = if (text.widthEnabled.value) text.width.value else null,
                cornerRadius = text.cornerRadius.value.toDouble(),
            )
        }
        model.remoteSceneSettingsUpdated()
    }

    val database by model.database.collectAsState()
    val textEffects = model.getTextEffects(id = widget.id)
    val timers by text.timers.collectAsState()
    val stopwatches by text.stopwatches.collectAsState()
    val checkboxes by text.checkboxes.collectAsState()
    val ratings by text.ratings.collectAsState()
    val lapTimes by text.lapTimes.collectAsState()
    val formatString by text.formatString.collectAsState()
    val horizontalAlignment by text.horizontalAlignment.collectAsState()
    val widthEnabled by text.widthEnabled.collectAsState()
    val width by text.width.collectAsState()
    val cornerRadius by text.cornerRadius.collectAsState()
    val backgroundColorColor by text.backgroundColorColor.collectAsState()
    val foregroundColorColor by text.foregroundColorColor.collectAsState()
    val fontSizeFloat by text.fontSizeFloat.collectAsState()
    val fontFamily by text.fontFamily.collectAsState()
    val fontStyle by text.fontStyle.collectAsState()
    val fontDesign by text.fontDesign.collectAsState()
    val fontWeight by text.fontWeight.collectAsState()
    val fontMonospacedDigits by text.fontMonospacedDigits.collectAsState()
    val delay by text.delay.collectAsState()

    Column {
        Row(
            modifier = Modifier.clickable { onNavigate("Text") },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextItemLocalizedView(name = "Text", value = formatString)
        }
        TextFormatWarningsView(
            model = model,
            location = database.location,
            value = formatString,
            onChange = { text.formatString.value = it },
        )
        if (textEffects.isNotEmpty()) {
            if (timers.isNotEmpty()) {
                Text(localized("Timers"), style = MaterialTheme.typography.titleSmall)
                timers.forEach { timer ->
                    val index = timers.indexOfFirst { it === timer }.takeIf { it >= 0 } ?: 0
                    TimerWidgetView(
                        name = localized("Timer ${index + 1}"),
                        timer = timer,
                        index = index,
                        textEffects = textEffects,
                        indented = false,
                    )
                }
            }
            if (stopwatches.isNotEmpty()) {
                Text(localized("Stopwatches"), style = MaterialTheme.typography.titleSmall)
                stopwatches.forEach { stopwatch ->
                    val index = stopwatches.indexOfFirst { it === stopwatch }.takeIf { it >= 0 } ?: 0
                    StopwatchWidgetView(
                        name = localized("Stopwatch ${index + 1}"),
                        stopwatch = stopwatch,
                        index = index,
                        textEffects = textEffects,
                        indented = false,
                    )
                }
            }
            if (checkboxes.isNotEmpty()) {
                val textFormat = loadTextFormat(format = formatString)
                Text(localized("Checkboxes"), style = MaterialTheme.typography.titleSmall)
                checkboxes.forEach { checkbox ->
                    val index = checkboxes.indexOfFirst { it === checkbox }.takeIf { it >= 0 } ?: 0
                    CheckboxWidgetView(
                        name = textFormat.getCheckboxText(index = index),
                        checkbox = checkbox,
                        index = index,
                        textEffects = textEffects,
                        indented = false,
                    )
                }
            }
            if (ratings.isNotEmpty()) {
                Text(localized("Ratings"), style = MaterialTheme.typography.titleSmall)
                ratings.forEach { rating ->
                    val index = ratings.indexOfFirst { it === rating }.takeIf { it >= 0 } ?: 0
                    RatingWidgetView(
                        name = localized("Rating ${index + 1}"),
                        rating = rating,
                        index = index,
                        textEffects = textEffects,
                        indented = false,
                    )
                }
            }
            if (lapTimes.isNotEmpty()) {
                Text(localized("Lap times"), style = MaterialTheme.typography.titleSmall)
                lapTimes.forEach { lapTime ->
                    val index = lapTimes.indexOfFirst { it === lapTime }.takeIf { it >= 0 } ?: 0
                    LapTimesWidgetView(
                        name = localized("Lap times ${index + 1}"),
                        lapTimes = lapTime,
                        index = index,
                        textEffects = textEffects,
                        indented = false,
                    )
                }
            }
        }
        Text(localized("Colors"), style = MaterialTheme.typography.titleSmall)
        RgbColorPickerView(
            title = "Background",
            color = backgroundColorColor,
            opacity = true,
            onChange = { color ->
                text.backgroundColor.value = color
                for (effect in model.getTextEffects(id = widget.id)) {
                    effect.setBackgroundColor(color = color)
                }
                model.remoteSceneSettingsUpdated()
            },
        )
        RgbColorPickerView(
            title = "Foreground",
            color = foregroundColorColor,
            opacity = true,
            onChange = { color ->
                text.foregroundColor.value = color
                for (effect in model.getTextEffects(id = widget.id)) {
                    effect.setForegroundColor(color = color)
                }
                model.remoteSceneSettingsUpdated()
            },
        )
        Text(localized("Font"), style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(localized("Size"))
            Slider(
                value = fontSizeFloat,
                onValueChange = { value ->
                    text.fontSizeFloat.value = value
                    text.fontSize.value = value.toInt()
                    for (effect in model.getTextEffects(id = widget.id)) {
                        effect.setFontSize(size = value)
                    }
                    model.remoteSceneSettingsUpdated()
                },
                valueRange = 10f..200f,
                steps = 37,
            )
            Text(fontSizeFloat.toInt().toString(), modifier = Modifier.width(35.dp))
        }
        Row(
            modifier = Modifier.clickable { onNavigate("Family") },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(localized("Family"))
            Spacer(Modifier.weight(1f))
            GrayTextView(text = text.fontFamilyString())
        }
        if (fontFamily != null) {
            Row(
                modifier = Modifier
                    .clickable { onNavigate("Style") }
                    .clickable(enabled = fontStyles(fontFamily).size != 1) { onNavigate("Style") },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(localized("Style"))
                Spacer(Modifier.weight(1f))
                GrayTextView(text = text.fontStyleString())
            }
        } else {
            PickerRow(
                selection = fontDesign,
                options = SettingsFontDesign.entries.toList(),
                optionLabel = { it.toString() },
                onSelectionChange = { design ->
                    text.fontDesign.value = design
                    for (effect in model.getTextEffects(id = widget.id)) {
                        effect.setFontDesign(design = design.toSystem())
                    }
                    model.remoteSceneSettingsUpdated()
                },
            )
            PickerRow(
                selection = fontWeight,
                options = SettingsFontWeight.entries.toList(),
                optionLabel = { it.toString() },
                onSelectionChange = { weight ->
                    text.fontWeight.value = weight
                    for (effect in model.getTextEffects(id = widget.id)) {
                        effect.setFontWeight(weight = weight.toSystem())
                    }
                    model.remoteSceneSettingsUpdated()
                },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(localized("Monospaced digits"))
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = fontMonospacedDigits,
                    onCheckedChange = { enabled ->
                        text.fontMonospacedDigits.value = enabled
                        for (effect in model.getTextEffects(id = widget.id)) {
                            effect.setFontMonospacedDigits(enabled = enabled)
                        }
                        model.remoteSceneSettingsUpdated()
                    },
                )
            }
        }
        Text(localized("Layout"), style = MaterialTheme.typography.titleSmall)
        PickerRow(
            selection = horizontalAlignment,
            options = SettingsHorizontalAlignment.entries.toList(),
            optionLabel = { it.toString() },
            onSelectionChange = {
                text.horizontalAlignment.value = it
                setLayout()
            },
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(localized("Minimum width"))
            Spacer(Modifier.weight(1f))
            Switch(
                checked = widthEnabled,
                onCheckedChange = {
                    text.widthEnabled.value = it
                    setLayout()
                },
            )
            GrayTextView(text = width.toString())
        }
        TextEditNavigationView(
            title = localized("Minimum width"),
            value = width.toString(),
            onChange = { changeWidth(it) },
            onSubmit = { submitWidth(it) },
            keyboardType = KeyboardType.Number,
        )
        TextEditNavigationView(
            title = localized("Corner radius"),
            value = cornerRadius.toString(),
            onChange = { changeCornerRadius(it) },
            onSubmit = { submitCornerRadius(it) },
            keyboardType = KeyboardType.Number,
        )
        Text(localized("Delay"), style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = delay.toFloat(),
                onValueChange = { text.delay.value = it.toDouble() },
                valueRange = 0f..10f,
                steps = 19,
                onValueChangeFinished = { model.resetSelectedScene(changeScene = false) },
            )
            Text(delay.toString(), modifier = Modifier.width(35.dp))
        }
        Text(localized("To show the widget in sync with high latency cameras."))
    }
}
