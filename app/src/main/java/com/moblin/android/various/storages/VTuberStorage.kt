package com.moblin.android.various.storages

import java.io.File
import java.util.UUID

const val vTuberStorageDirectory = "VTuber"

class VTuberStorage {
    private val storage = FileStorage(vTuberStorageDirectory)

    fun makePath(id: UUID): File = storage.makePath(id)

    fun ids(): List<UUID> = storage.ids()

    fun add(id: UUID, url: File) = storage.add(id, url)

    fun path(id: UUID): File = storage.path(id)

    fun remove(id: UUID) = storage.remove(id)

    fun write(id: UUID, data: ByteArray) = storage.write(id, data)

    fun write(id: UUID, url: File) = storage.write(id, url)

    fun read(id: UUID): ByteArray? = storage.read(id)

    fun tryRead(id: UUID): ByteArray? = storage.tryRead(id)
}
