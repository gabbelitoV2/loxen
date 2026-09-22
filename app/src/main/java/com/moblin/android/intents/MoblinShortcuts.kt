package com.moblin.android.intents

data class ShortcutTileColor(val rawValue: String) {
    companion object {
        val navy: ShortcutTileColor = ShortcutTileColor("navy")
    }
}

data class AppShortcut(
    val intent: Any,
    val phrases: List<String>,
    val shortTitle: String,
    val systemImageName: String,
)

object MoblinShortcuts {
    val shortcutTileColor: ShortcutTileColor = ShortcutTileColor.navy

    val appShortcuts: List<AppShortcut> = listOf(
        AppShortcut(
            intent = MuteIntent::class,
            phrases = listOf(
                "\${applicationName}, mute",
            ),
            shortTitle = "Mute",
            systemImageName = "microphone.slash",
        ),
        AppShortcut(
            intent = UnmuteIntent::class,
            phrases = listOf(
                "\${applicationName}, unmute",
            ),
            shortTitle = "Unmute",
            systemImageName = "microphone",
        ),
        AppShortcut(
            intent = SnapshotIntent::class,
            phrases = listOf(
                "\${applicationName}, take snapshot",
            ),
            shortTitle = "Take snapshot",
            systemImageName = "microphone",
        ),
    )

    fun updateAppShortcutParameters() {
        TODO("no Android counterpart for AppIntents")
    }
}
