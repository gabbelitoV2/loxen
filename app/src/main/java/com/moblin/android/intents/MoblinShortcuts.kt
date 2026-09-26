package com.moblin.android.intents

import com.moblin.android.platform.appintents.AppShortcut
import com.moblin.android.platform.appintents.AppShortcutPhraseToken
import com.moblin.android.platform.appintents.AppShortcutsProvider
import com.moblin.android.platform.appintents.ShortcutTileColor

object MoblinShortcuts : AppShortcutsProvider {
    override val shortcutTileColor = ShortcutTileColor.navy

    override val appShortcuts: List<AppShortcut> = listOf(
        AppShortcut(
            intent = MuteIntent(),
            phrases = listOf(
                "${AppShortcutPhraseToken.applicationName}, mute"
            ),
            shortTitle = "Mute",
            systemImageName = "microphone.slash"
        ),
        AppShortcut(
            intent = UnmuteIntent(),
            phrases = listOf(
                "${AppShortcutPhraseToken.applicationName}, unmute"
            ),
            shortTitle = "Unmute",
            systemImageName = "microphone"
        ),
        AppShortcut(
            intent = SnapshotIntent(),
            phrases = listOf(
                "${AppShortcutPhraseToken.applicationName}, take snapshot"
            ),
            shortTitle = "Take snapshot",
            systemImageName = "microphone"
        )
    )
}
