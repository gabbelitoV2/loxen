package com.moblin.android.common.various

import android.net.ConnectivityManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.moblin.android.localized
import com.moblin.android.various.storages.ThermalState
import java.net.URI
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

val iconWidth: Float = 32.0f
val controlBarButtonSize: Float = 40.0f
val controlBarWidthDefault: Float = 100.0f
val controlBarWidthBigQuickButtons: Float = 150.0f
val controlBarQuickButtonNameSize: Float = 10.0f
val controlBarQuickButtonNameSingleColumnSize: Float = 12.0f
val controlBarQuickButtonSingleQuickButtonSize: Float = 60.0f
val stealthModeButtonSize: Float = 80.0f
val maximumNumberOfWatchChatMessages: Int = 50
val heartRateType: Any = TODO("no Android counterpart for HealthKit")
val distanceCyclingType: Any = TODO("no Android counterpart for HealthKit")
val distanceWalkingRunningType: Any = TODO("no Android counterpart for HealthKit")
val stepCountType: Any = TODO("no Android counterpart for HealthKit")
val activeEnergyBurnedType: Any = TODO("no Android counterpart for HealthKit")
val runningPowerType: Any = TODO("no Android counterpart for HealthKit")
val cyclingPowerType: Any = TODO("no Android counterpart for HealthKit")
val cyclingCadenceType: Any = TODO("no Android counterpart for HealthKit")
val personalHotspotLocalAddress: String = "172.20.10.1"
val backgroundColor: Color = Color(0.0f, 0.0f, 0.0f, 0.4f)
val scoreboardBlueColor: Color = RgbColor(red = 0x0B, green = 0x10, blue = 0xAC).color()

fun String.trim(): String {
    return this.trim()
}

fun String.substring(begin: Int, end: Int): String {
    return this.substring(begin, end)
}

fun String.replace(of: String, with: String): String {
    return this.replace(of, with)
}

fun makeRtmpUri(url: String): String {
    val parts = url.split("/")
    if (parts.isEmpty()) {
        return ""
    }
    return parts.subList(0, parts.size - 1).joinToString("/")
}

fun makeRtmpStreamKey(url: String): String {
    val parts = url.split("/").filter { it.isNotEmpty() }
    if (parts.isEmpty()) {
        return ""
    }
    return parts[parts.size - 1]
}

fun cleanUrl(value: String): String {
    val stripped = value.replace(" ", "")
    val uri = runCatching { URI(stripped) }.getOrNull() ?: return stripped
    val scheme = uri.scheme ?: return stripped
    return runCatching {
        URI(
            scheme.lowercase(),
            uri.userInfo,
            uri.host,
            uri.port,
            uri.path,
            uri.query,
            uri.fragment,
        ).toString()
    }.getOrNull() ?: stripped
}

fun replaceSensitive(value: String, sensitive: Boolean): String {
    return if (sensitive) {
        Regex(".").replace(value, "•")
    } else {
        value
    }
}

val countFormatter: IntegerFormatStyle
    get() = IntegerFormatStyle()

val sizeFormatter: ByteCountFormatter
    get() = ByteCountFormatter().apply {
        allowsNonnumericFormatting = false
        countStyle = ByteCountFormatter.CountStyle.decimal
    }

val speedFormatterValue: ByteCountFormatter
    get() = ByteCountFormatter().apply {
        allowsNonnumericFormatting = false
        countStyle = ByteCountFormatter.CountStyle.decimal
        includesUnit = false
    }

val speedFormatterUnit: ByteCountFormatter
    get() = ByteCountFormatter().apply {
        allowsNonnumericFormatting = false
        includesCount = false
    }

fun formatBytesPerSecond(speed: Long): String {
    val value = speedFormatterValue.string(fromByteCount = speed)
    var unit = speedFormatterUnit.string(fromByteCount = speed)
    unit = unit.replace("bytes", "bps")
    unit = unit.replace("byte", "bps")
    if (unit.length == 2) {
        unit = unit[0] + "bps"
    }
    return "$value $unit"
}

private fun createUptimeFormatter(): DateComponentsFormatter {
    return DateComponentsFormatter().apply {
        allowedUnits = setOf(
            DateComponentsFormatter.CalendarUnit.day,
            DateComponentsFormatter.CalendarUnit.hour,
            DateComponentsFormatter.CalendarUnit.minute,
            DateComponentsFormatter.CalendarUnit.second,
        )
        unitsStyle = DateComponentsFormatter.UnitsStyle.abbreviated
    }
}

val uptimeFormatter: DateComponentsFormatter = createUptimeFormatter()

private fun createDigitalClockFormatter(): DateFormatter {
    return DateFormatter().apply {
        dateFormat = "HH:mm"
        amSymbol = ""
        pmSymbol = ""
    }
}

val digitalClockFormatter: DateFormatter = createDigitalClockFormatter()

private fun createDurationFormatter(): DateComponentsFormatter {
    return DateComponentsFormatter().apply {
        allowedUnits = setOf(
            DateComponentsFormatter.CalendarUnit.day,
            DateComponentsFormatter.CalendarUnit.hour,
            DateComponentsFormatter.CalendarUnit.minute,
        )
        unitsStyle = DateComponentsFormatter.UnitsStyle.abbreviated
    }
}

val durationFormatter: DateComponentsFormatter = createDurationFormatter()

fun formatDate(dateString: String): String? {
    return runCatching {
        OffsetDateTime.parse(dateString)
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }.getOrNull()
}

fun formatDate(date: Instant): String {
    return date.atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
}

fun Duration.format(): String {
    return durationFormatter.string(from = inWholeSeconds.toDouble())!!
}

fun Duration.formatWithSeconds(): String {
    return uptimeFormatter.string(from = inWholeSeconds.toDouble())!!
}

private fun createShortDurationFormatter(): DateComponentsFormatter {
    return DateComponentsFormatter().apply {
        allowedUnits = setOf(
            DateComponentsFormatter.CalendarUnit.day,
            DateComponentsFormatter.CalendarUnit.hour,
            DateComponentsFormatter.CalendarUnit.minute,
            DateComponentsFormatter.CalendarUnit.second,
        )
        unitsStyle = DateComponentsFormatter.UnitsStyle.short
        referenceDate = Instant.ofEpochSecond(0)
    }
}

private val shortDurationFormatter: DateComponentsFormatter = createShortDurationFormatter()

fun formatShortDuration(seconds: Int): String {
    return shortDurationFormatter.string(from = seconds.toDouble()) ?: ""
}

private fun createSpeedFormatter(): MeasurementFormatter {
    return MeasurementFormatter().apply {
        numberFormatter.maximumFractionDigits = 0
    }
}

fun formatSpeed(speed: Double): String {
    val measurement = Measurement(value = max(speed, 0.0), unit = UnitSpeed.metersPerSecond)
    return createSpeedFormatter().string(from = measurement)
}

private fun createWindSpeedFormatter(): MeasurementFormatter {
    return MeasurementFormatter().apply {
        numberFormatter.maximumFractionDigits = 0
        unitOptions = MeasurementFormatter.UnitOptions.providedUnit
    }
}

fun formatWindSpeed(speed: Measurement<UnitSpeed>, unit: UnitSpeed?): String {
    val resolvedUnit = unit
        ?: if (measurementSystemMetric()) UnitSpeed.metersPerSecond else UnitSpeed.milesPerHour
    return createWindSpeedFormatter().string(from = speed.converted(to = resolvedUnit))
}

fun formatWindAndGustSpeed(
    speed: Measurement<UnitSpeed>,
    gust: Measurement<UnitSpeed>,
    unit: UnitSpeed?,
): String {
    val resolvedUnit = unit
        ?: if (measurementSystemMetric()) UnitSpeed.metersPerSecond else UnitSpeed.milesPerHour
    val speedValue = speed.converted(to = resolvedUnit).value.toInt()
    val gustValue = gust.converted(to = resolvedUnit).value.toInt()
    return "$speedValue ($gustValue) ${resolvedUnit.symbol}"
}

fun formatPace(speed: Double): String {
    val unit: UnitLength = if (measurementSystemMetric()) UnitLength.kilometers else UnitLength.miles
    val pace: String
    if (speed > 0) {
        val metersPerUnit = Measurement(value = 1.0, unit = unit)
            .converted(to = UnitLength.meters).value
        val secondsPerUnit = (metersPerUnit / speed).toInt()
        val minutes = secondsPerUnit / 60
        val seconds = secondsPerUnit % 60
        pace = String.format(Locale.US, "%d:%02d", minutes, seconds)
    } else {
        pace = "-"
    }
    return localized("$pace min/${unit.symbol}")
}

private fun createDistanceFormatter(): LengthFormatter {
    return LengthFormatter().apply {
        numberFormatter.maximumFractionDigits = 1
    }
}

fun formatDistance(distance: Double): String {
    return createDistanceFormatter().string(fromMeters = distance)
}

fun ThermalState.string(): String {
    return when (this) {
        ThermalState.nominal -> "nominal"
        ThermalState.fair -> "fair"
        ThermalState.serious -> "serious"
        ThermalState.critical -> "critical"
        else -> "unknown"
    }
}

fun appVersion(): String {
    return com.moblin.android.BuildConfig.VERSION_NAME.ifEmpty { "-" }
}

fun <T : Number> formatOneDecimal(value: T): String {
    return String.format(Locale.US, "%.01f", value.toDouble())
}

fun <T : Number> formatTwoDecimals(value: T): String {
    return String.format(Locale.US, "%.02f", value.toDouble())
}

fun <T : Number> formatThreeDecimals(value: T): String {
    return String.format(Locale.US, "%.03f", value.toDouble())
}

fun <T : Number> formatFourDecimals(value: T): String {
    return String.format(Locale.US, "%.04f", value.toDouble())
}

fun <T : Comparable<T>> T.clamped(to: ClosedRange<T>): T {
    return minOf(maxOf(this, to.start), to.endInclusive)
}

fun Double.clamped(to: ClosedRange<Double>): Double {
    return if (isNaN()) to.start else min(max(this, to.start), to.endInclusive)
}

fun Float.clamped(to: ClosedRange<Float>): Float {
    return if (isNaN()) to.start else min(max(this, to.start), to.endInclusive)
}

fun bitrateToMbps(bitrate: UInt): Float {
    return bitrate.toFloat() / 1_000_000f
}

fun bitrateFromMbps(bitrate: Float): UInt {
    return (bitrate * 1_000_000f).toUInt()
}

fun Double.toRadians(): Double {
    return this * PI / 180
}

fun Double.toDegrees(): Double {
    return this * 180 / PI
}

fun Float.toRadians(): Float {
    return this * PI.toFloat() / 180
}

fun Float.toDegrees(): Float {
    return this * 180 / PI.toFloat()
}

fun diffAngles(one: Double, two: Double): Double {
    val diff = abs((one - two).toDegrees())
    return min(diff, 360 - diff)
}

fun diffAngles(one: Float, two: Float): Float {
    val diff = abs((one - two).toDegrees())
    return min(diff, 360 - diff)
}

val Response.isSuccessful: Boolean
    get() = code in 200..299

val Response.isNotFound: Boolean
    get() = code == 404

val Response.isUnauthorized: Boolean
    get() = code == 401

val Response.isForbidden: Boolean
    get() = code == 403

val Response.isTooManyRequests: Boolean
    get() = code == 429

private val httpClient: OkHttpClient = OkHttpClient()

suspend fun httpGet(from: String): Pair<ByteArray, Response> = withContext(Dispatchers.IO) {
    val request = Request.Builder().url(from).build()
    httpClient.newCall(request).execute().use { response ->
        val data = response.body?.bytes() ?: ByteArray(0)
        data to response
    }
}

suspend fun httpGet(request: Request): Pair<ByteArray, Response> = withContext(Dispatchers.IO) {
    httpClient.newCall(request).execute().use { response ->
        val data = response.body?.bytes() ?: ByteArray(0)
        data to response
    }
}

fun Request.Builder.setAuthorization(value: String) {
    header("Authorization", value)
}

fun Request.Builder.setContentType(value: String) {
    header("Content-Type", value)
}

val smallFont: TextStyle = TextStyle(fontSize = 13.sp)

fun ULong.formatBytes(): String {
    return sizeFormatter.string(fromByteCount = toLong())
}

fun ThermalState.color(): Color {
    return when (this) {
        ThermalState.nominal -> Color.White
        ThermalState.fair -> Color.White
        ThermalState.serious -> Color.Yellow
        ThermalState.critical -> Color.Red
        else -> Color(0xFFFFC0CB)
    }
}

fun Int.Companion.fromData(data: ByteArray): Int {
    val diff = Int.SIZE_BYTES - data.size
    val buffer = if (diff > 0) ByteArray(diff) + data else data
    return ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).int
}

fun Long.Companion.fromData(data: ByteArray): Long {
    val diff = Long.SIZE_BYTES - data.size
    val buffer = if (diff > 0) ByteArray(diff) + data else data
    return ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).long
}

fun Short.Companion.fromData(data: ByteArray): Short {
    val diff = Short.SIZE_BYTES - data.size
    val buffer = if (diff > 0) ByteArray(diff) + data else data
    return ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).short
}

fun UInt.Companion.fromData(data: ByteArray): UInt {
    val diff = UInt.SIZE_BYTES - data.size
    val buffer = if (diff > 0) ByteArray(diff) + data else data
    return ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).int.toUInt()
}

fun ULong.Companion.fromData(data: ByteArray): ULong {
    val diff = ULong.SIZE_BYTES - data.size
    val buffer = if (diff > 0) ByteArray(diff) + data else data
    return ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).long.toULong()
}

fun UShort.Companion.fromData(data: ByteArray): UShort {
    val diff = UShort.SIZE_BYTES - data.size
    val buffer = if (diff > 0) ByteArray(diff) + data else data
    return ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).short.toUShort()
}

fun UInt.isBitSet(index: Int): Boolean {
    return ((this shr index) and 1u) == 1u
}

fun ULong.isBitSet(index: Int): Boolean {
    return ((this shr index) and 1uL) == 1uL
}

fun UByte.isBitSet(index: Int): Boolean {
    return ((toUInt() shr index) and 1u) == 1u
}

fun UShort.isBitSet(index: Int): Boolean {
    return ((toUInt() shr index) and 1u) == 1u
}

fun hexStringToByteArray(hexString: String): ByteArray {
    if (hexString.length % 2 != 0) {
        throw IllegalStateException("Not multiple of 2")
    }
    val bytes = ByteArray(hexString.length / 2)
    for (index in bytes.indices) {
        val value = hexString.substring(index * 2, index * 2 + 2)
        bytes[index] = value.toIntOrNull(16)?.toByte()
            ?: throw IllegalStateException("Invalid radix 16 data $value")
    }
    return bytes
}

fun ByteArray.hexString(): String {
    return map { String.format(Locale.US, "%02x", it.toInt() and 0xFF) }.joinToString("")
}

fun ByteArray.getInt64Be(offset: Int = 0): Long {
    return ((getFourBytesBe(offset).toULong() shl 32) or getFourBytesBe(offset + 4).toULong()).toLong()
}

fun ByteArray.getUInt32Be(offset: Int = 0): UInt {
    val b0 = this[offset].toInt() and 0xFF
    val b1 = this[offset + 1].toInt() and 0xFF
    val b2 = this[offset + 2].toInt() and 0xFF
    val b3 = this[offset + 3].toInt() and 0xFF
    return ((b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3).toUInt()
}

fun ByteArray.getUInt16Be(offset: Int = 0): UShort {
    val b0 = this[offset].toInt() and 0xFF
    val b1 = this[offset + 1].toInt() and 0xFF
    return ((b0 shl 8) or b1).toUShort()
}

fun ByteArray.getThreeBytesBe(offset: Int = 0): UInt {
    val b0 = this[offset].toInt() and 0xFF
    val b1 = this[offset + 1].toInt() and 0xFF
    val b2 = this[offset + 2].toInt() and 0xFF
    return ((b0 shl 16) or (b1 shl 8) or b2).toUInt()
}

fun ByteArray.getFourBytesBe(offset: Int = 0): UInt {
    return getUInt32Be(offset)
}

fun ByteArray.getFourBytesLe(offset: Int = 0): UInt {
    val b0 = this[offset].toInt() and 0xFF
    val b1 = this[offset + 1].toInt() and 0xFF
    val b2 = this[offset + 2].toInt() and 0xFF
    val b3 = this[offset + 3].toInt() and 0xFF
    return ((b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0).toUInt()
}

fun ByteArray.setUInt16Be(value: UShort, offset: Int = 0) {
    val v = value.toInt()
    this[offset] = ((v shr 8) and 0xFF).toByte()
    this[offset + 1] = (v and 0xFF).toByte()
}

fun ByteArray.setUInt32Be(value: UInt, offset: Int = 0) {
    val v = value.toLong()
    this[offset] = ((v shr 24) and 0xFF).toByte()
    this[offset + 1] = ((v shr 16) and 0xFF).toByte()
    this[offset + 2] = ((v shr 8) and 0xFF).toByte()
    this[offset + 3] = (v and 0xFF).toByte()
}

fun ByteArray.setInt64Be(value: Long, offset: Int = 0) {
    for (i in 0 until 8) {
        this[offset + i] = ((value shr (8 * (7 - i))) and 0xFF).toByte()
    }
}

fun ByteArray.makeBlockBuffer(advancedBy: Int = 0, length: Int? = null): Any? {
    return TODO("no Android counterpart for CoreMedia CMBlockBuffer")
}

private const val cameraPositionRtmp = "(RTMP)"
private const val cameraPositionSrtla = "(SRT(LA))"
private const val cameraPositionSrtClient = "(SRT client)"
private const val cameraPositionRist = "(RIST)"
private const val cameraPositionRtsp = "(RTSP)"
private const val cameraPositionWhip = "(WHIP)"
private const val cameraPositionWhep = "(WHEP)"
private const val cameraPositionMediaPlayer = "(Media player)"

fun rtmpCamera(name: String): String {
    return "$name $cameraPositionRtmp"
}

fun isRtmpCameraOrMic(camera: String): Boolean {
    return camera.endsWith(cameraPositionRtmp)
}

fun srtlaCamera(name: String): String {
    return "$name $cameraPositionSrtla"
}

fun isSrtlaCameraOrMic(camera: String): Boolean {
    return camera.endsWith(cameraPositionSrtla)
}

fun srtClientCamera(name: String): String {
    return "$name $cameraPositionSrtClient"
}

fun isSrtClientCameraOrMic(camera: String): Boolean {
    return camera.endsWith(cameraPositionSrtClient)
}

fun ristCamera(name: String): String {
    return "$name $cameraPositionRist"
}

fun isRistCameraOrMic(camera: String): Boolean {
    return camera.endsWith(cameraPositionRist)
}

fun rtspCamera(name: String): String {
    return "$name $cameraPositionRtsp"
}

fun isRtspCameraOrMic(camera: String): Boolean {
    return camera.endsWith(cameraPositionRtsp)
}

fun whipCamera(name: String): String {
    return "$name $cameraPositionWhip"
}

fun isWhipCameraOrMic(camera: String): Boolean {
    return camera.endsWith(cameraPositionWhip)
}

fun whepCamera(name: String): String {
    return "$name $cameraPositionWhep"
}

fun isWhepCameraOrMic(camera: String): Boolean {
    return camera.endsWith(cameraPositionWhep)
}

fun mediaPlayerCamera(name: String): String {
    return "$name $cameraPositionMediaPlayer"
}

fun isMediaPlayerCameraOrMic(camera: String): Boolean {
    return camera.endsWith(cameraPositionMediaPlayer)
}

fun formatAudioLevelDb(level: Float): String {
    return localized("${level.toInt()} dB,")
}

fun formatAudioLevel(level: Float, muted: Boolean): String {
    return if (muted) {
        "Muted,"
    } else {
        formatAudioLevelDb(level = level)
    }
}

fun formatAudioLevelChannels(channels: Int): String {
    return localized(" $channels ch")
}

fun formatAudioLevelSampleRate(sampleRate: Double): String {
    return localized(" ${(sampleRate / 1000).toInt()} kHz")
}

val noValue = ""

fun urlImage(interfaceType: Int): String {
    return when (interfaceType) {
        ConnectivityManager.TYPE_WIFI -> "wifi"
        ConnectivityManager.TYPE_MOBILE -> "antenna.radiowaves.left.and.right"
        ConnectivityManager.TYPE_ETHERNET -> "cable.coaxial"
        else -> "questionmark"
    }
}

suspend fun sleepSeconds(seconds: Int) {
    delay(seconds.toLong() * 1000)
}

suspend fun sleepMilliSeconds(milliSeconds: Int) {
    delay(milliSeconds.toLong())
}

val moblinAppGroup: String = "group.com.eerimoq.Moblin"

val Duration.microseconds: Long
    get() = inWholeMicroseconds

val Duration.milliseconds: Long
    get() = inWholeMilliseconds

val Duration.seconds: Double
    get() = inWholeMilliseconds / 1000.0

fun String.Companion.fromUtf8(data: ByteArray): String {
    return String(data, Charsets.UTF_8)
}

val String.utf8Data: ByteArray
    get() = toByteArray(Charsets.UTF_8)

private fun moda(x: Float, m: Float): Float {
    return ((x % m) + m) % m
}

private class HSL {
    var hue: Float = 0.0f
    var saturation: Float = 0.0f
    var lightness: Float = 0.0f

    constructor(hue: Float, saturation: Float, lightness: Float) {
        this.hue = (hue % 360.0f) / 360.0f
        this.saturation = saturation.clamped(to = 0.0f..1.0f)
        this.lightness = lightness.clamped(to = 0.0f..1.0f)
    }

    constructor(color: RgbColor) : this(0.0f, 0.0f, 0.0f) {
        val red = color.red.toFloat() / 255
        val green = color.green.toFloat() / 255
        val blue = color.blue.toFloat() / 255
        val maximum = max(red, max(green, blue))
        val minimum = min(red, min(green, blue))
        val delta = maximum - minimum
        hue = 0.0f
        saturation = 0.0f
        lightness = (maximum + minimum) / 2.0f
        if (delta != 0.0f) {
            if (lightness < 0.5f) {
                saturation = delta / (maximum + minimum)
            } else {
                saturation = delta / (2.0f - maximum - minimum)
            }
            if (red == maximum) {
                hue = (green - blue) / delta
                if (green < blue) {
                    hue += 6.0f
                }
            } else if (green == maximum) {
                hue = ((blue - red) / delta) + 2.0f
            } else if (blue == maximum) {
                hue = ((red - green) / delta) + 4.0f
            }
        }
        hue /= 6.0f
    }

    fun lighter(amount: Float): HSL {
        return HSL(hue = hue * 360.0f, saturation = saturation, lightness = lightness + amount)
    }

    fun toRgbColor(): RgbColor {
        val m2 = if (lightness <= 0.5f) {
            lightness * (saturation + 1.0f)
        } else {
            (lightness + saturation) - (lightness * saturation)
        }
        val m1 = (lightness * 2.0f) - m2
        val r = hueToRGB(m1 = m1, m2 = m2, h = hue + (1.0f / 3.0f))
        val g = hueToRGB(m1 = m1, m2 = m2, h = hue)
        val b = hueToRGB(m1 = m1, m2 = m2, h = hue - (1.0f / 3.0f))
        return RgbColor(
            red = (r * 255).toInt(),
            green = (g * 255).toInt(),
            blue = (b * 255).toInt(),
            opacity = null,
        )
    }

    private fun hueToRGB(m1: Float, m2: Float, h: Float): Float {
        val hue = moda(h, 1.0f)
        if (hue * 6 < 1.0f) {
            return m1 + ((m2 - m1) * hue * 6.0f)
        } else if (hue * 2.0f < 1.0f) {
            return m2
        } else if (hue * 3.0f < 1.9999f) {
            return m1 + ((m2 - m1) * ((2.0f / 3.0f) - hue) * 6.0f)
        }
        return m1
    }
}

@Serializable
data class RgbColor(
    val red: Int,
    val green: Int,
    val blue: Int,
    val opacity: Double? = null,
) {
    companion object {
        val white = RgbColor(red = 255, green = 255, blue = 255)
        val black = RgbColor(red = 0, green = 0, blue = 0)

        fun fromHex(string: String): RgbColor? {
            val colorNumber = string.takeLast(6).toIntOrNull(16) ?: return null
            return RgbColor(
                red = (colorNumber shr 16) and 0xFF,
                green = (colorNumber shr 8) and 0xFF,
                blue = colorNumber and 0xFF,
            )
        }
    }

    fun isDark(): Boolean {
        return (red * 299) + (green * 587) + (blue * 114) < 500 * 255 / 2
    }

    fun makeReadableOnDarkBackground(): RgbColor {
        if (true) {
            return if (isDark()) {
                HSL(color = this).lighter(amount = 0.45f).toRgbColor()
            } else {
                this
            }
        } else {
            val threshold = 100
            if (red >= threshold || green >= threshold || blue >= threshold) {
                return this
            }
            return RgbColor(
                red = red + threshold,
                green = green + threshold,
                blue = blue + threshold,
            )
        }
    }

    fun withOpacity(opacity: Double?): RgbColor {
        return copy(opacity = opacity)
    }

    fun toHex(): String {
        return String.format(Locale.US, "#%02x%02x%02x", red, green, blue)
    }
}

private fun RgbColor.colorScale(color: Int): Double {
    return color / 255.0
}

fun RgbColor.color(): Color {
    return Color(
        red = colorScale(red).toFloat(),
        green = colorScale(green).toFloat(),
        blue = colorScale(blue).toFloat(),
        alpha = (opacity ?: 1.0).toFloat(),
    )
}

fun RgbColor.uiColor(): Color {
    return Color(
        red = colorScale(red).toFloat(),
        green = colorScale(green).toFloat(),
        blue = colorScale(blue).toFloat(),
        alpha = (opacity ?: 1.0).toFloat(),
    )
}

fun RgbColor.hue(): Double {
    return HSL(color = this).hue.toDouble()
}

fun Color.toRgb(): RgbColor? {
    return RgbColor(
        red = (255 * red).toInt(),
        green = (255 * green).toInt(),
        blue = (255 * blue).toInt(),
        opacity = alpha.toDouble(),
    )
}

fun Color.toStandardRgb(): RgbColor? {
    val extendedRgbColor = toRgb() ?: return null
    return RgbColor(
        red = extendedRgbColor.red.coerceIn(0, 255),
        green = extendedRgbColor.green.coerceIn(0, 255),
        blue = extendedRgbColor.blue.coerceIn(0, 255),
        opacity = extendedRgbColor.opacity,
    )
}

fun isSetWin(first: Int, second: Int): Boolean {
    if (first == 7) {
        return true
    }
    if (first == 6 && second <= 4) {
        return true
    }
    return false
}

fun <T> JsonObject.encode(key: String, serializer: KSerializer<T>, value: T): JsonObject {
    return JsonObject(this + (key to Json.encodeToJsonElement(serializer, value)))
}

fun <T> JsonObject.decode(key: String, serializer: KSerializer<T>, defaultValue: T): T {
    val element = this[key] ?: return defaultValue
    return runCatching {
        Json.decodeFromJsonElement(serializer, element)
    }.getOrDefault(defaultValue)
}

fun <T> JsonObject.decode(
    key: String,
    serializer: KSerializer<T>,
    defaultValue: T,
    isValid: (T) -> Boolean,
): T {
    val value = decode(key, serializer, defaultValue)
    return if (isValid(value)) value else defaultValue
}

private fun measurementSystemMetric(): Boolean {
    val country = Locale.getDefault().country.uppercase(Locale.US)
    return country !in setOf("US", "LR", "MM")
}

class IntegerFormatStyle {
    fun format(value: Int): String {
        val sign = if (value < 0) "-" else ""
        val absValue = abs(value.toLong())
        return when {
            absValue < 1_000 -> value.toString()
            absValue < 1_000_000 -> sign + compact(absValue / 1_000.0) + "K"
            absValue < 1_000_000_000 -> sign + compact(absValue / 1_000_000.0) + "M"
            else -> sign + compact(absValue / 1_000_000_000.0) + "B"
        }
    }

    private fun compact(value: Double): String {
        return if (value >= 100 || value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.1f", value)
        }
    }
}

class ByteCountFormatter {
    enum class CountStyle {
        file,
        memory,
        decimal,
        binary,
    }

    var allowsNonnumericFormatting: Boolean = true
    var countStyle: CountStyle = CountStyle.file
    var includesUnit: Boolean = true
    var includesCount: Boolean = true

    fun string(fromByteCount: Long): String {
        val binary = countStyle == CountStyle.binary || countStyle == CountStyle.memory
        val base = if (binary) 1024.0 else 1000.0
        val units = if (binary) {
            listOf("bytes", "KiB", "MiB", "GiB", "TiB", "PiB")
        } else {
            listOf("bytes", "kB", "MB", "GB", "TB", "PB")
        }
        var value = fromByteCount.toDouble()
        var index = 0
        while (abs(value) >= base && index < units.size - 1) {
            value /= base
            index += 1
        }
        val countString = if (index == 0) {
            fromByteCount.toString()
        } else {
            String.format(Locale.US, "%.1f", value)
        }
        val unitString = if (index == 0 && abs(fromByteCount) == 1L) "byte" else units[index]
        return when {
            includesCount && includesUnit -> "$countString $unitString"
            includesCount -> countString
            includesUnit -> unitString
            else -> countString
        }
    }
}

class DateComponentsFormatter {
    enum class CalendarUnit {
        day,
        hour,
        minute,
        second,
    }

    enum class UnitsStyle {
        positional,
        abbreviated,
        short,
        full,
        spellOut,
    }

    var allowedUnits: Set<CalendarUnit> = setOf(
        CalendarUnit.day,
        CalendarUnit.hour,
        CalendarUnit.minute,
        CalendarUnit.second,
    )
    var unitsStyle: UnitsStyle = UnitsStyle.abbreviated
    var referenceDate: Instant? = null

    fun string(from: Double): String? {
        if (!from.isFinite()) {
            return null
        }
        val total = abs(from.toLong())
        val days = total / 86_400
        val hours = (total % 86_400) / 3_600
        val minutes = (total % 3_600) / 60
        val seconds = total % 60
        val components = listOf(
            CalendarUnit.day to days,
            CalendarUnit.hour to hours,
            CalendarUnit.minute to minutes,
            CalendarUnit.second to seconds,
        )
        val parts = components
            .filter { (unit, value) -> unit in allowedUnits && value > 0 }
            .map { (unit, value) -> component(unit, value) }
        if (parts.isEmpty()) {
            return if (unitsStyle == UnitsStyle.abbreviated) "0s" else "0 seconds"
        }
        return when (unitsStyle) {
            UnitsStyle.abbreviated, UnitsStyle.positional -> parts.joinToString(" ")
            else -> parts.joinToString(", ")
        }
    }

    private fun component(unit: CalendarUnit, value: Long): String {
        val abbreviation = when (unit) {
            CalendarUnit.day -> "d"
            CalendarUnit.hour -> "h"
            CalendarUnit.minute -> "m"
            CalendarUnit.second -> "s"
        }
        val singular = when (unit) {
            CalendarUnit.day -> "day"
            CalendarUnit.hour -> "hour"
            CalendarUnit.minute -> "minute"
            CalendarUnit.second -> "second"
        }
        return when (unitsStyle) {
            UnitsStyle.abbreviated, UnitsStyle.positional -> "$value$abbreviation"
            else -> "$value $singular" + if (value == 1L) "" else "s"
        }
    }
}

class DateFormatter {
    var dateFormat: String = ""
    var amSymbol: String = ""
    var pmSymbol: String = ""

    fun string(from: Instant): String {
        return DateTimeFormatter.ofPattern(dateFormat, Locale.getDefault())
            .withZone(ZoneId.systemDefault())
            .format(from)
    }
}

class NumberFormatter {
    var maximumFractionDigits: Int = 3

    fun string(from: Double): String {
        return String.format(Locale.getDefault(), "%.${maximumFractionDigits}f", from)
    }
}

class LengthFormatter {
    var numberFormatter: NumberFormatter = NumberFormatter()

    fun string(fromMeters: Double): String {
        return if (measurementSystemMetric()) {
            "${numberFormatter.string(from = fromMeters / 1000.0)} km"
        } else {
            "${numberFormatter.string(from = fromMeters / 1609.344)} mi"
        }
    }
}

data class Measurement<T>(val value: Double, val unit: T)

enum class UnitSpeed(val symbol: String, val factor: Double) {
    metersPerSecond("m/s", 1.0),
    kilometersPerHour("km/h", 1.0 / 3.6),
    milesPerHour("mph", 0.44704),
}

enum class UnitLength(val symbol: String, val factor: Double) {
    meters("m", 1.0),
    kilometers("km", 1000.0),
    miles("mi", 1609.344),
}

fun Measurement<UnitSpeed>.converted(to: UnitSpeed): Measurement<UnitSpeed> {
    return Measurement(value = value * unit.factor / to.factor, unit = to)
}

fun Measurement<UnitLength>.converted(to: UnitLength): Measurement<UnitLength> {
    return Measurement(value = value * unit.factor / to.factor, unit = to)
}

class MeasurementFormatter {
    enum class UnitOptions {
        naturalScale,
        providedUnit,
        temperatureWithoutUnit,
    }

    var numberFormatter: NumberFormatter = NumberFormatter()
    var unitOptions: UnitOptions = UnitOptions.naturalScale

    fun string(from: Measurement<UnitSpeed>): String {
        val value: Double
        val unit: UnitSpeed
        if (unitOptions == UnitOptions.providedUnit) {
            value = from.value
            unit = from.unit
        } else if (measurementSystemMetric()) {
            value = from.value * from.unit.factor / UnitSpeed.metersPerSecond.factor
            unit = UnitSpeed.metersPerSecond
        } else {
            value = from.value * from.unit.factor / UnitSpeed.milesPerHour.factor
            unit = UnitSpeed.milesPerHour
        }
        return "${numberFormatter.string(from = value)} ${unit.symbol}"
    }
}
