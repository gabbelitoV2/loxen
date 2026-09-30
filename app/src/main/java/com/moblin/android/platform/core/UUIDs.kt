package com.moblin.android.platform.core

import java.io.File
import java.util.UUID

val UUID.uuidString: String
    get() = toString().uppercase()

fun uuidFile(directory: File, id: UUID, suffix: String = ""): File {
    val name = id.uuidString + suffix
    val file = File(directory, name)
    val written = id.toString() + suffix
    val names = directory.list() ?: return file
    if (name !in names && written in names) {
        File(directory, written).renameTo(file)
    }
    return file
}
