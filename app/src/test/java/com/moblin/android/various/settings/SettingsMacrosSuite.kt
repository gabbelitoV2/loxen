package com.moblin.android.various.settings

import java.util.UUID
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsMacrosSuite {
    @Test
    fun numbersAreComparedNumerically() {
        assertFalse(SettingsMacrosActionIfComparison.GREATER_THAN.evaluate(value = "9", otherValue = "10"))
        assertTrue(SettingsMacrosActionIfComparison.LESS_THAN.evaluate(value = "9", otherValue = "10"))
        assertTrue(SettingsMacrosActionIfComparison.EQUAL.evaluate(value = "10.0", otherValue = "10"))
        assertTrue(SettingsMacrosActionIfComparison.GREATER_EQUAL.evaluate(value = "10", otherValue = "10"))
        assertTrue(SettingsMacrosActionIfComparison.NOT_EQUAL.evaluate(value = "-1", otherValue = "1"))
    }

    @Test
    fun unitsAfterTheNumberAreIgnored() {
        assertTrue(SettingsMacrosActionIfComparison.GREATER_THAN.evaluate(value = "35 km/h", otherValue = "30"))
        assertTrue(SettingsMacrosActionIfComparison.LESS_EQUAL.evaluate(value = "-5 m", otherValue = "0"))
        assertTrue(SettingsMacrosActionIfComparison.GREATER_THAN.evaluate(value = " 45%", otherValue = "10"))
    }

    @Test
    fun textIsComparedCaseInsensitively() {
        assertTrue(SettingsMacrosActionIfComparison.EQUAL.evaluate(value = "Yes", otherValue = "yes"))
        assertTrue(SettingsMacrosActionIfComparison.LESS_THAN.evaluate(value = "apple", otherValue = "Banana"))
        assertTrue(SettingsMacrosActionIfComparison.CONTAINS.evaluate(value = "Heavy rain", otherValue = "RAIN"))
        assertFalse(
            SettingsMacrosActionIfComparison.CONTAINS
                .evaluate(value = "Sunny", otherValue = "rain")
        )
    }

    @Test
    fun ifActionSurvivesEncodeAndDecode() {
        val action = SettingsMacrosAction()
        action.function = SettingsMacrosActionFunction.IF_CONDITION
        action.ifValue = "{speed}"
        action.ifComparison = SettingsMacrosActionIfComparison.GREATER_EQUAL
        action.ifOtherValue = "30"
        action.ifRunCount = 3
        val decoded = Json.decodeFromString<SettingsMacrosAction>(Json.encodeToString(action))
        assertEquals(SettingsMacrosActionFunction.IF_CONDITION, decoded.function)
        assertEquals("{speed}", decoded.ifValue)
        assertEquals(SettingsMacrosActionIfComparison.GREATER_EQUAL, decoded.ifComparison)
        assertEquals("30", decoded.ifOtherValue)
        assertEquals(3, decoded.ifRunCount)
    }

    @Test
    fun ifActionDefaultsWhenMissingFromSettings() {
        val action = Json.decodeFromString<SettingsMacrosAction>("{}")
        assertEquals("", action.ifValue)
        assertEquals(SettingsMacrosActionIfComparison.EQUAL, action.ifComparison)
        assertEquals("", action.ifOtherValue)
        assertEquals(1, action.ifRunCount)
    }

    @Test
    fun waitForEventMatchesOnlyItsEvent() {
        val action = makeWaitForEventAction(SettingsMacrosEvent.TWITCH_FOLLOW)
        assertTrue(action.matches(event = MacroEvent(event = SettingsMacrosEvent.TWITCH_FOLLOW)))
        assertFalse(action.matches(event = MacroEvent(event = SettingsMacrosEvent.TWITCH_SUBSCRIPTION)))
    }

    @Test
    fun waitForEventWithMinimumAmountMatchesAtOrAboveIt() {
        val action = makeWaitForEventAction(SettingsMacrosEvent.TWITCH_CHEER)
        action.eventMinimumAmount = 100
        assertFalse(action.matches(event = MacroEvent(event = SettingsMacrosEvent.TWITCH_CHEER, amount = 99)))
        assertTrue(action.matches(event = MacroEvent(event = SettingsMacrosEvent.TWITCH_CHEER, amount = 100)))
        assertTrue(action.matches(event = MacroEvent(event = SettingsMacrosEvent.TWITCH_CHEER, amount = 500)))
    }

    @Test
    fun waitForEventTextIgnoresCaseAndSurroundingWhitespace() {
        val action = makeWaitForEventAction(SettingsMacrosEvent.TWITCH_REWARD)
        action.eventText = " Hydrate "
        assertTrue(action.matches(event = MacroEvent(event = SettingsMacrosEvent.TWITCH_REWARD, text = "hydrate")))
        assertFalse(action.matches(event = MacroEvent(event = SettingsMacrosEvent.TWITCH_REWARD, text = "Stretch")))
    }

    @Test
    fun waitForEventWithEmptyTextMatchesAnyText() {
        val action = makeWaitForEventAction(SettingsMacrosEvent.TWITCH_REWARD)
        assertTrue(action.matches(event = MacroEvent(event = SettingsMacrosEvent.TWITCH_REWARD, text = "Hydrate")))
        assertTrue(action.matches(event = MacroEvent(event = SettingsMacrosEvent.TWITCH_REWARD)))
    }

    @Test
    fun waitForSceneSwitchedMatchesSelectedOrAnyScene() {
        val sceneId = UUID.randomUUID()
        val action = makeWaitForEventAction(SettingsMacrosEvent.SWITCH_SCENE)
        assertTrue(action.matches(event = MacroEvent(event = SettingsMacrosEvent.SWITCH_SCENE, sceneId = sceneId)))
        action.eventSceneId = sceneId
        assertTrue(action.matches(event = MacroEvent(event = SettingsMacrosEvent.SWITCH_SCENE, sceneId = sceneId)))
        assertFalse(action.matches(event = MacroEvent(event = SettingsMacrosEvent.SWITCH_SCENE, sceneId = UUID.randomUUID())))
    }

    @Test
    fun waitForEventActionSurvivesEncodeAndDecode() {
        val sceneId = UUID.randomUUID()
        val action = makeWaitForEventAction(SettingsMacrosEvent.KICK_KICKS)
        action.eventMinimumAmount = 42
        action.eventText = "boom"
        action.eventSceneId = sceneId
        val decoded = Json.decodeFromString<SettingsMacrosAction>(Json.encodeToString(action))
        assertEquals(SettingsMacrosActionFunction.WAIT_FOR_EVENT, decoded.function)
        assertEquals(SettingsMacrosEvent.KICK_KICKS, decoded.event)
        assertEquals(42, decoded.eventMinimumAmount)
        assertEquals("boom", decoded.eventText)
        assertEquals(sceneId, decoded.eventSceneId)
    }

    @Test
    fun waitForEventActionDefaultsWhenMissingFromSettings() {
        val action = Json.decodeFromString<SettingsMacrosAction>("{}")
        assertEquals(SettingsMacrosEvent.TWITCH_FOLLOW, action.event)
        assertEquals(0, action.eventMinimumAmount)
        assertEquals("", action.eventText)
        assertNull(action.eventSceneId)
    }

    @Test
    fun runAtAppStartDefaultsToOffWhenMissingFromSettings() {
        val macro = Json.decodeFromString<SettingsMacrosMacro>("{}")
        assertFalse(macro.runAtAppStart)
    }

    private fun makeWaitForEventAction(event: SettingsMacrosEvent): SettingsMacrosAction {
        val action = SettingsMacrosAction()
        action.function = SettingsMacrosActionFunction.WAIT_FOR_EVENT
        action.event = event
        return action
    }
}
