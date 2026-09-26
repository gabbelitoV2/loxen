package com.moblin.android.platform.mapkit

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.moblin.android.AppDelegate
import com.moblin.android.various.utils.CLLocationCoordinate2D
import com.moblin.android.various.utils.MKCoordinateRegion
import com.moblin.android.various.utils.MKCoordinateSpan
import java.io.IOException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.log2
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

internal class MapKitRateLimiter(
    private val intervalMs: Long,
    private val clock: () -> Long = { SystemClock.elapsedRealtime() },
) {
    private var next = Long.MIN_VALUE

    @Synchronized
    fun reserve(): Long {
        val now = clock()
        val start = max(now, next)
        next = start + intervalMs
        return start - now
    }
}

internal object MapKitServices {
    private const val photonLanguages = "de en fr it"
    private val limiters = ConcurrentHashMap<String, MapKitRateLimiter>()
    private val mainHandler: Handler by lazy { Handler(Looper.getMainLooper()) }
    private val json = Json { ignoreUnknownKeys = true }

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    var rateLimitIntervalMs = 1_000L

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    val userAgent: String by lazy {
        val context = applicationContext()
        val packageName = context?.packageName ?: "com.moblin.android"
        val version = try {
            @Suppress("DEPRECATION")
            context?.packageManager?.getPackageInfo(packageName, 0)?.versionName
        } catch (_: Throwable) {
            null
        }
        "$packageName/${version ?: "1"} (Android)"
    }

    fun <T> run(
        block: suspend () -> T,
        isCancelled: () -> Boolean,
        completionHandler: (T?, Throwable?) -> Unit,
    ): Job {
        return scope.launch {
            val result = runCatching { block() }
            deliverOnMain {
                if (isCancelled()) {
                    return@deliverOnMain
                }
                completionHandler(result.getOrNull(), result.exceptionOrNull()?.let(::toMKError))
            }
        }
    }

    fun deliverOnMain(block: () -> Unit) {
        val mainLooper = Looper.getMainLooper()
        if (mainLooper != null && Looper.myLooper() === mainLooper) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    fun toMKError(error: Throwable): Throwable {
        return error as? MKError ?: MKError(error.message ?: error.javaClass.simpleName)
    }

    suspend fun getJson(url: HttpUrl): JsonElement {
        val limiter = limiters.getOrPut("${url.host}:${url.port}:$rateLimitIntervalMs") {
            MapKitRateLimiter(intervalMs = rateLimitIntervalMs)
        }
        val wait = limiter.reserve()
        if (wait > 0) {
            delay(wait)
        }
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .build()
        val body = execute(client.newCall(request))
        return try {
            json.parseToJsonElement(body)
        } catch (error: Exception) {
            throw MKError("Invalid response from ${url.host}")
        }
    }

    private suspend fun execute(call: Call): String {
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation {
                call.cancel()
            }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWith(Result.failure(MKError(e.message ?: "Network error")))
                }

                override fun onResponse(call: Call, response: Response) {
                    val result = response.use {
                        val text = it.body?.string() ?: ""
                        if (it.isSuccessful) {
                            Result.success(text)
                        } else {
                            Result.failure(MKError("HTTP ${it.code} from ${call.request().url.host}"))
                        }
                    }
                    continuation.resumeWith(result)
                }
            })
        }
    }

    fun photonLanguage(): String? {
        val language = Locale.getDefault().language
        return if (language.isNotEmpty() && photonLanguages.split(" ").contains(language)) language else null
    }

    fun searchUrl(query: String, region: MKCoordinateRegion?, limit: Int): HttpUrl {
        val builder = MapKitConfiguration.searchUrl.trimEnd('/').toHttpUrl().newBuilder()
            .addPathSegment("api")
            .addQueryParameter("q", query)
            .addQueryParameter("limit", limit.toString())
        if (region != null) {
            builder.addQueryParameter("lat", format(region.center.latitude))
            builder.addQueryParameter("lon", format(region.center.longitude))
            builder.addQueryParameter("zoom", biasZoom(region).toString())
            builder.addQueryParameter("location_bias_scale", "0.2")
        }
        photonLanguage()?.let { builder.addQueryParameter("lang", it) }
        return builder.build()
    }

    fun reverseUrl(coordinate: CLLocationCoordinate2D): HttpUrl {
        val builder = MapKitConfiguration.searchUrl.trimEnd('/').toHttpUrl().newBuilder()
            .addPathSegment("reverse")
            .addQueryParameter("lat", format(coordinate.latitude))
            .addQueryParameter("lon", format(coordinate.longitude))
            .addQueryParameter("limit", "1")
        photonLanguage()?.let { builder.addQueryParameter("lang", it) }
        return builder.build()
    }

    fun routeProfile(transportType: MKDirectionsTransportType): String? {
        return when (transportType) {
            MKDirectionsTransportType.automobile, MKDirectionsTransportType.any -> "routed-car"
            MKDirectionsTransportType.cycling -> "routed-bike"
            MKDirectionsTransportType.walking -> "routed-foot"
            MKDirectionsTransportType.transit -> null
        }
    }

    fun routeUrl(
        profile: String,
        source: CLLocationCoordinate2D,
        destination: CLLocationCoordinate2D,
        alternatives: Boolean,
    ): HttpUrl {
        val coordinates = "${format(source.longitude)},${format(source.latitude)};" +
            "${format(destination.longitude)},${format(destination.latitude)}"
        return MapKitConfiguration.routingUrl.trimEnd('/').toHttpUrl().newBuilder()
            .addPathSegment(profile)
            .addPathSegments("route/v1/driving")
            .addPathSegment(coordinates)
            .addQueryParameter("overview", "full")
            .addQueryParameter("geometries", "geojson")
            .addQueryParameter("steps", "false")
            .addQueryParameter("alternatives", alternatives.toString())
            .build()
    }

    fun parsePhoton(element: JsonElement): List<MKMapItem> {
        val features = (element as? JsonObject)?.get("features") as? JsonArray ?: return emptyList()
        return features.mapNotNull { element ->
            val feature = element as? JsonObject ?: return@mapNotNull null
            val geometry = feature["geometry"] as? JsonObject ?: return@mapNotNull null
            val coordinates = geometry["coordinates"] as? JsonArray ?: return@mapNotNull null
            val longitude = coordinates.getOrNull(0)?.let { (it as? JsonPrimitive)?.doubleOrNull }
            val latitude = coordinates.getOrNull(1)?.let { (it as? JsonPrimitive)?.doubleOrNull }
            if (longitude == null || latitude == null) {
                return@mapNotNull null
            }
            val properties = feature["properties"] as? JsonObject ?: JsonObject(emptyMap())
            fun text(key: String): String? = (properties[key] as? JsonPrimitive)?.contentOrNull?.ifBlank { null }
            val street = text("street")
            val houseNumber = text("housenumber")
            val locality = text("city") ?: text("town") ?: text("village") ?: text("locality")
            val placemark = MKPlacemark(
                coordinate = CLLocationCoordinate2D(latitude = latitude, longitude = longitude),
                thoroughfare = street,
                subThoroughfare = houseNumber,
                locality = locality,
                subLocality = text("district"),
                postalCode = text("postcode"),
                administrativeArea = text("state"),
                country = text("country"),
                isoCountryCode = text("countrycode")?.uppercase(Locale.ROOT),
            )
            val streetName = listOfNotNull(street, houseNumber).joinToString(" ").ifEmpty { null }
            val item = MKMapItem(placemark = placemark)
            item.name = text("name") ?: streetName ?: locality ?: text("state") ?: text("country")
            item
        }
    }

    fun parseRoutes(
        element: JsonElement,
        transportType: MKDirectionsTransportType,
    ): List<MKRoute> {
        val root = element as? JsonObject ?: throw MKError("Invalid route response")
        val code = (root["code"] as? JsonPrimitive)?.contentOrNull
        if (code != "Ok") {
            val message = (root["message"] as? JsonPrimitive)?.contentOrNull
            throw MKError(message ?: "Directions not available ($code)")
        }
        val routes = root["routes"] as? JsonArray ?: return emptyList()
        return routes.mapNotNull { element ->
            val route = element as? JsonObject ?: return@mapNotNull null
            val geometry = route["geometry"] as? JsonObject ?: return@mapNotNull null
            val points = (geometry["coordinates"] as? JsonArray)?.mapNotNull { point ->
                val pair = point as? JsonArray ?: return@mapNotNull null
                val longitude = (pair.getOrNull(0) as? JsonPrimitive)?.doubleOrNull
                val latitude = (pair.getOrNull(1) as? JsonPrimitive)?.doubleOrNull
                if (longitude == null || latitude == null) {
                    null
                } else {
                    CLLocationCoordinate2D(latitude = latitude, longitude = longitude)
                }
            } ?: return@mapNotNull null
            val distance = (route["distance"] as? JsonPrimitive)?.doubleOrNull ?: 0.0
            val duration = (route["duration"] as? JsonPrimitive)?.doubleOrNull ?: 0.0
            val legs = route["legs"] as? JsonArray
            val summary = ((legs?.firstOrNull() as? JsonObject)?.get("summary") as? JsonPrimitive)?.contentOrNull
            MKRoute(
                polyline = MKPolyline(coordinates = points),
                distance = distance,
                expectedTravelTime = duration,
                name = summary ?: "",
                transportType = transportType,
            )
        }
    }

    fun boundingRegion(coordinates: List<CLLocationCoordinate2D>): MKCoordinateRegion {
        if (coordinates.isEmpty()) {
            return MKCoordinateRegion(
                center = CLLocationCoordinate2D(latitude = 0.0, longitude = 0.0),
                span = MKCoordinateSpan(latitudeDelta = 180.0, longitudeDelta = 360.0),
            )
        }
        val minimumLatitude = coordinates.minOf { it.latitude }
        val maximumLatitude = coordinates.maxOf { it.latitude }
        val minimumLongitude = coordinates.minOf { it.longitude }
        val maximumLongitude = coordinates.maxOf { it.longitude }
        return MKCoordinateRegion(
            center = CLLocationCoordinate2D(
                latitude = (minimumLatitude + maximumLatitude) / 2,
                longitude = (minimumLongitude + maximumLongitude) / 2,
            ),
            span = MKCoordinateSpan(
                latitudeDelta = maximumLatitude - minimumLatitude,
                longitudeDelta = maximumLongitude - minimumLongitude,
            ),
        )
    }

    private fun biasZoom(region: MKCoordinateRegion): Int {
        val degrees = max(region.span.latitudeDelta, region.span.longitudeDelta)
        if (!(degrees > 0.0) || !degrees.isFinite()) {
            return 14
        }
        return log2(360.0 / degrees).toInt().coerceIn(1, 18)
    }

    private fun format(value: Double): String {
        return String.format(Locale.US, "%.6f", value)
    }

    fun applicationContext(): Context? {
        return try {
            AppDelegate.context
        } catch (_: Throwable) {
            null
        }
    }
}

internal object MapKitCurrentLocation {
    @Volatile
    var reported: Location? = null

    fun hasPermission(context: Context): Boolean {
        return listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
            context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun latest(): Location? {
        val candidates = mutableListOf<Location>()
        reported?.let { candidates.add(it) }
        val context = MapKitServices.applicationContext()
        if (context != null && hasPermission(context)) {
            val manager = context.getSystemService(LocationManager::class.java)
            val providers = try {
                manager?.getProviders(true) ?: emptyList()
            } catch (_: Throwable) {
                emptyList()
            }
            for (provider in providers) {
                try {
                    manager?.getLastKnownLocation(provider)?.let { candidates.add(it) }
                } catch (_: SecurityException) {
                } catch (_: IllegalArgumentException) {
                }
            }
        }
        return candidates.maxByOrNull { it.time }
    }
}

val Location.coordinate: CLLocationCoordinate2D
    get() = CLLocationCoordinate2D(latitude = latitude, longitude = longitude)
