# Tier: logic

This file is pure logic without Apple platform frameworks. The translation is expected to compile and behave identically. Produce a complete, idiomatic Kotlin file. The unsupported list should normally be empty.

A Swift `T: Identifiable` constraint with `ID == UUID` is `T : Identifiable<UUID>` (`com.moblin.android.various.utils.Identifiable`); the Settings classes SettingsScene, SettingsSceneWidget, SettingsWidget, SettingsVideoEffect and SettingsWidgetScoreboardPlayer declare `: Identifiable<UUID>` with `override var id`, and `Named` classes keep `: Named`.
