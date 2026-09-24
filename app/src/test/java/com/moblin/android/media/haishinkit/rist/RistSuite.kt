package com.moblin.android.media.haishinkit.rist

import com.moblin.android.platform.network.NWEndpoint
import org.junit.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RistSuite {
    @Test
    fun makeBondingUrl() {
        assertEquals(
            "rist://foobar?secret=1234&weight=1",
            makeRistBondingUrl("rist://foobar?secret=1234")
        )
    }

    @Test
    fun makeBondingUrlWithPort() {
        assertEquals(
            "rist://a.com:54?weight=1",
            makeRistBondingUrl("rist://a.com:54")
        )
    }

    @Test
    fun makeMoblinkBondingUrl() {
        assertEquals(
            "rist://1.2.3.4:143?secret=1234&weight=1",
            makeRistMoblinkBondingUrl(
                "rist://foobar?secret=1234",
                NWEndpoint.hostPort(host = NWEndpoint.Host("1.2.3.4"), port = NWEndpoint.Port(143)),
            )
        )
    }
}
