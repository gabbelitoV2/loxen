package com.moblin.android.platform.srt

import android.util.Log
import com.moblin.android.media.haishinkit.srt.CBytePerfMon
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress

private const val TAG = "MoblinSrt"

typealias SRTSOCKET = Int

typealias SRT_SOCKOPT = Int

fun interface SrtSendHook {
    fun onSend(packet: ByteArray): Boolean
}

class SrtError(message: String) : IOException(message) {
    override fun toString(): String {
        return message ?: ""
    }
}

object SrtNative {
    const val SRT_INVALID_SOCK: Int = -1
    const val SRT_ERROR: Int = -1
    const val SRT_LIVE_DEF_PLSIZE: Int = 1316
    const val SRT_LIVE_MAX_PLSIZE: Int = 1456
    const val SRT_LIVE_DEF_LATENCY_MS: Int = 120

    const val SRTS_INIT: Int = 1
    const val SRTS_OPENED: Int = 2
    const val SRTS_LISTENING: Int = 3
    const val SRTS_CONNECTING: Int = 4
    const val SRTS_CONNECTED: Int = 5
    const val SRTS_BROKEN: Int = 6
    const val SRTS_CLOSING: Int = 7
    const val SRTS_CLOSED: Int = 8
    const val SRTS_NONEXIST: Int = 9

    const val SRTO_MSS: Int = 0
    const val SRTO_SNDSYN: Int = 1
    const val SRTO_RCVSYN: Int = 2
    const val SRTO_ISN: Int = 3
    const val SRTO_FC: Int = 4
    const val SRTO_SNDBUF: Int = 5
    const val SRTO_RCVBUF: Int = 6
    const val SRTO_LINGER: Int = 7
    const val SRTO_UDP_SNDBUF: Int = 8
    const val SRTO_UDP_RCVBUF: Int = 9
    const val SRTO_RENDEZVOUS: Int = 12
    const val SRTO_SNDTIMEO: Int = 13
    const val SRTO_RCVTIMEO: Int = 14
    const val SRTO_REUSEADDR: Int = 15
    const val SRTO_MAXBW: Int = 16
    const val SRTO_STATE: Int = 17
    const val SRTO_EVENT: Int = 18
    const val SRTO_SNDDATA: Int = 19
    const val SRTO_RCVDATA: Int = 20
    const val SRTO_SENDER: Int = 21
    const val SRTO_TSBPDMODE: Int = 22
    const val SRTO_LATENCY: Int = 23
    const val SRTO_INPUTBW: Int = 24
    const val SRTO_OHEADBW: Int = 25
    const val SRTO_PASSPHRASE: Int = 26
    const val SRTO_PBKEYLEN: Int = 27
    const val SRTO_KMSTATE: Int = 28
    const val SRTO_IPTTL: Int = 29
    const val SRTO_IPTOS: Int = 30
    const val SRTO_TLPKTDROP: Int = 31
    const val SRTO_SNDDROPDELAY: Int = 32
    const val SRTO_NAKREPORT: Int = 33
    const val SRTO_VERSION: Int = 34
    const val SRTO_PEERVERSION: Int = 35
    const val SRTO_CONNTIMEO: Int = 36
    const val SRTO_DRIFTTRACER: Int = 37
    const val SRTO_MININPUTBW: Int = 38
    const val SRTO_SNDKMSTATE: Int = 40
    const val SRTO_RCVKMSTATE: Int = 41
    const val SRTO_LOSSMAXTTL: Int = 42
    const val SRTO_RCVLATENCY: Int = 43
    const val SRTO_PEERLATENCY: Int = 44
    const val SRTO_MINVERSION: Int = 45
    const val SRTO_STREAMID: Int = 46
    const val SRTO_CONGESTION: Int = 47
    const val SRTO_MESSAGEAPI: Int = 48
    const val SRTO_PAYLOADSIZE: Int = 49
    const val SRTO_TRANSTYPE: Int = 50
    const val SRTO_KMREFRESHRATE: Int = 51
    const val SRTO_KMPREANNOUNCE: Int = 52
    const val SRTO_ENFORCEDENCRYPTION: Int = 53
    const val SRTO_IPV6ONLY: Int = 54
    const val SRTO_PEERIDLETIMEO: Int = 55
    const val SRTO_BINDTODEVICE: Int = 56
    const val SRTO_GROUPCONNECT: Int = 57
    const val SRTO_GROUPMINSTABLETIMEO: Int = 58
    const val SRTO_GROUPTYPE: Int = 59
    const val SRTO_PACKETFILTER: Int = 60
    const val SRTO_RETRANSMITALGO: Int = 61
    const val SRTO_MAXREXMITBW: Int = 63
    const val SRTO_SRTLAPATCHES: Int = 120

    const val SRTT_LIVE: Int = 0
    const val SRTT_FILE: Int = 1
    const val SRTT_INVALID: Int = 2

    const val SRT_EUNKNOWN: Int = -1
    const val SRT_SUCCESS: Int = 0
    const val SRT_ECONNSETUP: Int = 1000
    const val SRT_ENOSERVER: Int = 1001
    const val SRT_ECONNREJ: Int = 1002
    const val SRT_ESOCKFAIL: Int = 1003
    const val SRT_ESECFAIL: Int = 1004
    const val SRT_ESCLOSED: Int = 1005
    const val SRT_ECONNFAIL: Int = 2000
    const val SRT_ECONNLOST: Int = 2001
    const val SRT_ENOCONN: Int = 2002
    const val SRT_ERESOURCE: Int = 3000
    const val SRT_ETHREAD: Int = 3001
    const val SRT_ENOBUF: Int = 3002
    const val SRT_ESYSOBJ: Int = 3003
    const val SRT_EFILE: Int = 4000
    const val SRT_EINVRDOFF: Int = 4001
    const val SRT_ERDPERM: Int = 4002
    const val SRT_EINVWROFF: Int = 4003
    const val SRT_EWRPERM: Int = 4004
    const val SRT_EINVOP: Int = 5000
    const val SRT_EBOUNDSOCK: Int = 5001
    const val SRT_ECONNSOCK: Int = 5002
    const val SRT_EINVPARAM: Int = 5003
    const val SRT_EINVSOCK: Int = 5004
    const val SRT_EUNBOUNDSOCK: Int = 5005
    const val SRT_ENOLISTEN: Int = 5006
    const val SRT_ERDVNOSERV: Int = 5007
    const val SRT_ERDVUNBOUND: Int = 5008
    const val SRT_EINVALMSGAPI: Int = 5009
    const val SRT_EINVALBUFFERAPI: Int = 5010
    const val SRT_EDUPLISTEN: Int = 5011
    const val SRT_ELARGEMSG: Int = 5012
    const val SRT_EINVPOLLID: Int = 5013
    const val SRT_EPOLLEMPTY: Int = 5014
    const val SRT_EBINDCONFLICT: Int = 5015
    const val SRT_EASYNCFAIL: Int = 6000
    const val SRT_EASYNCSND: Int = 6001
    const val SRT_EASYNCRCV: Int = 6002
    const val SRT_ETIMEOUT: Int = 6003
    const val SRT_ECONGEST: Int = 6004
    const val SRT_EPEERERR: Int = 7000

    const val AF_INET: Int = 2

    init {
        try {
            System.loadLibrary("moblin_srt")
            val version = srt_getversion()
            Log.i(
                TAG,
                "libmoblin_srt loaded, srt ${(version shr 16) and 0xFF}.${(version shr 8) and 0xFF}.${version and 0xFF}",
            )
        } catch (error: Throwable) {
            Log.e(TAG, "libmoblin_srt failed to load: $error")
        }
    }

    @JvmStatic
    external fun srt_getversion(): Int

    @JvmStatic
    external fun srt_startup(): Int

    @JvmStatic
    external fun srt_cleanup(): Int

    @JvmStatic
    external fun srt_create_socket(): Int

    @JvmStatic
    external fun srt_close(u: Int): Int

    @JvmStatic
    external fun srt_setsockopt(u: Int, level: Int, optname: Int, optval: ByteArray, optlen: Int): Int

    @JvmStatic
    external fun srt_getsockflag(u: Int, opt: Int, optval: ByteArray, optlen: IntArray): Int

    @JvmStatic
    external fun srt_getsockflag(u: Int, opt: Int, optval: IntArray, optlen: IntArray): Int

    @JvmStatic
    external fun srt_bind(u: Int, name: ByteArray, namelen: Int): Int

    @JvmStatic
    external fun srt_listen(u: Int, backlog: Int): Int

    @JvmStatic
    external fun srt_accept(u: Int): Int

    fun srt_accept(u: Int, addr: Nothing?, addrlen: Nothing?): Int {
        return srt_accept(u)
    }

    @JvmStatic
    external fun srt_connect(u: Int, name: ByteArray, namelen: Int): Int

    @JvmStatic
    external fun srt_sendmsg2(u: Int, buf: ByteArray, len: Int): Int

    fun srt_sendmsg2(u: Int, buf: ByteArray, len: Int, mctrl: Nothing?): Int {
        return srt_sendmsg2(u, buf, len)
    }

    @JvmStatic
    external fun srt_recvmsg(u: Int, buf: ByteArray, len: Int): Int

    @JvmStatic
    external fun srt_bstats(u: Int, perf: CBytePerfMon, clear: Int): Int

    @JvmStatic
    external fun srt_send_callback(u: Int, hook: SrtSendHook): Int

    @JvmStatic
    external fun srt_getlasterror_str(): String

    @JvmStatic
    external fun srt_getlasterror(errno_loc: IntArray?): Int

    fun srt_getlasterror(): Int {
        return srt_getlasterror(null)
    }

    @JvmStatic
    external fun srt_getrejectreason(u: Int): Int

    @JvmStatic
    external fun srt_rejectreason_str(reason: Int): String

    fun sockaddrIn(host: String, port: Int): ByteArray {
        val clampedPort = port.coerceIn(0, 0xFFFF)
        val address = ByteArray(16)
        address[0] = (AF_INET and 0xFF).toByte()
        address[1] = ((AF_INET shr 8) and 0xFF).toByte()
        address[2] = ((clampedPort shr 8) and 0xFF).toByte()
        address[3] = (clampedPort and 0xFF).toByte()
        val resolved = try {
            InetAddress.getAllByName(host).firstOrNull { it is Inet4Address }
        } catch (error: Exception) {
            Log.i(TAG, "Failed to resolve $host: $error")
            null
        }
        resolved?.address?.copyInto(address, destinationOffset = 4)
        return address
    }
}
