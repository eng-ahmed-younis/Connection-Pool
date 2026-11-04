package com.example.pool.data.network

import okhttp3.Call
import okhttp3.Connection
import okhttp3.EventListener
import okhttp3.Protocol
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.ConcurrentHashMap

data class CallTimingInfo(
    var dnsDurationMs: Long = 0,
    var connectDurationMs: Long = 0,
    var isConnectionReused: Boolean = false,
    var connectionCreated: Boolean = false
)

class ConnectionMetricsEventListener : EventListener() {

    private var dnsStartNs: Long = 0
    private var connectStartNs: Long = 0
    val timing = CallTimingInfo()

    override fun callStart(call: Call) {
        // Reset timing
    }

    override fun dnsStart(call: Call, domainName: String) {
        dnsStartNs = System.nanoTime()
    }

    override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<InetAddress>) {
        if (dnsStartNs > 0) {
            timing.dnsDurationMs = (System.nanoTime() - dnsStartNs) / 1_000_000
        }
    }

    override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
        connectStartNs = System.nanoTime()
        timing.connectionCreated = true
    }

    override fun connectEnd(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy,
        protocol: Protocol?
    ) {
        if (connectStartNs > 0) {
            timing.connectDurationMs = (System.nanoTime() - connectStartNs) / 1_000_000
        }
    }

    override fun connectionAcquired(call: Call, connection: Connection) {
        // If connectionAcquired is called without connectionCreated being true, connection was reused!
        timing.isConnectionReused = !timing.connectionCreated
    }
}
