package com.moblin.android.platform.core

import com.moblin.android.various.settings.Database

fun upperCaseMicIds(database: Database) {
    for (mic in database.mics.mics.value) {
        mic.inputUid = upperCaseLeadingUuid(mic.inputUid)
    }
    database.mics.defaultMic = upperCaseLeadingUuid(database.mics.defaultMic)
    for (scene in database.scenes) {
        scene.micId = upperCaseLeadingUuid(scene.micId)
    }
    database.talkback.micId.value = upperCaseLeadingUuid(database.talkback.micId.value)
}
