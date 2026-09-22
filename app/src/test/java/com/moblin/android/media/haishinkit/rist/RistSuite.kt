package com.moblin.android.media.haishinkit.rist

import org.junit.Test
import kotlin.test.assertEquals

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
            makeRistMoblinkBondingUrl("rist://foobar?secret=1234", RistEndpoint("1.2.3.4", 143))
        )
    }
}
