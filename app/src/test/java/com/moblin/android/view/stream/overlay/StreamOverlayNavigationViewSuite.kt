package com.moblin.android.view.stream.overlay

import android.location.Location
import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.platform.mapkit.MapKitConfiguration
import com.moblin.android.platform.mapkit.MapKitCurrentLocation
import com.moblin.android.platform.mapkit.MapKitServices
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.Navigation
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StreamOverlayNavigationViewSuite {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var server: MockWebServer
    private val searchUrl = MapKitConfiguration.searchUrl
    private val routingUrl = MapKitConfiguration.routingUrl
    private val interval = MapKitServices.rateLimitIntervalMs

    private val photon = """
        {"type":"FeatureCollection","features":[
          {"type":"Feature","geometry":{"type":"Point","coordinates":[18.0710,59.3251]},
           "properties":{"name":"Stockholm Central","city":"Stockholm"}}]}
    """.trimIndent()

    private val osrm = """
        {"code":"Ok","routes":[{"distance":900.0,"duration":720.0,
          "geometry":{"type":"LineString","coordinates":[[18.06,59.33],[18.071,59.3251]]},"legs":[{"summary":""}]}]}
    """.trimIndent()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        MapKitConfiguration.searchUrl = server.url("/photon").toString()
        MapKitConfiguration.routingUrl = server.url("/osrm").toString()
        MapKitServices.rateLimitIntervalMs = 0
        val location = Location("test")
        location.latitude = 59.33
        location.longitude = 18.06
        location.time = System.currentTimeMillis()
        MapKitCurrentLocation.reported = location
    }

    @After
    fun tearDown() {
        MapKitConfiguration.searchUrl = searchUrl
        MapKitConfiguration.routingUrl = routingUrl
        MapKitServices.rateLimitIntervalMs = interval
        MapKitCurrentLocation.reported = null
        server.shutdown()
    }

    private fun count(node: LayoutInfo, name: String): Int {
        val getChildren = node.javaClass.methods.first { it.name.startsWith("getChildren") && it.parameterCount == 0 }
        val children = getChildren.invoke(node) as List<*>
        val own = node.getModifierInfo().count { it.modifier.javaClass.name.contains(name) }
        return own + children.sumOf { count(it as LayoutInfo, name) }
    }

    private fun waitFor(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        while (!condition() && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            rule.waitForIdle()
            Thread.sleep(10)
        }
        assertTrue(condition(), "timed out")
    }

    private fun glassLayers(): Int {
        return count(rule.onNodeWithTag("overlay").fetchSemanticsNode().layoutInfo, "GlassEffect")
    }

    private fun show(navigation: Navigation) {
        val model = Model()
        rule.setContent {
            CompositionLocalProvider(LocalModel provides model) {
                Box(modifier = Modifier.size(width = 800.dp, height = 400.dp).testTag("overlay")) {
                    StreamOverlayNavigationView(model = model, database = model.database, navigation = navigation)
                }
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun smallMapHasGlassButtonsAndBigMapAddsSearch() {
        val navigation = Navigation()
        show(navigation)
        assertEquals(2, glassLayers())
        assertEquals(0, rule.onAllNodesWithText("What are you looking for?").fetchSemanticsNodes().size)
        rule.onAllNodes(hasClickAction()).onLast().performClick()
        rule.waitForIdle()
        assertEquals(false, navigation.isSmall.value)
        assertEquals(4, glassLayers())
        assertEquals(1, rule.onAllNodesWithText("What are you looking for?").fetchSemanticsNodes().size)
        rule.onAllNodes(hasClickAction()).onLast().performClick()
        rule.waitForIdle()
        assertEquals(true, navigation.isSmall.value)
    }

    @Test
    fun searchShowsResultsAndPickingOneDrawsTheRouteWithTravelTime() {
        val navigation = Navigation()
        navigation.isSmall.value = false
        show(navigation)
        waitFor { navigation.cameraRegion != null }
        server.enqueue(MockResponse().setBody(photon))
        rule.onNode(hasSetTextAction()).performTextInput("central station")
        rule.onNode(hasSetTextAction()).performImeAction()
        waitFor { navigation.searchResults.value.isNotEmpty() }
        val searchRequest = assertNotNull(server.takeRequest())
        assertEquals("central station", searchRequest.requestUrl!!.queryParameter("q"))
        server.enqueue(MockResponse().setBody(osrm))
        rule.onNodeWithText("Stockholm Central").performClick()
        rule.waitForIdle()
        assertEquals(navigation.searchResults.value.first(), navigation.destination.value)
        waitFor { navigation.route.value != null }
        rule.waitForIdle()
        val label = rule.onAllNodesWithText("Stockholm Central (", substring = true).fetchSemanticsNodes()
        assertEquals(1, label.size)
        assertTrue(server.takeRequest()!!.requestUrl!!.encodedPath.startsWith("/osrm/routed-foot/"))
    }
}
