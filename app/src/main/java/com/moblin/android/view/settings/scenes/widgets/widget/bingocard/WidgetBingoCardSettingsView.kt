package com.moblin.android.view.settings.scenes.widgets.widget.bingocard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.common.various.RgbColor
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetBingoCard
import com.moblin.android.view.utils.MultiLineTextFieldDoneButtonView
import com.moblin.android.view.utils.MultiLineTextFieldView
import com.moblin.android.view.utils.RgbColorPickerView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.various.model.getBingoCardEffect

@Composable
fun BingCardWidgetSquaresView(value: String, onValueChange: (String) -> Unit) {
    var editingText by remember { mutableStateOf(false) }
    Section(
        header = localized("Squares"),
        footerContent = {
            MultiLineTextFieldDoneButtonView(
                editingText = editingText,
                onEditingTextChange = { editingText = it },
            )
        },
    ) {
        MultiLineTextFieldView(
            value = value,
            onValueChange = onValueChange,
            placeholder = localized("My text"),
        )
    }
}

@Composable
fun BingoCardMarksView(bingoCard: SettingsWidgetBingoCard, updateEffect: () -> Unit) {
    val palette = formPalette()
    bingoCard.squares.forEachIndexed { index, square ->
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        FormRow {
            Text(text = square.text)
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .alpha(if (pressed) 0.2f else 1f)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                    ) {
                        val squares = bingoCard.squares.toMutableList()
                        squares[index] = squares[index].copy(checked = !squares[index].checked)
                        bingoCard.squares = squares
                        updateEffect()
                    },
            ) {
                SystemImage(
                    name = if (square.checked) "squareshape.split.2x2" else "square",
                    fontSize = 28.sp,
                    tint = palette.label,
                )
            }
        }
    }
}

@Composable
fun WidgetBingoCardQuickButtonControlsView(
    bingoCard: SettingsWidgetBingoCard,
    updateEffect: () -> Unit,
) {
    val squaresCountSide = bingoCard.size()
    val palette = formPalette()
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        for (row in 0 until squaresCountSide) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.weight(1f))
                for (column in 0 until squaresCountSide) {
                    val index = row * squaresCountSide + column
                    if (index < bingoCard.squares.size) {
                        val interactionSource = remember { MutableInteractionSource() }
                        val pressed by interactionSource.collectIsPressedAsState()
                        Box(
                            modifier = Modifier
                                .alpha(if (pressed) 0.2f else 1f)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                ) {
                                    val squares = bingoCard.squares.toMutableList()
                                    squares[index] =
                                        squares[index].copy(checked = !squares[index].checked)
                                    bingoCard.squares = squares
                                    updateEffect()
                                },
                        ) {
                            SystemImage(
                                name = if (bingoCard.squares[index].checked) {
                                    "squareshape.split.2x2"
                                } else {
                                    "square"
                                },
                                fontSize = 28.sp,
                                tint = palette.label,
                            )
                        }
                    } else {
                        SystemImage(
                            name = "square",
                            fontSize = 28.sp,
                            tint = palette.gray,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetBingoCardSettingsView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    bingoCard: SettingsWidgetBingoCard,
) {
    val updateEffect: () -> Unit = {
        model.getBingoCardEffect(widget.id)?.setSettings(bingoCard)
    }

    BingCardWidgetSquaresView(
        value = bingoCard.squaresText,
        onValueChange = {
            bingoCard.squaresText = it
            bingoCard.squaresTextChanged()
            updateEffect()
        },
    )
    Section(header = localized("Marks")) {
        BingoCardMarksView(bingoCard = bingoCard, updateEffect = updateEffect)
        TextButtonView(localized("Reset"), action = {
            bingoCard.uncheckAll()
            updateEffect()
        })
    }
    Section(header = localized("Colors")) {
        RgbColorPickerView(
            title = localized("Background"),
            color = bingoCard.backgroundColorColor,
            opacity = true,
            onColorChanged = { color: Color ->
                bingoCard.backgroundColorColor = color
            },
            onChange = { color: RgbColor ->
                bingoCard.backgroundColor = color
                updateEffect()
            },
        )
        RgbColorPickerView(
            title = localized("Foreground"),
            color = bingoCard.foregroundColorColor,
            onColorChanged = { color: Color ->
                bingoCard.foregroundColorColor = color
            },
            onChange = { color: RgbColor ->
                bingoCard.foregroundColor = color
                updateEffect()
            },
        )
    }
}
