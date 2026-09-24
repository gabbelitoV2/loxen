package com.moblin.android.platform.webkit

import android.os.Looper
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.videoeffects.browser.BrowserEffectServer
import com.moblin.android.videoeffects.browser.BrowserEffectServerDelegate
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class WebKitSuite {
    private class Delegate : BrowserEffectServerDelegate {
        var playing = 0
        var ended = 0

        override fun browserEffectServerVideoPlaying() {
            playing += 1
        }

        override fun browserEffectServerVideoEnded() {
            ended += 1
        }
    }

    private class Page(moblinAccess: Boolean) {
        val configuration = WKWebViewConfiguration()
        val server = BrowserEffectServer(configuration = configuration, moblinAccess = moblinAccess)
        val webView = WKWebView(frame = CGRect(0.0, 0.0, 200.0, 100.0), configuration = configuration)
        val delegate = Delegate()

        init {
            server.webView = webView
            server.delegate = delegate
            webView.loadHTMLString("<html></html>", baseURL = null)
            idle()
        }

        fun postFromPage(message: String) {
            val view = webView.borrowView()
            val bridge = assertNotNull(shadowOf(view).getJavascriptInterface(webKitBridgeName))
            val post = bridge.javaClass.getDeclaredMethod("post", String::class.java, String::class.java)
            post.isAccessible = true
            post.invoke(bridge, "moblin", JSONObject.quote(message))
            webView.returnView()
            idle()
        }

        fun lastMessageToPage(): String? {
            val view = webView.borrowView()
            val script = shadowOf(view).lastEvaluatedJavascript
            webView.returnView()
            idle()
            val data = Regex("moblin\\.handleMessage\\(\"([^\"]*)\"\\)").find(script ?: return null)
                ?: return null
            return String(Base64.getDecoder().decode(data.groupValues[1]), Charsets.UTF_8)
        }
    }

    private fun assertSameJson(expected: String, actual: String?) {
        assertNotNull(actual)
        assertEquals(Json.parseToJsonElement(expected), Json.parseToJsonElement(actual))
    }

    @Test
    fun bridgeDefinesMessageHandlersAndBase64Polyfill() {
        val script = WebKitScripts.bridgeInstaller(listOf("moblin"))
        assertTrue(script.contains("var names = [\"moblin\"];"))
        assertTrue(script.contains("window.webkit.messageHandlers[name] = {"))
        assertTrue(script.contains("window[\"moblinBridge\"].post(name, JSON.stringify("))
        assertTrue(script.contains("Object.defineProperty(Uint8Array, 'fromBase64'"))
    }

    @Test
    fun documentEndScriptsRunAfterTheDocumentIsParsed() {
        val script = WebKitScripts.documentEndAtDocumentStart(
            WKUserScript(
                source = "document.head.appendChild(style);",
                injectionTime = WKUserScriptInjectionTime.atDocumentEnd,
                forMainFrameOnly = false,
            )
        )
        assertTrue(script.contains("window.addEventListener('DOMContentLoaded', moblinRunUserScript, { once: true });"))
        assertTrue(script.contains("document.head.appendChild(style);"))
        assertFalse(script.contains("window.top"))
    }

    @Test
    fun mainFrameOnlyScriptsSkipSubframes() {
        val script = WebKitScripts.documentStart(
            WKUserScript(source = "x();", injectionTime = WKUserScriptInjectionTime.atDocumentStart, forMainFrameOnly = true)
        )
        assertTrue(script.startsWith("if (window !== window.top)"))
        assertTrue(script.endsWith("x();"))
    }

    @Test
    fun scriptMessageBodiesBecomeFoundationValues() {
        assertEquals("{\"ping\":{}}", webKitJsonToValue(JSONObject.quote("{\"ping\":{}}")))
        assertEquals(mapOf("a" to listOf(1, "b", null)), webKitJsonToValue("{\"a\":[1,\"b\",null]}"))
        assertEquals(true, webKitJsonToValue("true"))
        assertNull(webKitJsonToValue("null"))
        assertNull(webKitJsonToValue(null))
    }

    @Test
    fun serverAnswersSubscriptionsWithSwiftCodableJson() {
        val page = Page(moblinAccess = true)
        page.server.sendSpeechToText(position = 1, text = "ignored")
        assertNull(page.lastMessageToPage())
        page.postFromPage("{\"subscribe\":{\"topic\":{\"speechToText\":{}}}}")
        page.server.sendSpeechToText(position = 3, text = "hej då")
        assertSameJson(
            "{\"message\":{\"data\":{\"speechToText\":{\"position\":3,\"text\":\"hej då\"}}}}",
            page.lastMessageToPage(),
        )
        page.server.sendSpeechToTextClear()
        assertSameJson("{\"message\":{\"data\":{\"speechToTextClear\":{}}}}", page.lastMessageToPage())
    }

    @Test
    fun serverIgnoresSubscriptionsWithoutMoblinAccess() {
        val page = Page(moblinAccess = false)
        page.postFromPage("{\"subscribe\":{\"topic\":{\"speechToText\":{}}}}")
        page.server.sendSpeechToText(position = 3, text = "hej")
        assertNull(page.lastMessageToPage())
    }

    @Test
    fun serverForwardsVideoPlayingToItsDelegate() {
        val page = Page(moblinAccess = false)
        page.postFromPage("{\"publish\":{\"message\":{\"videoPlaying\":{\"value\":true}}}}")
        assertEquals(1, page.delegate.playing)
        page.postFromPage("{\"publish\":{\"message\":{\"videoPlaying\":{\"value\":false}}}}")
        assertEquals(1, page.delegate.ended)
        page.postFromPage("{\"ping\":{}}")
        page.postFromPage("not json")
        assertEquals(1, page.delegate.playing)
        assertEquals(1, page.delegate.ended)
    }

    private companion object {
        fun idle() {
            shadowOf(Looper.getMainLooper()).idle()
        }
    }
}
