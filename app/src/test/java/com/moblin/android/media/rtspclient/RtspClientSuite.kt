package com.moblin.android.media.rtspclient

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.Test

class RtspClientSuite {
    @Test
    fun tcpTransportAcceptsValidInterleavedChannels() {
        val transport = RtspTransportRtpRtspTcp()
        transport.handleSetupTransportResponse("RTP/AVP/TCP;unicast;interleaved=0-1")
    }

    @Test
    fun tcpTransportRejectsInvalidInterleavedChannels() {
        val transport = RtspTransportRtpRtspTcp()
        val error = assertFailsWith<Exception> {
            transport.handleSetupTransportResponse("RTP/AVP/TCP;unicast;interleaved=256-257")
        }
        assertEquals("Invalid interleaving channels in RTP/AVP/TCP;unicast;interleaved=256-257.", error.message)
    }

    @Test
    fun udpTransportAcceptsValidServerPorts() {
        val transport = RtspTransportRtpUdp()
        transport.handleSetupTransportResponse("RTP/AVP;unicast;server_port=5004-5005")
    }

    @Test
    fun udpTransportRejectsInvalidRtpServerPort() {
        val transport = RtspTransportRtpUdp()
        val error = assertFailsWith<Exception> {
            transport.handleSetupTransportResponse("RTP/AVP;unicast;server_port=999999-5005")
        }
        assertEquals("Invalid RTP or RTCP server port in: RTP/AVP;unicast;server_port=999999-5005", error.message)
    }
}
