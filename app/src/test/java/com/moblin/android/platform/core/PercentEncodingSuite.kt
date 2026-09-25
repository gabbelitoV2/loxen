package com.moblin.android.platform.core

import com.moblin.android.various.MoblinSettingsUrl
import com.moblin.android.various.MoblinSettingsWebBrowser
import kotlin.test.assertEquals
import org.junit.Test

class PercentEncodingSuite {
    @Test
    fun urlQueryAllowedKeepsAppleCharacters() {
        assertEquals(
            "https://a.b/c?d=e&f=g;h@i!$'()*+,-._~",
            "https://a.b/c?d=e&f=g;h@i!$'()*+,-._~".addingPercentEncoding(CharacterSet.urlQueryAllowed),
        )
    }

    @Test
    fun urlQueryAllowedEncodesTheRestAsUtf8() {
        assertEquals(
            "%7B%22a%22%20%5B%5D%7D%25%23%5C%C3%A5%F0%9F%94%A5",
            "{\"a\" []}%#\\å🔥".addingPercentEncoding(CharacterSet.urlQueryAllowed),
        )
    }

    @Test
    fun urlPathAllowedEncodesQuestionMarkAndSemicolon() {
        assertEquals("/a%3Fb%3Bc", "/a?b;c".addingPercentEncoding(CharacterSet.urlPathAllowed))
    }

    @Test
    fun deepLinkJsonEncodesLikeSwift() {
        val settings = MoblinSettingsUrl()
        settings.webBrowser = MoblinSettingsWebBrowser()
        settings.webBrowser!!.home = "https://moblin.example"
        assertEquals(
            "%7B%22webBrowser%22:%7B%22home%22:%22https://moblin.example%22%7D%7D",
            settings.toString().addingPercentEncoding(CharacterSet.urlQueryAllowed),
        )
    }
}
