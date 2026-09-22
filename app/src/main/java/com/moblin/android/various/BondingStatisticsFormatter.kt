package com.moblin.android.various

import androidx.compose.ui.graphics.Color
import com.moblin.android.common.various.RgbColor
import com.moblin.android.various.settings.SettingsNetworkInterfaceName

data class BondingConnection(
    val name: String,
    var usage: Long,
    var rtt: Int?,
)

data class BondingPercentage(
    val id: Int,
    val percentage: Long,
    val color: Color,
)

private val colors: List<Color> = listOf(
    Color(red = 0xE6, green = 0x9F, blue = 0x00),
    Color(red = 0x00, green = 0x9E, blue = 0x73),
    Color(red = 0xF0, green = 0xE4, blue = 0x42),
    Color(red = 0x00, green = 0x72, blue = 0xB2),
    Color(red = 0xCC, green = 0x79, blue = 0xA7),
    Color(red = 0x56, green = 0xB4, blue = 0xE9),
    Color(red = 0xD5, green = 0x5E, blue = 0x00),
)

class BondingStatisticsFormatter {
    private var networkInterfaceNames: List<SettingsNetworkInterfaceName> = listOf()

    fun setNetworkInterfaceNames(networkInterfaceNames: List<SettingsNetworkInterfaceName>) {
        this.networkInterfaceNames = networkInterfaceNames
    }

    fun format(connections: List<BondingConnection>): Triple<String, String, List<BondingPercentage>>? {
        if (connections.isEmpty()) {
            return null
        }
        var totalUsage = connections.fold(0L) { partialResult, connection ->
            partialResult + connection.usage
        }
        if (totalUsage == 0L) {
            totalUsage = 1L
        }
        val percentages = connections.map { connection ->
            BondingConnection(
                name = connection.name,
                usage = 100L * connection.usage / totalUsage,
                rtt = null,
            )
        }.toMutableList()
        percentages[percentages.size - 1].usage = 100L -
            percentages.take(percentages.size - 1).fold(0L) { total, percentage ->
                total + percentage.usage
            }
        val message = percentages.map { percentage ->
            "${percentage.usage}% ${percentage.name}"
        }.joinToString(separator = ", ")
        val rtts = connections.map { connection ->
            "${connection.rtt ?: -1} ms ${connection.name}"
        }.joinToString(separator = ", ")
        return Triple(
            message,
            rtts,
            percentages.mapIndexed { index, percentage ->
                BondingPercentage(
                    id = index,
                    percentage = percentage.usage,
                    color = getNameColor(percentage.name),
                )
            },
        )
    }

    private fun getNameColor(name: String): Color {
        return if (name == "Cellular") {
            colors[0]
        } else if (name == "WiFi") {
            colors[1]
        } else {
            val index = networkInterfaceNames.indexOfFirst { name == it.name }
            if (index != -1) {
                colors[(index + 2) % colors.size]
            } else {
                colors[(2 + networkInterfaceNames.size) % colors.size]
            }
        }
    }
}
