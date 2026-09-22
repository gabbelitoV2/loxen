package com.moblin.android.media.haishinkit.srt

import android.net.Uri
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val TAG = "SrtSocketOption"

private const val SRTT_LIVE = 1
private const val SRTT_FILE = 2

private const val SRTO_MSS = 0
private const val SRTO_SNDSYN = 1
private const val SRTO_RCVSYN = 2
private const val SRTO_ISN = 3
private const val SRTO_FC = 4
private const val SRTO_SNDBUF = 5
private const val SRTO_RCVBUF = 6
private const val SRTO_LINGER = 7
private const val SRTO_UDP_SNDBUF = 8
private const val SRTO_UDP_RCVBUF = 9
private const val SRTO_RENDEZVOUS = 10
private const val SRTO_SNDTIMEO = 11
private const val SRTO_RCVTIMEO = 12
private const val SRTO_REUSEADDR = 13
private const val SRTO_MAXBW = 14
private const val SRTO_STATE = 15
private const val SRTO_EVENT = 16
private const val SRTO_SNDDATA = 17
private const val SRTO_RCVDATA = 18
private const val SRTO_SENDER = 19
private const val SRTO_TSBPDMODE = 20
private const val SRTO_LATENCY = 21
private const val SRTO_INPUTBW = 22
private const val SRTO_OHEADBW = 23
private const val SRTO_PASSPHRASE = 24
private const val SRTO_PBKEYLEN = 25
private const val SRTO_KMSTATE = 26
private const val SRTO_IPTTL = 27
private const val SRTO_IPTOS = 28
private const val SRTO_TLPKTDROP = 29
private const val SRTO_SNDDROPDELAY = 32
private const val SRTO_NAKREPORT = 31
private const val SRTO_CONNTIMEO = 34
private const val SRTO_SNDKMSTATE = 36
private const val SRTO_LOSSMAXTTL = 38
private const val SRTO_RCVLATENCY = 39
private const val SRTO_PEERLATENCY = 40
private const val SRTO_MINVERSION = 41
private const val SRTO_STREAMID = 42
private const val SRTO_MESSAGEAPI = 44
private const val SRTO_PAYLOADSIZE = 45
private const val SRTO_TRANSTYPE = 46
private const val SRTO_KMREFRESHRATE = 47
private const val SRTO_KMPREANNOUNCE = 48
private const val SRTO_MAXREXMITBW = 53
private const val SRTO_SRTLAPATCHES = 1000

private val enummapTranstype: Map<String, Int> = mapOf(
    "live" to SRTT_LIVE,
    "file" to SRTT_FILE,
)

private fun Int.toSrtBytes(): ByteArray =
    ByteBuffer.allocate(Int.SIZE_BYTES).order(ByteOrder.nativeOrder()).putInt(this).array()

private fun Long.toSrtBytes(): ByteArray =
    ByteBuffer.allocate(Long.SIZE_BYTES).order(ByteOrder.nativeOrder()).putLong(this).array()

internal object SrtNative {
    init {
        System.loadLibrary("srt")
    }

    external fun srt_setsockopt(u: Int, level: Int, optname: Int, optval: ByteArray, optlen: Int): Int
}

enum class SrtSocketOption(val rawValue: String) {
    mss("mss"),
    sndsyn("sndsyn"),
    rcvsyn("rcvsyn"),
    isn("isn"),
    fc("fc"),
    sndbuf("sndbuf"),
    rcvbuf("rcvbuf"),
    linger("linger"),
    udpsndbuf("udpsndbuf"),
    udprcvbuf("udprcvbuf"),
    rendezvous("rendezvous"),
    sndtimeo("sndtimeo"),
    rcvtimeo("rcvtimeo"),
    reuseaddr("reuseaddr"),
    maxbw("maxbw"),
    state("state"),
    event("event"),
    snddata("snddata"),
    rcvdata("rcvdata"),
    sender("sender"),
    tsbdmode("tsbdmode"),
    latency("latency"),
    inputbw("inputbw"),
    oheadbw("oheadbw"),
    passphrase("passphrase"),
    pbkeylen("pbkeylen"),
    kmstate("kmstate"),
    ipttl("ipttl"),
    iptos("iptos"),
    tlpktdrop("tlpktdrop"),
    snddropdelay("snddropdelay"),
    nakreport("nakreport"),
    conntimeo("conntimeo"),
    sndkmstate("sndkmstate"),
    lossmaxttl("lossmaxttl"),
    rcvlatency("rcvlatency"),
    peerlatency("peerlatency"),
    minversion("minversion"),
    streamid("streamid"),
    messageapi("messageapi"),
    payloadsize("payloadsize"),
    transtype("transtype"),
    kmrefreshrate("kmrefreshrate"),
    kmpreannounce("kmpreannounce"),
    maxrexmitbw("maxrexmitbw"),
    srtlaPatches("srtlaPatches");

    enum class Type(val rawValue: Int) {
        string(0),
        int(1),
        int64(2),
        bool(3),
        enumeration(4),
    }

    enum class Binding(val rawValue: Int) {
        pre(0),
        post(1),
    }

    private val symbol: Int
        get() = when (this) {
            rcvsyn -> SRTO_RCVSYN
            maxbw -> SRTO_MAXBW
            pbkeylen -> SRTO_PBKEYLEN
            passphrase -> SRTO_PASSPHRASE
            mss -> SRTO_MSS
            fc -> SRTO_FC
            sndbuf -> SRTO_SNDBUF
            rcvbuf -> SRTO_RCVBUF
            ipttl -> SRTO_IPTTL
            iptos -> SRTO_IPTOS
            inputbw -> SRTO_INPUTBW
            oheadbw -> SRTO_OHEADBW
            latency -> SRTO_LATENCY
            tsbdmode -> SRTO_TSBPDMODE
            tlpktdrop -> SRTO_TLPKTDROP
            nakreport -> SRTO_NAKREPORT
            conntimeo -> SRTO_CONNTIMEO
            lossmaxttl -> SRTO_LOSSMAXTTL
            rcvlatency -> SRTO_RCVLATENCY
            peerlatency -> SRTO_PEERLATENCY
            minversion -> SRTO_MINVERSION
            streamid -> SRTO_STREAMID
            messageapi -> SRTO_MESSAGEAPI
            payloadsize -> SRTO_PAYLOADSIZE
            transtype -> SRTO_TRANSTYPE
            kmrefreshrate -> SRTO_KMREFRESHRATE
            kmpreannounce -> SRTO_KMPREANNOUNCE
            maxrexmitbw -> SRTO_MAXREXMITBW
            sndsyn -> SRTO_SNDSYN
            isn -> SRTO_ISN
            linger -> SRTO_LINGER
            udpsndbuf -> SRTO_UDP_SNDBUF
            udprcvbuf -> SRTO_UDP_RCVBUF
            rendezvous -> SRTO_RENDEZVOUS
            sndtimeo -> SRTO_SNDTIMEO
            rcvtimeo -> SRTO_RCVTIMEO
            reuseaddr -> SRTO_REUSEADDR
            state -> SRTO_STATE
            event -> SRTO_EVENT
            snddata -> SRTO_SNDDATA
            rcvdata -> SRTO_RCVDATA
            sender -> SRTO_SENDER
            kmstate -> SRTO_KMSTATE
            snddropdelay -> SRTO_SNDDROPDELAY
            sndkmstate -> SRTO_SNDKMSTATE
            srtlaPatches -> SRTO_SRTLAPATCHES
        }

    val binding: Binding
        get() = when (this) {
            rcvsyn -> Binding.pre
            maxbw -> Binding.post
            pbkeylen -> Binding.pre
            passphrase -> Binding.pre
            mss -> Binding.pre
            fc -> Binding.pre
            sndbuf -> Binding.pre
            rcvbuf -> Binding.pre
            ipttl -> Binding.pre
            iptos -> Binding.pre
            inputbw -> Binding.post
            oheadbw -> Binding.post
            tsbdmode -> Binding.pre
            latency -> Binding.pre
            tlpktdrop -> Binding.pre
            nakreport -> Binding.pre
            conntimeo -> Binding.pre
            lossmaxttl -> Binding.pre
            rcvlatency -> Binding.pre
            peerlatency -> Binding.pre
            minversion -> Binding.pre
            streamid -> Binding.pre
            messageapi -> Binding.pre
            payloadsize -> Binding.pre
            transtype -> Binding.pre
            kmrefreshrate -> Binding.pre
            kmpreannounce -> Binding.pre
            maxrexmitbw -> Binding.post
            sndsyn -> Binding.post
            isn -> Binding.post
            linger -> Binding.post
            udpsndbuf -> Binding.pre
            udprcvbuf -> Binding.pre
            rendezvous -> Binding.pre
            sndtimeo -> Binding.post
            rcvtimeo -> Binding.post
            reuseaddr -> Binding.post
            state -> Binding.post
            event -> Binding.post
            snddata -> Binding.post
            rcvdata -> Binding.post
            sender -> Binding.post
            kmstate -> Binding.post
            snddropdelay -> Binding.post
            sndkmstate -> Binding.post
            srtlaPatches -> Binding.pre
        }

    val type: Type
        get() = when (this) {
            tsbdmode -> Type.bool
            rcvsyn -> Type.bool
            maxbw -> Type.int64
            pbkeylen -> Type.int
            passphrase -> Type.string
            mss -> Type.int
            fc -> Type.int
            sndbuf -> Type.int
            rcvbuf -> Type.int
            ipttl -> Type.int
            iptos -> Type.int
            inputbw -> Type.int64
            oheadbw -> Type.int
            latency -> Type.int
            tlpktdrop -> Type.bool
            nakreport -> Type.bool
            conntimeo -> Type.int
            lossmaxttl -> Type.int
            rcvlatency -> Type.int
            peerlatency -> Type.int
            minversion -> Type.int
            streamid -> Type.string
            messageapi -> Type.bool
            payloadsize -> Type.int
            transtype -> Type.enumeration
            kmrefreshrate -> Type.int
            kmpreannounce -> Type.int
            maxrexmitbw -> Type.int64
            sndsyn -> Type.bool
            isn -> Type.int
            linger -> Type.int
            udpsndbuf -> Type.int
            udprcvbuf -> Type.int
            rendezvous -> Type.bool
            sndtimeo -> Type.int
            rcvtimeo -> Type.int
            reuseaddr -> Type.bool
            state -> Type.int
            event -> Type.int
            snddata -> Type.int
            rcvdata -> Type.int
            sender -> Type.int
            kmstate -> Type.int
            snddropdelay -> Type.int
            sndkmstate -> Type.int
            srtlaPatches -> Type.bool
        }

    val valmap: Map<String, Int>?
        get() = when (this) {
            transtype -> enummapTranstype
            else -> null
        }

    fun setOption(socket: Int, value: String): Boolean {
        val data = this.data(value) ?: return false
        val result: Int = SrtNative.srt_setsockopt(socket, 0, symbol, data, data.size)
        return result != -1
    }

    fun data(value: String): ByteArray? {
        return when (type) {
            Type.string -> value.toByteArray(Charsets.UTF_8)
            Type.int -> {
                val v = value.toIntOrNull() ?: return null
                v.toSrtBytes()
            }
            Type.int64 -> {
                val v = value.toLongOrNull() ?: return null
                v.toSrtBytes()
            }
            Type.bool -> {
                val v = value.toIntOrNull() ?: return null
                (if (v != 0) 1 else 0).toSrtBytes()
            }
            Type.enumeration -> when (this) {
                transtype -> {
                    val v = valmap?.get(value) ?: return null
                    v.toSrtBytes()
                }
                else -> null
            }
        }
    }

    companion object {
        fun fromRawValue(rawValue: String): SrtSocketOption? =
            SrtSocketOption.entries.firstOrNull { it.rawValue == rawValue }

        fun from(uri: String?): Map<SrtSocketOption, String> {
            if (uri == null) {
                return emptyMap()
            }
            val queryItems = getQueryItems(uri)
            val options = mutableMapOf<SrtSocketOption, String>()
            for ((key, value) in queryItems) {
                val option = fromRawValue(key)
                if (option == null) {
                    Log.i(TAG, "Unknown option: $key")
                    continue
                }
                options[option] = value
            }
            return options
        }

        fun configure(socket: Int,
                      binding: Binding,
                      options: Map<SrtSocketOption, String>): List<String>
        {
            val failures = mutableListOf<String>()
            for ((key, value) in options) {
                if (key.binding == binding) {
                    if (!key.setOption(socket, value)) {
                        failures.add(key.rawValue)
                    }
                }
            }
            return failures
        }

        fun getQueryItems(uri: String): Map<String, String> {
            val urlComponent = Uri.parse(uri)
            val params = mutableMapOf<String, String>()
            for (name in urlComponent.queryParameterNames) {
                params[name] = urlComponent.getQueryParameter(name) ?: ""
            }
            return params
        }
    }
}
