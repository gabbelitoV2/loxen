package com.moblin.android.platform.mapkit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.swiftui.glassEffect
import com.moblin.android.various.utils.CLLocationCoordinate2D
import com.moblin.android.various.utils.MKCoordinateRegion
import com.moblin.android.various.utils.MKCoordinateSpan
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MapKitMapSuite {
    @get:Rule
    val rule = createComposeRule()

    private val origin = CLLocationCoordinate2D(latitude = 59.33, longitude = 18.06)
    private val region = MKCoordinateRegion(center = origin, span = MKCoordinateSpan(latitudeDelta = 0.02, longitudeDelta = 0.02))

    private fun count(node: LayoutInfo, name: String): Int {
        val getChildren = node.javaClass.methods.first { it.name.startsWith("getChildren") && it.parameterCount == 0 }
        val children = getChildren.invoke(node) as List<*>
        val own = node.getModifierInfo().count { it.modifier.javaClass.name.contains(name) }
        return own + children.sumOf { count(it as LayoutInfo, name) }
    }

    @Test
    fun taggedMarkerIsSelectedAndMapTapClearsSelection() {
        var selection by mutableStateOf<String?>(null)
        rule.setContent {
            Box(modifier = Modifier.size(200.dp).testTag("map")) {
                Map(
                    position = MapCameraPosition.region(region),
                    onPositionChange = {},
                    selection = selection,
                    onSelectionChange = { selection = it },
                ) {
                    Marker(coordinate = origin, tag = "alpha") {
                        Text("Alpha")
                    }
                    Marker(coordinate = CLLocationCoordinate2D(latitude = 59.334, longitude = 18.064)) {
                        Text("Beta")
                    }
                }
            }
        }
        rule.onNodeWithText("Beta").performClick()
        rule.waitForIdle()
        assertNull(selection)
        rule.onNodeWithText("Alpha").performClick()
        rule.waitForIdle()
        assertEquals("alpha", selection)
        rule.onNodeWithTag("map").performTouchInput { click(Offset(5f, 5f)) }
        rule.waitForIdle()
        assertNull(selection)
    }

    @Test
    fun markerTipIsAtItsCoordinate() {
        rule.setContent {
            Box(modifier = Modifier.size(200.dp)) {
                Map(position = MapCameraPosition.region(region), onPositionChange = {}) {
                    Marker(coordinate = origin) {
                        Text("Center")
                    }
                }
            }
        }
        val bounds = rule.onNodeWithText("Center").getUnclippedBoundsInRoot()
        assertTrue(abs(((bounds.left + bounds.right) / 2).value - 100f) < 1f, "$bounds")
        assertTrue(abs(bounds.top.value - 100f) < 1f, "$bounds")
    }

    @Test
    fun cameraChangeReportsTheShownRegion() {
        var reported: MKCoordinateRegion? = null
        rule.setContent {
            Box(modifier = Modifier.size(200.dp)) {
                Map(
                    position = MapCameraPosition.userLocation(followsHeading = true, fallback = MapCameraPosition.region(region)),
                    onPositionChange = {},
                    onMapCameraChange = { reported = it.region },
                )
            }
        }
        rule.waitForIdle()
        val shown = assertNotNull(reported)
        assertEquals(origin.latitude, shown.center.latitude, 1e-6)
        assertEquals(origin.longitude, shown.center.longitude, 1e-6)
        assertEquals(0.02, minOf(shown.span.latitudeDelta, shown.span.longitudeDelta), 1e-4)
    }

    @Test
    fun longPressIsConvertedToACoordinateThroughTheReader() {
        var proxy: MapProxy? = null
        var pressed: CLLocationCoordinate2D? = null
        rule.setContent {
            Box(modifier = Modifier.size(200.dp).testTag("map")) {
                MapReader { reader ->
                    proxy = reader
                    Map(
                        position = MapCameraPosition.region(region),
                        onPositionChange = {},
                        onLongPress = { location -> pressed = reader.convert(location, from = CoordinateSpace.local) },
                    )
                }
            }
        }
        rule.onNodeWithTag("map").performTouchInput { longClick(center) }
        rule.waitForIdle()
        val coordinate = assertNotNull(pressed)
        assertEquals(origin.latitude, coordinate.latitude, 1e-4)
        assertEquals(origin.longitude, coordinate.longitude, 1e-4)
        val point = assertNotNull(assertNotNull(proxy).convert(origin, to = CoordinateSpace.local))
        assertEquals(100.0, point.x, 0.5)
        assertEquals(100.0, point.y, 0.5)
        val back = assertNotNull(proxy?.convert(CGPoint(x = 100.0, y = 100.0), from = CoordinateSpace.local))
        assertEquals(origin.latitude, back.latitude, 1e-6)
    }

    @Test
    fun polylineAndUserAnnotationFollowTheContent() {
        var proxy: MapProxy? = null
        var showRoute by mutableStateOf(true)
        val route = MKPolyline(coordinates = listOf(origin, CLLocationCoordinate2D(latitude = 59.34, longitude = 18.07)))
        rule.setContent {
            Box(modifier = Modifier.size(200.dp)) {
                MapReader { reader ->
                    proxy = reader
                    Map(position = MapCameraPosition.automatic, onPositionChange = {}) {
                        UserAnnotation()
                        if (showRoute) {
                            MapPolyline(route, stroke = androidx.compose.ui.graphics.Color(0xFF007AFF), lineWidth = 5.0)
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        val state = assertNotNull(proxy?.state)
        assertEquals(1, state.polylineCount)
        assertEquals(1, state.userAnnotationCount)
        showRoute = false
        rule.waitForIdle()
        assertEquals(0, state.polylineCount)
    }

    @Test
    fun glassEffectAddsOneGlassLayer() {
        rule.setContent {
            Box(modifier = Modifier.testTag("glass").glassEffect().size(44.dp))
        }
        val node = rule.onNodeWithTag("glass").fetchSemanticsNode().layoutInfo
        assertEquals(1, count(node, "GlassEffect"))
    }
}
