package com.moblin.android.media.haishinkit.srt

import android.net.Uri
import com.moblin.android.platform.log.Log
import com.moblin.android.platform.srt.SrtNative
import java.net.URI
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val TAG = "SrtSocketOption"

private val enummapTranstype: Map<String, Any> = mapOf(
    "live" to SrtNative.SRTT_LIVE,
    "file" to SrtNative.SRTT_FILE,
)

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
        enumeration(4);

        companion object {
            fun fromRawValue(rawValue: Int): Type? = entries.firstOrNull { it.rawValue == rawValue }
        }
    }

    enum class Binding(val rawValue: Int) {
        pre(0),
        post(1);

        companion object {
            fun fromRawValue(rawValue: Int): Binding? = entries.firstOrNull { it.rawValue == rawValue }
        }
    }

    private val symbol: Int
        get() = when (this) {
            rcvsyn -> SrtNative.SRTO_RCVSYN
            maxbw -> SrtNative.SRTO_MAXBW
            pbkeylen -> SrtNative.SRTO_PBKEYLEN
            passphrase -> SrtNative.SRTO_PASSPHRASE
            mss -> SrtNative.SRTO_MSS
            fc -> SrtNative.SRTO_FC
            sndbuf -> SrtNative.SRTO_SNDBUF
            rcvbuf -> SrtNative.SRTO_RCVBUF
            ipttl -> SrtNative.SRTO_IPTTL
            iptos -> SrtNative.SRTO_IPTOS
            inputbw -> SrtNative.SRTO_INPUTBW
            oheadbw -> SrtNative.SRTO_OHEADBW
            latency -> SrtNative.SRTO_LATENCY
            tsbdmode -> SrtNative.SRTO_TSBPDMODE
            tlpktdrop -> SrtNative.SRTO_TLPKTDROP
            nakreport -> SrtNative.SRTO_NAKREPORT
            conntimeo -> SrtNative.SRTO_CONNTIMEO
            lossmaxttl -> SrtNative.SRTO_LOSSMAXTTL
            rcvlatency -> SrtNative.SRTO_RCVLATENCY
            peerlatency -> SrtNative.SRTO_PEERLATENCY
            minversion -> SrtNative.SRTO_MINVERSION
            streamid -> SrtNative.SRTO_STREAMID
            messageapi -> SrtNative.SRTO_MESSAGEAPI
            payloadsize -> SrtNative.SRTO_PAYLOADSIZE
            transtype -> SrtNative.SRTO_TRANSTYPE
            kmrefreshrate -> SrtNative.SRTO_KMREFRESHRATE
            kmpreannounce -> SrtNative.SRTO_KMPREANNOUNCE
            maxrexmitbw -> SrtNative.SRTO_MAXREXMITBW
            sndsyn -> SrtNative.SRTO_SNDSYN
            isn -> SrtNative.SRTO_ISN
            linger -> SrtNative.SRTO_LINGER
            udpsndbuf -> SrtNative.SRTO_UDP_SNDBUF
            udprcvbuf -> SrtNative.SRTO_UDP_RCVBUF
            rendezvous -> SrtNative.SRTO_RENDEZVOUS
            sndtimeo -> SrtNative.SRTO_SNDTIMEO
            rcvtimeo -> SrtNative.SRTO_RCVTIMEO
            reuseaddr -> SrtNative.SRTO_REUSEADDR
            state -> SrtNative.SRTO_STATE
            event -> SrtNative.SRTO_EVENT
            snddata -> SrtNative.SRTO_SNDDATA
            rcvdata -> SrtNative.SRTO_RCVDATA
            sender -> SrtNative.SRTO_SENDER
            kmstate -> SrtNative.SRTO_KMSTATE
            snddropdelay -> 32
            sndkmstate -> SrtNative.SRTO_SNDKMSTATE
            srtlaPatches -> SrtNative.SRTO_SRTLAPATCHES
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

    val valmap: Map<String, Any>?
        get() = when (this) {
            transtype -> enummapTranstype
            else -> null
        }

    fun setOption(socket: Int, value: String): Boolean {
        val data = data(value) ?: return false
        val result: Int = SrtNative.srt_setsockopt(socket, 0, symbol, data, data.size)
        return result != -1
    }

    fun data(value: String): ByteArray? {
        return when (type) {
            Type.string -> value.toByteArray(Charsets.UTF_8)
            Type.int -> {
                val intValue = value.toIntOrNull() ?: return null
                ByteBuffer.allocate(Int.SIZE_BYTES).order(ByteOrder.nativeOrder()).putInt(intValue).array()
            }
            Type.int64 -> {
                val longValue = value.toLongOrNull() ?: return null
                ByteBuffer.allocate(Long.SIZE_BYTES).order(ByteOrder.nativeOrder()).putLong(longValue).array()
            }
            Type.bool -> {
                var boolValue = value.toIntOrNull() ?: return null
                boolValue = if (boolValue != 0) 1 else 0
                ByteBuffer.allocate(Int.SIZE_BYTES).order(ByteOrder.nativeOrder()).putInt(boolValue).array()
            }
            Type.enumeration -> when (this) {
                transtype -> {
                    val key = value
                    val v = valmap?.get(key) as? Int ?: return null
                    ByteBuffer.allocate(Int.SIZE_BYTES).order(ByteOrder.nativeOrder()).putInt(v).array()
                }
                else -> null
            }
        }
    }

    companion object {
        fun fromRawValue(rawValue: String): SrtSocketOption? = entries.firstOrNull { it.rawValue == rawValue }

        fun from(uri: URI?): Map<SrtSocketOption, String> {
            if (uri == null) {
                return emptyMap()
            }
            val queryItems = getQueryItems(uri = uri)
            val options = mutableMapOf<SrtSocketOption, String>()
            for (item in queryItems) {
                val option = fromRawValue(item.key)
                if (option == null) {
                    Log.i(TAG, "Unknown option: ${item.key}")
                    continue
                }
                options[option] = item.value
            }
            return options
        }

        fun configure(
            socket: Int,
            binding: Binding,
            options: Map<SrtSocketOption, String>,
        ): List<String> {
            val failures = mutableListOf<String>()
            for ((key, value) in options) {
                if (key.binding != binding) {
                    continue
                }
                if (!key.setOption(socket, value = value)) {
                    failures.add(key.rawValue)
                }
            }
            return failures
        }

        fun getQueryItems(uri: URI): Map<String, String> {
            val query = uri.rawQuery ?: return emptyMap()
            val params = mutableMapOf<String, String>()
            for (item in query.split("&")) {
                if (item.isEmpty()) {
                    continue
                }
                val separator = item.indexOf('=')
                val name: String = Uri.decode(if (separator < 0) item else item.substring(0, separator)) ?: ""
                val value: String? = if (separator < 0) null else Uri.decode(item.substring(separator + 1))
                params[name] = value?.let { Uri.decode(it) } ?: ""
            }
            return params
        }
    }
}
