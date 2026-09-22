package com.moblin.android.view.settings.scenes.widgets.widget.bingocard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.RgbColor
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetBingoCard
import com.moblin.android.view.utils.MultiLineTextFieldDoneButtonView
import com.moblin.android.view.utils.MultiLineTextFieldView
import com.moblin.android.view.utils.RgbColorPickerView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.LocalModel

@Composable
fun BingCardWidgetSquaresView(value: String, onValueChange: (String) -> Unit) {
    var editingText by remember { mutableStateOf(false) }
    Text(text = "Squares", style = MaterialTheme.typography.titleSmall)
    MultiLineTextFieldView(
        value = value,
        onValueChange = onValueChange,
        placeholder = localized("My text"),
    )
    MultiLineTextFieldDoneButtonView(
        editingText = editingText,
        onEditingTextChange = { editingText = it },
    )
}

@Composable
fun BingoCardMarksView(bingoCard: SettingsWidgetBingoCard, updateEffect: () -> Unit) {
    bingoCard.squares.forEachIndexed { index, square ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = square.text)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = {
                    val squares = bingoCard.squares.toMutableList()
                    squares[index] = squares[index].copy(checked = !squares[index].checked)
                    bingoCard.squares = squares
                    updateEffect()
                },
            ) {
                Icon(
                    imageVector = if (square.checked) {
                        Icons.Filled.CheckBox
                    } else {
                        Icons.Filled.CheckBoxOutlineBlank
                    },
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
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
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        for (row in 0 until squaresCountSide) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.weight(1f))
                for (column in 0 until squaresCountSide) {
                    val index = row * squaresCountSide + column
                    if (index < bingoCard.squares.count()) {
                        IconButton(
                            onClick = {
                                val squares = bingoCard.squares.toMutableList()
                                squares[index] = squares[index].copy(checked = !squares[index].checked)
                                bingoCard.squares = squares
                                updateEffect()
                            },
                        ) {
                            Icon(
                                imageVector = if (bingoCard.squares[index].checked) {
                                    Icons.Filled.CheckBox
                                } else {
                                    Icons.Filled.CheckBoxOutlineBlank
                                },
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Filled.CheckBoxOutlineBlank,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = Color.Gray,
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
        TODO("model.getBingoCardEffect has no Android counterpart")
    }

    BingCardWidgetSquaresView(
        value = bingoCard.squaresText,
        onValueChange = { bingoCard.squaresText = it },
    )
    LaunchedEffect(bingoCard.squaresText) {
        bingoCard.squaresTextChanged()
        updateEffect()
    }
    Text(text = "Marks", style = MaterialTheme.typography.titleSmall)
    BingoCardMarksView(bingoCard = bingoCard, updateEffect = updateEffect)
    TextButtonView("Reset", action = {
        bingoCard.uncheckAll()
        updateEffect()
    })
    Text(text = "Colors", style = MaterialTheme.typography.titleSmall)
    RgbColorPickerView(
        title = "Background",
        color = bingoCard.backgroundColorColor,
        opacity = true,
        onColorChanged = {},
        onChange = { color: RgbColor ->
            bingoCard.backgroundColor = color
            updateEffect()
        },
    )
    RgbColorPickerView(
        title = "Foreground",
        color = bingoCard.foregroundColorColor,
        onColorChanged = {},
        onChange = { color: RgbColor ->
            bingoCard.foregroundColor = color
            updateEffect()
        },
    )
}
