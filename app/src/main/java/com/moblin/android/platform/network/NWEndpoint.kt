package com.moblin.android.platform.network

class NWEndpoint internal constructor(val host: Host, val port: Port) {
    class Host(val value: String) {
        override fun equals(other: Any?): Boolean {
            return other is Host && other.value == value
        }

        override fun hashCode(): Int {
            return value.hashCode()
        }

        override fun toString(): String {
            return value
        }
    }

    class Port(val value: Int) {
        val rawValue: UShort
            get() = value.toUShort()

        override fun equals(other: Any?): Boolean {
            return other is Port && other.value == value
        }

        override fun hashCode(): Int {
            return value
        }

        override fun toString(): String {
            return value.toString()
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is NWEndpoint && other.host == host && other.port == port
    }

    override fun hashCode(): Int {
        return host.hashCode() * 31 + port.hashCode()
    }

    override fun toString(): String {
        return if (host.value.contains(':')) {
            "[$host]:$port"
        } else {
            "$host:$port"
        }
    }

    companion object {
        fun hostPort(host: Host, port: Port): NWEndpoint {
            return NWEndpoint(host, port)
        }
    }
}
