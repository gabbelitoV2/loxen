package com.moblin.android.media.haishinkit.srt

data class CBytePerfMon(
    var pktRetransTotal: Int = 0,
    var pktRecvNAKTotal: Int = 0,
    var pktSndDropTotal: Int = 0,
    var pktFlightSize: Int = 0,
    var msRTT: Double = 0.0,
    var pktSndBuf: Int = 0,
    var mbpsSendRate: Double = 0.0,
)

data class SrtPerformanceData(
    var pktRetransTotal: Int,
    var pktRecvNakTotal: Int,
    var pktSndDropTotal: Int,
    var pktFlightSize: Int,
    var msRtt: Double,
    var pktSndBuf: Int,
    var mbpsSendRate: Double,
) {
    constructor(mon: CBytePerfMon) : this(
        pktRetransTotal = mon.pktRetransTotal,
        pktRecvNakTotal = mon.pktRecvNAKTotal,
        pktSndDropTotal = mon.pktSndDropTotal,
        pktFlightSize = mon.pktFlightSize,
        msRtt = mon.msRTT,
        pktSndBuf = mon.pktSndBuf,
        mbpsSendRate = mon.mbpsSendRate,
    )

    companion object {
        val zero = SrtPerformanceData(
            pktRetransTotal = 0,
            pktRecvNakTotal = 0,
            pktSndDropTotal = 0,
            pktFlightSize = 0,
            msRtt = 0.0,
            pktSndBuf = 0,
            mbpsSendRate = 0.0,
        )
    }
}
