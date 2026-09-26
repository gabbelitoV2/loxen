package com.moblin.android.platform.network

import android.util.Log

class NWProtocolTLS {
    class Options
}

class NWProtocolUDP {
    class Options
}

class NWProtocolTCP {
    class Options {
        var noDelay = false
        var enableKeepalive = false
        var connectionTimeout = 0
    }
}

class NWParameters private constructor(
    internal val isDatagram: Boolean,
    internal val tlsOptions: NWProtocolTLS.Options?,
    internal val tcpOptions: NWProtocolTCP.Options,
) {
    var requiredInterface: NWInterface? = null
    var requiredInterfaceType: NWInterface.InterfaceType = NWInterface.InterfaceType.other
    var requiredLocalEndpoint: NWEndpoint? = null
    var allowLocalEndpointReuse = false
    var acceptLocalOnly = false
    var prohibitExpensivePaths = false

    companion object {
        private const val TAG = "MoblinNet"

        val tcp: NWParameters
            get() = tls(null)

        val tls: NWParameters
            get() = tls(NWProtocolTLS.Options())

        val udp: NWParameters
            get() = dtls(null)

        fun tls(options: NWProtocolTLS.Options?, tcp: NWProtocolTCP.Options = NWProtocolTCP.Options()): NWParameters {
            return NWParameters(isDatagram = false, tlsOptions = options, tcpOptions = tcp)
        }

        fun dtls(options: NWProtocolTLS.Options?, udp: NWProtocolUDP.Options = NWProtocolUDP.Options()): NWParameters {
            if (options != null) {
                Log.i(TAG, "DTLS is not supported, using plain UDP")
            }
            return NWParameters(isDatagram = true, tlsOptions = null, tcpOptions = NWProtocolTCP.Options())
        }
    }
}
