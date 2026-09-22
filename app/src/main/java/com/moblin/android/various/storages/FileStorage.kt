package com.moblin.android.various.storages

import android.util.Log
import com.moblin.android.various.utils.createAndGetDirectory
import com.moblin.android.various.utils.ids
import java.io.File
import java.io.IOException
import java.util.UUID

class FileStorage(directory: String) {
    private var directory: File = createAndGetDirectory(directory)

    fun makePath(id: UUID): File {
        return File(directory, id.toString())
    }

    fun ids(): List<UUID> {
        return ids(directory.path)
    }

    fun add(id: UUID, url: File) {
        try {
            val path = makePath(id)
            path.delete()
            if (!url.renameTo(path)) {
                url.copyTo(path, overwrite = true)
                url.delete()
            }
        } catch (e: Exception) {
            Log.i("FileStorage", "file-storage: $directory: Move failed with error $e")
        }
    }

    fun remove(id: UUID) {
        try {
            if (!makePath(id).delete()) {
                throw IOException("Remove failed")
            }
        } catch (e: Exception) {
            Log.i("FileStorage", "file-storage: $directory: Remove failed with error $e")
        }
    }

    fun write(id: UUID, data: ByteArray) {
        try {
            makePath(id).writeBytes(data)
        } catch (e: Exception) {
            Log.i("FileStorage", "file-storage: $directory: Write failed with error $e")
        }
    }

    fun write(id: UUID, url: File) {
        try {
            write(id, url.readBytes())
        } catch (e: Exception) {
            Log.i("FileStorage", "file-storage: $directory: Write URL failed with error $e")
        }
    }

    fun read(id: UUID): ByteArray? {
        return try {
            makePath(id).readBytes()
        } catch (e: Exception) {
            Log.i("FileStorage", "image-storage: Read failed with error $e")
            null
        }
    }

    fun tryRead(id: UUID): ByteArray? {
        return runCatching { makePath(id).readBytes() }.getOrNull()
    }
}
