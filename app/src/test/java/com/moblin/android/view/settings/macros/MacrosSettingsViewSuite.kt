package com.moblin.android.view.settings.macros

import com.moblin.android.various.settings.SettingsMacrosAction
import com.moblin.android.various.settings.SettingsMacrosActionFunction
import kotlin.test.assertEquals
import org.junit.Test

class MacrosSettingsViewSuite {
    @Test
    fun actionsRunByIfShareItsLevel() {
        val actions = listOf(makeAction(), makeIfAction(2), makeAction(), makeAction(), makeAction())
        val bars = macroActionIfBars(actions)
        assertEquals(
            listOf(emptyList<Int>(), listOf(0), listOf(0), listOf(0), emptyList<Int>()),
            bars.map { bar -> bar.map { it.level } },
        )
        assertEquals(
            listOf(emptyList<Boolean>(), listOf(true), listOf(false), listOf(false), emptyList<Boolean>()),
            bars.map { bar -> bar.map { it.isFirst } },
        )
        assertEquals(
            listOf(emptyList<Boolean>(), listOf(false), listOf(false), listOf(true), emptyList<Boolean>()),
            bars.map { bar -> bar.map { it.isLast } },
        )
    }

    @Test
    fun nestedIfsAreOneLevelApart() {
        val actions = listOf(makeIfAction(3), makeIfAction(1), makeAction(), makeAction())
        val bars = macroActionIfBars(actions)
        assertEquals(
            listOf(listOf(0), listOf(0, 1), listOf(0, 1), listOf(0)),
            bars.map { bar -> bar.map { it.level } },
        )
        assertEquals(
            listOf(listOf(false), listOf(false, false), listOf(false, true), listOf(true)),
            bars.map { bar -> bar.map { it.isLast } },
        )
    }

    @Test
    fun ifsRunningActionsOutsideTheOuterIfKeepTheirOwnLevel() {
        val actions = listOf(makeIfAction(1), makeIfAction(2), makeAction(), makeAction())
        val bars = macroActionIfBars(actions)
        assertEquals(
            listOf(listOf(0), listOf(0, 1), listOf(1), listOf(1)),
            bars.map { bar -> bar.map { it.level } },
        )
    }

    @Test
    fun ifsAfterEachOtherAreSeparateBlocksOnTheSameLevel() {
        val actions = listOf(makeIfAction(1), makeAction(), makeIfAction(1), makeAction())
        val bars = macroActionIfBars(actions)
        assertEquals(
            listOf(listOf(0), listOf(0), listOf(0), listOf(0)),
            bars.map { bar -> bar.map { it.level } },
        )
        assertEquals(
            listOf(listOf(true), listOf(false), listOf(true), listOf(false)),
            bars.map { bar -> bar.map { it.isFirst } },
        )
        assertEquals(
            listOf(listOf(false), listOf(true), listOf(false), listOf(true)),
            bars.map { bar -> bar.map { it.isLast } },
        )
    }

    @Test
    fun ifWithoutActionsToRunHasNoBar() {
        val actions = listOf(makeIfAction(0), makeAction())
        assertEquals(
            listOf(emptyList<Int>(), emptyList<Int>()),
            macroActionIfBars(actions).map { bar -> bar.map { it.level } },
        )
    }

    @Test
    fun ifRunningMoreActionsThanExistEndsAtTheLastAction() {
        val actions = listOf(makeAction(), makeIfAction(10), makeAction())
        val bars = macroActionIfBars(actions)
        assertEquals(
            listOf(emptyList<Int>(), listOf(0), listOf(0)),
            bars.map { bar -> bar.map { it.level } },
        )
        assertEquals(
            listOf(emptyList<Boolean>(), listOf(false), listOf(true)),
            bars.map { bar -> bar.map { it.isLast } },
        )
    }

    private fun makeAction(): SettingsMacrosAction {
        val action = SettingsMacrosAction()
        action.function = SettingsMacrosActionFunction.SNAPSHOT
        return action
    }

    private fun makeIfAction(runCount: Int): SettingsMacrosAction {
        val action = SettingsMacrosAction()
        action.function = SettingsMacrosActionFunction.IF_CONDITION
        action.ifRunCount = runCount
        return action
    }
}
