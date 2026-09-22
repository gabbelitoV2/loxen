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

class SettingsMacrosSuite {
    @Test
    fun numbersAreComparedNumerically() {
        assertFalse(SettingsMacrosActionIfComparison.greaterThan.evaluate(value = "9", otherValue = "10"))
        assertTrue(SettingsMacrosActionIfComparison.lessThan.evaluate(value = "9", otherValue = "10"))
        assertTrue(SettingsMacrosActionIfComparison.equal.evaluate(value = "10.0", otherValue = "10"))
        assertTrue(SettingsMacrosActionIfComparison.greaterEqual.evaluate(value = "10", otherValue = "10"))
        assertTrue(SettingsMacrosActionIfComparison.notEqual.evaluate(value = "-1", otherValue = "1"))
    }

    @Test
    fun unitsAfterTheNumberAreIgnored() {
        assertTrue(SettingsMacrosActionIfComparison.greaterThan.evaluate(value = "35 km/h", otherValue = "30"))
        assertTrue(SettingsMacrosActionIfComparison.lessEqual.evaluate(value = "-5 m", otherValue = "0"))
        assertTrue(SettingsMacrosActionIfComparison.greaterThan.evaluate(value = " 45%", otherValue = "10"))
    }

    @Test
    fun textIsComparedCaseInsensitively() {
        assertTrue(SettingsMacrosActionIfComparison.equal.evaluate(value = "Yes", otherValue = "yes"))
        assertTrue(SettingsMacrosActionIfComparison.lessThan.evaluate(value = "apple", otherValue = "Banana"))
        assertTrue(SettingsMacrosActionIfComparison.contains.evaluate(value = "Heavy rain", otherValue = "RAIN"))
        assertFalse(
            SettingsMacrosActionIfComparison.contains
                .evaluate(value = "Sunny", otherValue = "rain")
        )
    }

    @Test
    fun ifActionSurvivesEncodeAndDecode() {
        val action = SettingsMacrosAction()
        action.function = SettingsMacrosActionFunction.ifCondition
        action.ifValue = "{speed}"
        action.ifComparison = SettingsMacrosActionIfComparison.greaterEqual
        action.ifOtherValue = "30"
        action.ifRunCount = 3
        val decoded = Json.decodeFromString<SettingsMacrosAction>(Json.encodeToString(action))
        assertEquals(SettingsMacrosActionFunction.ifCondition, decoded.function)
        assertEquals("{speed}", decoded.ifValue)
        assertEquals(SettingsMacrosActionIfComparison.greaterEqual, decoded.ifComparison)
        assertEquals("30", decoded.ifOtherValue)
        assertEquals(3, decoded.ifRunCount)
    }

    @Test
    fun ifActionDefaultsWhenMissingFromSettings() {
        val action = Json.decodeFromString<SettingsMacrosAction>("{}")
        assertEquals("", action.ifValue)
        assertEquals(SettingsMacrosActionIfComparison.equal, action.ifComparison)
        assertEquals("", action.ifOtherValue)
        assertEquals(1, action.ifRunCount)
    }

    @Test
    fun waitForEventMatchesOnlyItsEvent() {
        val action = makeWaitForEventAction(SettingsMacrosEvent.twitchFollow)
        assertTrue(action.matches(event = MacroEvent(event = SettingsMacrosEvent.twitchFollow)))
        assertFalse(action.matches(event = MacroEvent(event = SettingsMacrosEvent.twitchSubscription)))
    }

    @Test
    fun waitForEventWithMinimumAmountMatchesAtOrAboveIt() {
        val action = makeWaitForEventAction(SettingsMacrosEvent.twitchCheer)
        action.eventMinimumAmount = 100
        assertFalse(action.matches(event = MacroEvent(event = SettingsMacrosEvent.twitchCheer, amount = 99)))
        assertTrue(action.matches(event = MacroEvent(event = SettingsMacrosEvent.twitchCheer, amount = 100)))
        assertTrue(action.matches(event = MacroEvent(event = SettingsMacrosEvent.twitchCheer, amount = 500)))
    }

    @Test
    fun waitForEventTextIgnoresCaseAndSurroundingWhitespace() {
        val action = makeWaitForEventAction(SettingsMacrosEvent.twitchReward)
        action.eventText = " Hydrate "
        assertTrue(action.matches(event = MacroEvent(event = SettingsMacrosEvent.twitchReward, text = "hydrate")))
        assertFalse(action.matches(event = MacroEvent(event = SettingsMacrosEvent.twitchReward, text = "Stretch")))
    }

    @Test
    fun waitForEventWithEmptyTextMatchesAnyText() {
        val action = makeWaitForEventAction(SettingsMacrosEvent.twitchReward)
        assertTrue(action.matches(event = MacroEvent(event = SettingsMacrosEvent.twitchReward, text = "Hydrate")))
        assertTrue(action.matches(event = MacroEvent(event = SettingsMacrosEvent.twitchReward)))
    }

    @Test
    fun waitForSceneSwitchedMatchesSelectedOrAnyScene() {
        val sceneId = UUID.randomUUID()
        val action = makeWaitForEventAction(SettingsMacrosEvent.switchScene)
        assertTrue(action.matches(event = MacroEvent(event = SettingsMacrosEvent.switchScene, sceneId = sceneId)))
        action.eventSceneId = sceneId
        assertTrue(action.matches(event = MacroEvent(event = SettingsMacrosEvent.switchScene, sceneId = sceneId)))
        assertFalse(action.matches(event = MacroEvent(event = SettingsMacrosEvent.switchScene, sceneId = UUID.randomUUID())))
    }

    @Test
    fun waitForEventActionSurvivesEncodeAndDecode() {
        val sceneId = UUID.randomUUID()
        val action = makeWaitForEventAction(SettingsMacrosEvent.kickKicks)
        action.eventMinimumAmount = 42
        action.eventText = "boom"
        action.eventSceneId = sceneId
        val decoded = Json.decodeFromString<SettingsMacrosAction>(Json.encodeToString(action))
        assertEquals(SettingsMacrosActionFunction.waitForEvent, decoded.function)
        assertEquals(SettingsMacrosEvent.kickKicks, decoded.event)
        assertEquals(42, decoded.eventMinimumAmount)
        assertEquals("boom", decoded.eventText)
        assertEquals(sceneId, decoded.eventSceneId)
    }

    @Test
    fun waitForEventActionDefaultsWhenMissingFromSettings() {
        val action = Json.decodeFromString<SettingsMacrosAction>("{}")
        assertEquals(SettingsMacrosEvent.twitchFollow, action.event)
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
        action.function = SettingsMacrosActionFunction.waitForEvent
        action.event = event
        return action
    }
}
