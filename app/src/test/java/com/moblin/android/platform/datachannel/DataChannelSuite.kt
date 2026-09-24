package com.moblin.android.platform.datachannel

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.Test

class DataChannelSuite {
    @Test
    fun connectionStateFromValue() {
        assertEquals(DataChannelConnectionState.new, DataChannelConnectionState(value = RTC_NEW))
        assertEquals(DataChannelConnectionState.connecting, DataChannelConnectionState(value = RTC_CONNECTING))
        assertEquals(DataChannelConnectionState.connected, DataChannelConnectionState(value = RTC_CONNECTED))
        assertEquals(DataChannelConnectionState.disconnected, DataChannelConnectionState(value = RTC_DISCONNECTED))
        assertEquals(DataChannelConnectionState.failed, DataChannelConnectionState(value = RTC_FAILED))
        assertEquals(DataChannelConnectionState.closed, DataChannelConnectionState(value = RTC_CLOSED))
        assertNull(DataChannelConnectionState(value = 6))
        assertNull(DataChannelConnectionState(value = -1))
        assertEquals("connected", "${DataChannelConnectionState.connected}")
    }

    @Test
    fun gatheringStateFromValue() {
        assertEquals(DataChannelGatheringState.new, DataChannelGatheringState(value = RTC_GATHERING_NEW))
        assertEquals(DataChannelGatheringState.inProgress, DataChannelGatheringState(value = RTC_GATHERING_INPROGRESS))
        assertEquals(DataChannelGatheringState.complete, DataChannelGatheringState(value = RTC_GATHERING_COMPLETE))
        assertNull(DataChannelGatheringState(value = 3))
        assertEquals("inProgress", "${DataChannelGatheringState.inProgress}")
    }

    @Test
    fun errorPrintsItsMessage() {
        assertEquals("Error -2", "${DataChannelError("Error -2")}")
    }

    @Test
    fun userPointersResolveToTheSameObject() {
        val first = Any()
        val second = Any()
        val firstHandle = DataChannelUserPointers.register(first)
        val secondHandle = DataChannelUserPointers.register(second)
        assertNotEquals(0L, firstHandle)
        assertNotEquals(firstHandle, secondHandle)
        assertEquals(firstHandle, DataChannelUserPointers.register(first))
        assertSame(first, DataChannelUserPointers.get(firstHandle))
        assertSame(second, DataChannelUserPointers.get(secondHandle))
        assertEquals(0L, DataChannelUserPointers.register(null))
        assertNull(DataChannelUserPointers.get(0))
        assertNull(DataChannelUserPointers.get(Long.MAX_VALUE))
    }

    @Test
    fun callbacksGetTheUserPointer() {
        val owner = Any()
        val handle = DataChannelUserPointers.register(owner)
        val states = mutableListOf<Pair<Int, Any?>>()
        DataChannelCallbacks.stateChange[1001] = { pc, state, ptr ->
            assertEquals(1001, pc)
            states.add(Pair(state, ptr))
        }
        var frame: Triple<Int, rtcFrameInfo?, Any?>? = null
        DataChannelCallbacks.frame[1002] = { _, data, size, info, ptr ->
            assertEquals(size, data?.size)
            frame = Triple(size, info, ptr)
        }
        DataChannelNative.onStateChange(1001, RTC_CONNECTED, handle)
        DataChannelNative.onStateChange(1001, RTC_CLOSED, 0)
        DataChannelNative.onStateChange(1003, RTC_CLOSED, handle)
        DataChannelNative.onFrame(1002, byteArrayOf(1, 2, 3), 3, true, -1, 96, -1.0, handle)
        assertEquals(listOf(Pair(RTC_CONNECTED, owner), Pair(RTC_CLOSED, null)), states)
        assertEquals(3, frame?.first)
        assertEquals(UInt.MAX_VALUE, frame?.second?.timestamp)
        assertEquals(96.toUByte(), frame?.second?.payloadType)
        assertSame(owner, frame?.third)
        DataChannelCallbacks.addTrack(1001, 1002)
        DataChannelCallbacks.removePeerConnection(1001)
        assertFalse(DataChannelCallbacks.stateChange.containsKey(1001))
        assertFalse(DataChannelCallbacks.frame.containsKey(1002))
    }

    @Test
    fun functionsFailWithoutTheNativeLibrary() {
        if (DataChannelNative.isLoaded) {
            return
        }
        assertEquals(RTC_ERR_FAILURE, rtcCreatePeerConnection(rtcConfiguration()))
        assertEquals(RTC_ERR_FAILURE, rtcSetStateChangeCallback(1) { _, _, _ -> })
        assertEquals(RTC_ERR_FAILURE, rtcSendMessage(1, ByteArray(1), 1))
        assertTrue(rtcIsClosed(1))
        assertNull(rtcGetUserPointer(1))
    }
}
