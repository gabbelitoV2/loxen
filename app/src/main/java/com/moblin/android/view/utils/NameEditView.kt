package com.moblin.android.view.utils

import androidx.compose.runtime.Composable
import com.moblin.android.localized
import com.moblin.android.various.utils.Named
import com.moblin.android.LocalOnNavigate
import com.moblin.android.platform.swiftui.NavigationLink

private fun onChange(value: String, name: String, existingNames: List<Named>): String? {
    if (value.isEmpty()) {
        return localized("Empty names are not allowed.")
    } else if (existingNames.any { it.name == value } && value != name) {
        return localized("The name '$value' is already in use.")
    } else {
        return null
    }
}

@Composable
fun NameEditView(
    name: String,
    onNameChange: (String) -> Unit,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
    existingNames: List<Named> = emptyList(),
) {
    NavigationLink(
        destination = {
            TextEditView(
                title = localized("Name"),
                value = name,
                capitalize = true,
                onChange = { onChange(it, name, existingNames) },
                onSubmit = { onNameChange(it) },
            )
        },
    ) {
        TextItemLocalizedView(name = "Name", value = name)
    }
}
