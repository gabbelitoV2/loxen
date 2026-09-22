package com.moblin.android.various.network

import android.system.OsConstants
import android.util.Log
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

private const val tag = "DnsLookup"

enum class DnsLookupFamily {
    ipv4,
    ipv6,
    unspec;

    fun toCFamily(): Int {
        return when (this) {
            DnsLookupFamily.ipv4 -> OsConstants.AF_INET
            DnsLookupFamily.ipv6 -> OsConstants.AF_INET6
            DnsLookupFamily.unspec -> OsConstants.AF_UNSPEC
        }
    }
}

fun performDnsLookup(host: String, family: DnsLookupFamily): String? {
    val allAddresses = try {
        InetAddress.getAllByName(host)
    } catch (e: Exception) {
        Log.i(tag, "dns: Lookup of $host failed with ${e.message}")
        return null
    }
    val matchedAddresses = allAddresses.filter { address ->
        when (family) {
            DnsLookupFamily.ipv4 -> address is Inet4Address
            DnsLookupFamily.ipv6 -> address is Inet6Address
            DnsLookupFamily.unspec -> true
        }
    }
    val addresses = matchedAddresses.mapNotNull { it.hostAddress }
    for (address in addresses) {
        Log.i(tag, "dns: Found address $address for $host")
    }
    return addresses.firstOrNull()
}
