package com.moblin.android.view.settings.scenes.widgets.widget.text

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.formatShortDuration
import com.moblin.android.common.various.iconWidth
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.IosSwitch
import com.moblin.android.platform.swiftui.LocalTint
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.getTextEffects
import com.moblin.android.various.model.reloadLocation
import com.moblin.android.various.model.remoteSceneSettingsUpdated
import com.moblin.android.various.model.resetSelectedScene
import com.moblin.android.various.model.textWidgetTextChanged
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
import com.moblin.android.videoeffects.text.getCheckboxText
import com.moblin.android.videoeffects.text.isLocationVariable
import com.moblin.android.videoeffects.text.isWeatherVariable
import com.moblin.android.videoeffects.text.isWorkoutVariable
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
import com.moblin.android.view.utils.TextEditView
import com.moblin.android.view.utils.TextItemLocalizedView
import java.time.Duration
import java.time.Instant
import kotlin.math.min

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
private fun RowIconButton(
    systemImage: String,
    width: Dp = 0.dp,
    tint: Color = LocalTint.current,
    action: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .then(if (width > 0.dp) Modifier.width(width) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = action,
            )
            .alpha(if (pressed) 0.2f else 1f)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        SystemImage(name = systemImage, fontSize = 28.sp, modifier = Modifier, tint = tint)
    }
}

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

    FormRow(onClick = {
        if (text.isEmpty()) {
            submit()
        } else {
            presentingConfirmation = true
        }
    }) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(localized(suggestion.name), style = TextStyle(fontSize = 20.sp))
            Text(suggestion.text)
        }
    }
    if (presentingConfirmation) {
        AlertDialog(
            onDismissRequest = { presentingConfirmation = false },
            title = {
                Text(
                    if (widget) {
                        localized(
                            "Are you sure you want to replace the content of the current text widget?",
                        )
                    } else {
                        localized("Are you sure you want to replace the text of the current command?")
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = { submit() }) {
                    Text(localized("Yes"), color = formPalette().red)
                }
            },
            dismissButton = {
                TextButton(onClick = { presentingConfirmation = false }) {
                    Text(localized("Cancel"))
                }
            },
        )
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
    FormRow(onClick = {
        onChange(text + title)
        model.makeToast(title = "Appended $title to text")
    }) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(title, style = TextStyle(fontSize = 20.sp))
            Text(description)
        }
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
    val languages = remember { mutableStateOf(listOf<Language>()) }.value
    var presentingLanguagePicker by remember { mutableStateOf(false) }
    FormRow(onClick = { presentingLanguagePicker = true }) {
        Column(horizontalAlignment = Alignment.Start) {
            Text("{subtitles:<language-identifier>}", style = TextStyle(fontSize = 20.sp))
            Text(localized("Show subtitles in given language"))
        }
    }
    if (presentingLanguagePicker) {
        Sheet(onDismissRequest = { presentingLanguagePicker = false }) {
            Form(title = localized("Subtitles language")) {
                Section {
                    Text(
                        localized(
                            "Download languages in iOS Settings → Apps → Translate → Languages.",
                        ),
                    )
                }
                Section {
                    languages.forEach { language ->
                        val value = "{subtitles:${language.identifier}}"
                        FormRow(onClick = {
                            onChange(text + value)
                            model.makeToast(title = "Appended $value to text")
                            presentingLanguagePicker = false
                        }) {
                            Text(language.name)
                        }
                    }
                }
            }
        }
    }
}

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
    FormRow(onClick = { presentingPicker = true }) {
        Column(horizontalAlignment = Alignment.Start) {
            Text("{$variable:<unit>}", style = TextStyle(fontSize = 20.sp))
            Text(description)
        }
    }
    if (presentingPicker) {
        Sheet(onDismissRequest = { presentingPicker = false }) {
            Form(title = localized("Unit")) {
                Section {
                    units.forEach { (name, symbol) ->
                        FormRow(onClick = {
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

@Composable
fun TimeComponentPickerView(
    title: String,
    range: IntRange,
    time: Int,
    onChange: (Int) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(localized(title))
        Picker(
            title = "",
            selection = time,
            options = range.toList(),
            text = { it.toString() },
            onChange = onChange,
        )
    }
}

@Composable
fun TimeButtonView(
    text: String,
    action: () -> Unit,
) {
    Box(modifier = Modifier.width(100.dp), contentAlignment = Alignment.Center) {
        FormRow(onClick = action) {
            Text(localized(text))
        }
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

    fun updateTextEffect() {
        for (effect in textEffects) {
            effect.setEndTime(index = index, endTime = timer.textEffectEndTime() as Long)
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (indented) {
            Text("")
            Text("", modifier = Modifier.width(iconWidth.dp))
        }
        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name)
                Spacer(Modifier.weight(1f))
                Text(timer.format())
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    Picker(
                        title = "",
                        selection = timer.delta,
                        options = listOf(1, 2, 5, 15, 60),
                        text = { formatShortDuration(seconds = 60 * it) },
                        onChange = { timer.delta = it },
                    )
                }
                RowIconButton(systemImage = "minus") {
                    timer.add(delta = -60.0 * timer.delta)
                    updateTextEffect()
                }
                RowIconButton(systemImage = "plus") {
                    timer.add(delta = 60.0 * timer.delta)
                    updateTextEffect()
                }
                RowIconButton(systemImage = "clock") {
                    presentingSetTime = true
                }
            }
        }
    }
    if (presentingSetTime) {
        Sheet(onDismissRequest = { presentingSetTime = false }) {
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

@Composable
private fun StopwatchWidgetView(
    name: String,
    stopwatch: SettingsWidgetTextStopwatch,
    index: Int,
    textEffects: List<TextEffect>,
    indented: Boolean,
) {
    var presentingSetTime by remember { mutableStateOf(false) }

    fun updateTextEffect() {
        for (effect in textEffects) {
            effect.setStopwatch(index = index, stopwatch = stopwatch.clone())
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (indented) {
            Text("")
            Text("", modifier = Modifier.width(iconWidth.dp))
        }
        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name)
                Spacer(Modifier.weight(1f))
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                RowIconButton(systemImage = "clock") {
                    presentingSetTime = true
                }
                RowIconButton(systemImage = "arrow.counterclockwise") {
                    stopwatch.totalElapsed = 0.0
                    stopwatch.running = false
                    updateTextEffect()
                }
                RowIconButton(
                    systemImage = if (stopwatch.running) "stop" else "play",
                    width = 35.dp,
                ) {
                    stopwatch.running = !stopwatch.running
                    if (stopwatch.running) {
                        stopwatch.playPressedTime = Instant.now()
                    } else {
                        stopwatch.totalElapsed += Duration
                            .between(stopwatch.playPressedTime, Instant.now())
                            .toMillis() / 1000.0
                    }
                    updateTextEffect()
                }
            }
        }
    }
    if (presentingSetTime) {
        Sheet(onDismissRequest = { presentingSetTime = false }) {
            TimePickerView(
                time = stopwatch.currentTime(),
                onSet = { time ->
                    stopwatch.playPressedTime = Instant.now()
                    stopwatch.totalElapsed = time
                    updateTextEffect()
                    presentingSetTime = false
                },
                onCancel = { presentingSetTime = false },
            )
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
    var checked by remember { mutableStateOf(checkbox.checked) }

    fun updateTextEffect() {
        for (effect in textEffects) {
            effect.setCheckbox(index = index, checked = checkbox.checked)
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (indented) {
            Text("")
            Text("", modifier = Modifier.width(iconWidth.dp))
        }
        Text(name)
        Spacer(Modifier.weight(1f))
        RowIconButton(systemImage = if (checked) "checkmark" else "square") {
            checked = !checked
            checkbox.checked = checked
            updateTextEffect()
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
    var ratingSelection by remember { mutableStateOf(rating.rating) }

    fun updateTextEffect() {
        for (effect in textEffects) {
            effect.setRating(index = index, rating = rating.rating)
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (indented) {
            Text("")
            Text("", modifier = Modifier.width(iconWidth.dp))
        }
        Box(modifier = Modifier.weight(1f)) {
            Picker(
                title = name,
                selection = ratingSelection,
                options = (0 until 6).toList(),
                text = { it.toString() },
                onChange = {
                    ratingSelection = it
                    rating.rating = it
                    updateTextEffect()
                },
            )
        }
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
            effect.setLapTimes(index = index, lapTimes = lapTimes.lapTimes.toMutableList())
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (indented) {
            Text("")
            Text("", modifier = Modifier.width(iconWidth.dp))
        }
        Text(name)
        Spacer(Modifier.weight(1f))
        RowIconButton(systemImage = "trash", tint = formPalette().red) {
            lapTimes.currentLapStartTime = null
            lapTimes.lapTimes = mutableListOf()
            updateTextEffect()
        }
        RowIconButton(systemImage = "stopwatch") {
            val now = Instant.now().toEpochMilli() / 1000.0
            val updated = lapTimes.lapTimes.toMutableList()
            val lastIndex = updated.size - 1
            val currentLapStartTime = lapTimes.currentLapStartTime
            if (lastIndex >= 0 && currentLapStartTime != null) {
                updated[lastIndex] = now - currentLapStartTime
            }
            lapTimes.currentLapStartTime = now
            updated.add(0.0)
            lapTimes.lapTimes = updated
            updateTextEffect()
        }
        RowIconButton(systemImage = "flag.checkered") {
            val currentLapStartTime = lapTimes.currentLapStartTime
            if (currentLapStartTime != null) {
                val updated = lapTimes.lapTimes.toMutableList()
                val lastIndex = updated.size - 1
                if (lastIndex >= 0) {
                    val now = Instant.now().toEpochMilli() / 1000.0
                    updated[lastIndex] = now - currentLapStartTime
                }
                lapTimes.currentLapStartTime = null
                updated.add(Double.POSITIVE_INFINITY)
                lapTimes.lapTimes = updated
            }
            updateTextEffect()
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
    Form(title = localized("Suggestions")) {
        Section {
            val items = if (widget) suggestions else chatBotSuggestions
            items.forEach { suggestion ->
                key(suggestion.id) {
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
    }
}

@Composable
private fun GeneralVariablesView(
    model: Model = LocalModel.current,
    widget: Boolean,
    value: String,
    onChange: (String) -> Unit,
) {
    NavigationLink(title = localized("General"), destination = {
        Form(title = localized("General")) {
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
    })
}

@Composable
private fun TimeVariablesView(
    model: Model = LocalModel.current,
    widget: Boolean,
    value: String,
    onChange: (String) -> Unit,
) {
    NavigationLink(title = localized("Time"), destination = {
        Form(title = localized("Time")) {
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
    })
}

@Composable
private fun LocationVariablesView(
    model: Model = LocalModel.current,
    value: String,
    onChange: (String) -> Unit,
) {
    NavigationLink(title = localized("Location"), destination = {
        Form(title = localized("Location")) {
            Section {
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
    })
}

@Composable
private fun WeatherVariablesView(
    model: Model = LocalModel.current,
    value: String,
    onChange: (String) -> Unit,
) {
    NavigationLink(title = localized("Weather"), destination = {
        Form(title = localized("Weather")) {
            Section(footerContent = {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        localized(
                            "Weather data is provided by Apple Weather. " +
                                "[Legal information](https://weatherkit.apple.com/legal-attribution.html).",
                        ),
                    )
                }
            }) {
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
            }
        }
    })
}

@Composable
private fun LanguageVariablesView(
    model: Model = LocalModel.current,
    value: String,
    onChange: (String) -> Unit,
) {
    NavigationLink(title = localized("Language"), destination = {
        Form(title = localized("Language")) {
            VariableView(
                model = model,
                title = "{subtitles}",
                description = localized("Show subtitles in app language"),
                text = value,
                onChange = onChange,
            )
            SubtitlesWithLanguageView(
                model = model,
                text = value,
                onChange = onChange,
            )
        }
    })
}

@Composable
private fun WorkoutVariablesView(
    model: Model = LocalModel.current,
    value: String,
    onChange: (String) -> Unit,
) {
    NavigationLink(title = localized("Workout"), destination = {
        Form(title = localized("Workout")) {
            Section(header = localized("Apple workout")) {
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
            }
            Section(header = localized("Other devices")) {
                model.database.workoutDevices.devices.forEach { device ->
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
    })
}

@Composable
private fun TeslaVariablesView(
    model: Model = LocalModel.current,
    value: String,
    onChange: (String) -> Unit,
) {
    NavigationLink(title = localized("Tesla"), destination = {
        Form(title = localized("Tesla")) {
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
    })
}

@Composable
private fun StreamingVariablesView(
    model: Model = LocalModel.current,
    value: String,
    onChange: (String) -> Unit,
) {
    NavigationLink(title = localized("Streaming"), destination = {
        Form(title = localized("Streaming")) {
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
    })
}

@Composable
private fun DebugVariablesView(
    model: Model = LocalModel.current,
    value: String,
    onChange: (String) -> Unit,
) {
    NavigationLink(title = localized("Debug"), destination = {
        Form(title = localized("Debug")) {
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
    })
}

@Composable
fun TextFormatVariablesView(
    model: Model = LocalModel.current,
    widget: Boolean,
    value: String,
    onChange: (String) -> Unit,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Section(header = localized("Variables")) {
        GeneralVariablesView(model = model, widget = widget, value = value, onChange = onChange)
        TimeVariablesView(model = model, widget = widget, value = value, onChange = onChange)
        LocationVariablesView(model = model, value = value, onChange = onChange)
        WeatherVariablesView(model = model, value = value, onChange = onChange)
        if (widget) {
            LanguageVariablesView(model = model, value = value, onChange = onChange)
        }
        WorkoutVariablesView(model = model, value = value, onChange = onChange)
        TeslaVariablesView(model = model, value = value, onChange = onChange)
        StreamingVariablesView(model = model, value = value, onChange = onChange)
        DebugVariablesView(model = model, value = value, onChange = onChange)
    }
}

@Composable
fun TextSelectionView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    text: SettingsWidgetText,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var value by remember { mutableStateOf(text.formatString) }
    val onValueChange: (String) -> Unit = { newValue ->
        value = newValue
        text.formatString = newValue
        model.textWidgetTextChanged(widget = widget)
    }
    Form(title = localized("Text")) {
        TextWidgetTextView(value = value, onChange = onValueChange)
        TextFormatWarningsView(
            model = model,
            location = model.database.location,
            value = value,
            onChange = onValueChange,
        )
        Section {
            TextWidgetSuggestionsView(
                widget = true,
                text = value,
                onChange = onValueChange,
                onNavigate = onNavigate,
            )
        }
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
    Section(footerContent = {
        MultiLineTextFieldDoneButtonView(
            editingText = editingText,
            onEditingTextChange = { editingText = it },
        )
    }) {
        MultiLineTextFieldView(
            value = value,
            placeholder = localized("My text"),
            onValueChange = onChange,
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
    val workoutType by model.workoutType.collectAsState()
    if (textFormat.isWorkoutVariable() && workoutType == null) {
        Section {
            Text(
                localized(
                    "⚠️ Start a workout using the Workout quick button or the Start workout " +
                        "button on your watch to update workout variables.",
                ),
            )
        }
    }
    if (textFormat.isLocationVariable() && !location.enabled) {
        Section {
            Text(localized("⚠️ Enable Location to update location variables."))
            Toggle(title = localized("Location"), isOn = location.enabled, onChange = {
                location.enabled = it
                model.reloadLocation()
            })
        }
    }
    if (textFormat.isWeatherVariable() && !location.enabled) {
        Section {
            Text(localized("⚠️ Enable Location to update weather variables."))
            Toggle(title = localized("Location"), isOn = location.enabled, onChange = {
                location.enabled = it
                model.reloadLocation()
            })
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
    if (textEffects.isNotEmpty()) {
        val textFormat = loadTextFormat(format = text.formatString)
        text.timers.forEach { timer ->
            val index = text.timers.indexOfFirst { it === timer }.takeIf { it >= 0 } ?: 0
            TimerWidgetView(
                name = localized("Timer ${index + 1}"),
                timer = timer,
                index = index,
                textEffects = textEffects,
                indented = true,
            )
        }
        text.stopwatches.forEach { stopwatch ->
            val index = text.stopwatches.indexOfFirst { it === stopwatch }.takeIf { it >= 0 } ?: 0
            StopwatchWidgetView(
                name = localized("Stopwatch ${index + 1}"),
                stopwatch = stopwatch,
                index = index,
                textEffects = textEffects,
                indented = true,
            )
        }
        text.checkboxes.forEach { checkbox ->
            val index = text.checkboxes.indexOfFirst { it === checkbox }.takeIf { it >= 0 } ?: 0
            CheckboxWidgetView(
                name = textFormat.getCheckboxText(index = index),
                checkbox = checkbox,
                index = index,
                textEffects = textEffects,
                indented = true,
            )
        }
        text.ratings.forEach { rating ->
            val index = text.ratings.indexOfFirst { it === rating }.takeIf { it >= 0 } ?: 0
            RatingWidgetView(
                name = localized("Rating ${index + 1}"),
                rating = rating,
                index = index,
                textEffects = textEffects,
                indented = true,
            )
        }
        text.lapTimes.forEach { lapTime ->
            val index = text.lapTimes.indexOfFirst { it === lapTime }.takeIf { it >= 0 } ?: 0
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

@Composable
fun TextWidgetSuggestionsView(
    widget: Boolean,
    text: String,
    onChange: (String) -> Unit,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(title = localized("Suggestions"), destination = {
        TextWidgetSuggestionsInnerView(
            widget = widget,
            text = text,
            onChange = onChange,
            onDismiss = {},
        )
    })
}

@Composable
private fun FontFamilyPickerView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    text: SettingsWidgetText,
) {
    var fontFamilies by remember { mutableStateOf(listOf<String>()) }

    fun selectFamily(family: String?) {
        text.fontFamily = family
        if (family != null) {
            text.fontStyle = fontStyles(fontFamily = family).firstOrNull() ?: ""
        }
        for (effect in model.getTextEffects(id = widget.id)) {
            effect.setFontFamily(family = text.fontFamily)
            effect.setFontStyle(style = text.fontStyle)
        }
        model.remoteSceneSettingsUpdated()
    }

    LaunchedEffect(Unit) {
        if (fontFamilies.isEmpty()) {
            fontFamilies = loadFontFamilies()
        }
    }
    Form(title = localized("Family")) {
        Section {
            FormRow(onClick = { selectFamily(null) }) {
                Text(localized("System"))
                Spacer(Modifier.weight(1f))
                if (text.fontFamily == null) {
                    SystemImage(name = "checkmark", fontSize = 17.sp, modifier = Modifier, tint = LocalTint.current)
                }
            }
            fontFamilies.forEach { family ->
                FormRow(onClick = { selectFamily(family) }) {
                    Text(family)
                    Spacer(Modifier.weight(1f))
                    if (text.fontFamily == family) {
                        SystemImage(
                            name = "checkmark",
                            fontSize = 17.sp,
                            modifier = Modifier,
                            tint = LocalTint.current,
                        )
                    }
                }
            }
        }
    }
}

private fun loadFontFamilies(): List<String> =
    emptyList()

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
    emptyList()

@Composable
private fun FontStylePickerView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    text: SettingsWidgetText,
    fontFamily: String,
) {
    Form(title = localized("Style")) {
        Section {
            fontStyles(fontFamily = fontFamily).forEach { style ->
                FormRow(onClick = {
                    text.fontStyle = style
                    for (effect in model.getTextEffects(id = widget.id)) {
                        effect.setFontStyle(style = text.fontStyle)
                    }
                    model.remoteSceneSettingsUpdated()
                }) {
                    Text(fontStyleName(family = fontFamily, fontName = style))
                    Spacer(Modifier.weight(1f))
                    if (text.fontStyle == style) {
                        SystemImage(
                            name = "checkmark",
                            fontSize = 17.sp,
                            modifier = Modifier,
                            tint = LocalTint.current,
                        )
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

    fun setLayout() {
        for (effect in model.getTextEffects(id = widget.id)) {
            effect.setLayout(
                alignment = text.horizontalAlignment,
                width = if (text.widthEnabled) text.width else null,
                cornerRadius = text.cornerRadius.toDouble(),
            )
        }
        model.remoteSceneSettingsUpdated()
    }

    fun submitWidth(value: String) {
        val width = value.toIntOrNull() ?: return
        text.width = width
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
        text.cornerRadius = cornerRadius
        setLayout()
    }

    Section {
        NavigationLink(destination = {
            TextSelectionView(model = model, widget = widget, text = text, onNavigate = onNavigate)
        }) {
            TextItemLocalizedView(name = "Text", value = text.formatString)
        }
    }
    TextFormatWarningsView(
        model = model,
        location = model.database.location,
        value = text.formatString,
        onChange = { text.formatString = it },
    )
    val textEffects = model.getTextEffects(id = widget.id)
    if (textEffects.isNotEmpty()) {
        if (text.timers.isNotEmpty()) {
            Section(header = localized("Timers")) {
                text.timers.forEach { timer ->
                    val index = text.timers.indexOfFirst { it === timer }.takeIf { it >= 0 } ?: 0
                    TimerWidgetView(
                        name = localized("Timer ${index + 1}"),
                        timer = timer,
                        index = index,
                        textEffects = textEffects,
                        indented = false,
                    )
                }
            }
        }
        if (text.stopwatches.isNotEmpty()) {
            Section(header = localized("Stopwatches")) {
                text.stopwatches.forEach { stopwatch ->
                    val index = text.stopwatches.indexOfFirst { it === stopwatch }.takeIf { it >= 0 } ?: 0
                    StopwatchWidgetView(
                        name = localized("Stopwatch ${index + 1}"),
                        stopwatch = stopwatch,
                        index = index,
                        textEffects = textEffects,
                        indented = false,
                    )
                }
            }
        }
        if (text.checkboxes.isNotEmpty()) {
            Section(header = localized("Checkboxes")) {
                val textFormat = loadTextFormat(format = text.formatString)
                text.checkboxes.forEach { checkbox ->
                    val index = text.checkboxes.indexOfFirst { it === checkbox }.takeIf { it >= 0 } ?: 0
                    CheckboxWidgetView(
                        name = textFormat.getCheckboxText(index = index),
                        checkbox = checkbox,
                        index = index,
                        textEffects = textEffects,
                        indented = false,
                    )
                }
            }
        }
        if (text.ratings.isNotEmpty()) {
            Section(header = localized("Ratings")) {
                text.ratings.forEach { rating ->
                    val index = text.ratings.indexOfFirst { it === rating }.takeIf { it >= 0 } ?: 0
                    RatingWidgetView(
                        name = localized("Rating ${index + 1}"),
                        rating = rating,
                        index = index,
                        textEffects = textEffects,
                        indented = false,
                    )
                }
            }
        }
        if (text.lapTimes.isNotEmpty()) {
            Section(header = localized("Lap times")) {
                text.lapTimes.forEach { lapTime ->
                    val index = text.lapTimes.indexOfFirst { it === lapTime }.takeIf { it >= 0 } ?: 0
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
    }
    Section(header = localized("Colors")) {
        RgbColorPickerView(
            title = "Background",
            color = text.backgroundColorColor,
            onColorChanged = { text.backgroundColorColor = it },
            opacity = true,
            onChange = { color ->
                text.backgroundColor = color
                for (effect in model.getTextEffects(id = widget.id)) {
                    effect.setBackgroundColor(color = color)
                }
                model.remoteSceneSettingsUpdated()
            },
        )
        RgbColorPickerView(
            title = "Foreground",
            color = text.foregroundColorColor,
            onColorChanged = { text.foregroundColorColor = it },
            opacity = true,
            onChange = { color ->
                text.foregroundColor = color
                for (effect in model.getTextEffects(id = widget.id)) {
                    effect.setForegroundColor(color = color)
                }
                model.remoteSceneSettingsUpdated()
            },
        )
    }
    Section(header = localized("Font")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(localized("Size"))
            FormSlider(
                value = text.fontSizeFloat,
                onValueChange = { value ->
                    text.fontSizeFloat = value
                    text.fontSize = value.toInt()
                    for (effect in model.getTextEffects(id = widget.id)) {
                        effect.setFontSize(size = value)
                    }
                    model.remoteSceneSettingsUpdated()
                },
                modifier = Modifier.weight(1f),
                valueRange = 10f..200f,
            )
            Text(text.fontSizeFloat.toInt().toString(), modifier = Modifier.width(35.dp))
        }
        NavigationLink(destination = {
            FontFamilyPickerView(model = model, widget = widget, text = text)
        }) {
            Text(localized("Family"))
            Spacer(Modifier.weight(1f))
            GrayTextView(text = text.fontFamilyString())
        }
        val fontFamily = text.fontFamily
        if (fontFamily != null) {
            NavigationLink(
                enabled = fontStyles(fontFamily = fontFamily).size != 1,
                destination = {
                    FontStylePickerView(
                        model = model,
                        widget = widget,
                        text = text,
                        fontFamily = fontFamily,
                    )
                },
            ) {
                Text(localized("Style"))
                Spacer(Modifier.weight(1f))
                GrayTextView(text = text.fontStyleString())
            }
        } else {
            Picker(
                title = localized("Design"),
                selection = text.fontDesign,
                options = SettingsFontDesign.entries.toList(),
                text = { it.toString() },
                onChange = { design ->
                    text.fontDesign = design
                    for (effect in model.getTextEffects(id = widget.id)) {
                        effect.setFontDesign(design = design)
                    }
                    model.remoteSceneSettingsUpdated()
                },
            )
            Picker(
                title = localized("Weight"),
                selection = text.fontWeight,
                options = SettingsFontWeight.entries.toList(),
                text = { it.toString() },
                onChange = { weight ->
                    text.fontWeight = weight
                    for (effect in model.getTextEffects(id = widget.id)) {
                        effect.setFontWeight(weight = weight)
                    }
                    model.remoteSceneSettingsUpdated()
                },
            )
            Toggle(
                title = localized("Monospaced digits"),
                isOn = text.fontMonospacedDigits,
                onChange = { enabled ->
                    text.fontMonospacedDigits = enabled
                    for (effect in model.getTextEffects(id = widget.id)) {
                        effect.setFontMonospacedDigits(enabled = enabled)
                    }
                    model.remoteSceneSettingsUpdated()
                },
            )
        }
    }
    Section(header = localized("Layout")) {
        Picker(
            title = localized("Alignment"),
            selection = text.horizontalAlignment,
            options = SettingsHorizontalAlignment.entries.toList(),
            text = { it.toString() },
            onChange = {
                text.horizontalAlignment = it
                setLayout()
            },
        )
        NavigationLink(destination = {
            TextEditView(
                title = localized("Minimum width"),
                value = text.width.toString(),
                onChange = { changeWidth(it) },
                onSubmit = { submitWidth(it) },
                keyboardType = KeyboardType.Number,
            )
        }) {
            Text(localized("Minimum width"))
            Spacer(Modifier.weight(1f))
            IosSwitch(
                checked = text.widthEnabled,
                onCheckedChange = {
                    text.widthEnabled = it
                    setLayout()
                },
            )
            GrayTextView(text = text.width.toString())
        }
        TextEditNavigationView(
            title = localized("Corner radius"),
            value = text.cornerRadius.toString(),
            onChange = { changeCornerRadius(it) },
            onSubmit = { submitCornerRadius(it) },
            keyboardType = KeyboardType.Number,
        )
    }
    Section(
        header = localized("Delay"),
        footer = localized("To show the widget in sync with high latency cameras."),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FormSlider(
                value = text.delay.toFloat(),
                onValueChange = { text.delay = it.toDouble() },
                modifier = Modifier.weight(1f),
                valueRange = 0f..10f,
                onValueChangeFinished = { model.resetSelectedScene(changeScene = false) },
            )
            Text(text.delay.toString(), modifier = Modifier.width(35.dp))
        }
    }
}
