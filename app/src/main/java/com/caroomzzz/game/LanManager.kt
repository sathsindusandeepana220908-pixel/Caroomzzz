package com.caroomzzz.game

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

class LanManager(
    private val status: (String) -> Unit,
    private val message: (String) -> Unit
) {
    private val pool = Executors.newCachedThreadPool()
    @Volatile private var socket: Socket? = null
    @Volatile private var server: ServerSocket? = null
    @Volatile private var writer: BufferedWriter? = null
    @Volatile private var running = false

    fun host() {
        close()
        running = true
        pool.execute {
            try {
                val listening = ServerSocket(47891)
                server = listening
                status("HOST READY • IP: ${localIp()} • Port 47891")
                pool.execute {
                    val discovery = try { DatagramSocket(47892, InetAddress.getByName("0.0.0.0")) }
                        catch (_: Exception) { null }
                    if (discovery != null) {
                        discovery.broadcast = true
                        discovery.soTimeout = 1000
                        val buffer = ByteArray(256)
                        while (running) {
                            try {
                                val packet = DatagramPacket(buffer, buffer.size)
                                discovery.receive(packet)
                                val request = String(packet.data, packet.offset, packet.length).trim()
                                if (request == "CAROOMZZZ_DISCOVER") {
                                    val replyBytes = "CAROOMZZZ_HOST".toByteArray()
                                    discovery.send(DatagramPacket(replyBytes, replyBytes.size, packet.address, packet.port))
                                }
                            } catch (_: java.net.SocketTimeoutException) {
                                // Recheck running flag.
                            } catch (_: Exception) {
                                if (running) status("LAN discovery error")
                            }
                        }
                        discovery.close()
                    }
                }
                while (running) {
                    val client = listening.accept()
                    socket = client
                    writer = BufferedWriter(OutputStreamWriter(client.getOutputStream(), Charsets.UTF_8))
                    status("Player connected")
                    pool.execute { listen(client) }
                }
                listening.close()
            } catch (e: Exception) {
                if (running) status("Host error: ${e.message}")
            }
        }
    }

    fun join(ip: String) {
        running = true
        pool.execute {
            try {
                val client = Socket()
                client.tcpNoDelay = true
                client.connect(InetSocketAddress(ip.trim(), 47891), 7000)
                socket = client
                writer = BufferedWriter(OutputStreamWriter(client.getOutputStream(), Charsets.UTF_8))
                status("CONNECTED TO HOST • Waiting for board")
                listen(client)
            } catch (e: Exception) {
                if (running) status("JOIN FAILED • ${e.message}")
            }
        }
    }

    fun discover(found: (String) -> Unit) {
        pool.execute {
            try {
                DatagramSocket().use { discovery ->
                    discovery.broadcast = true
                    discovery.soTimeout = 2500
                    val data = "CAROOMZZZ_DISCOVER".toByteArray()
                    discovery.send(DatagramPacket(data, data.size,
                        InetAddress.getByName("255.255.255.255"), 47892))
                    val buffer = ByteArray(128)
                    val packet = DatagramPacket(buffer, buffer.size)
                    discovery.receive(packet)
                    if (String(packet.data, packet.offset, packet.length).trim() == "CAROOMZZZ_HOST") {
                        found(packet.address.hostAddress ?: "")
                    }
                }
            } catch (_: Exception) {
                status("HOST NOT FOUND • Enter host IP manually")
            }
        }
    }

    private fun listen(client: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
            while (running && !client.isClosed) {
                val line = reader.readLine() ?: break
                message(line)
            }
        } catch (_: Exception) {
            // Socket closed or connection lost.
        } finally {
            if (running) status("CONNECTION LOST")
        }
    }

    fun send(line: String) {
        val out = writer ?: return
        try {
            synchronized(out) {
                out.write(line)
                out.newLine()
                out.flush()
            }
        } catch (_: Exception) {
            status("SEND FAILED • Connection lost")
        }
    }

    fun close() {
        running = false
        try { socket?.close() } catch (_: Exception) {}
        try { server?.close() } catch (_: Exception) {}
        socket = null
        server = null
        writer = null
    }

    private fun localIp(): String = try {
        val interfaces = NetworkInterface.getNetworkInterfaces().toList()
        val addresses = interfaces
            .filter { it.isUp && !it.isLoopback }
            .sortedBy { iface ->
                val name = iface.name.lowercase()
                if (name.contains("wlan") || name.contains("ap") || name.contains("wifi")) 0 else 1
            }
            .flatMap { it.inetAddresses.toList() }
        (addresses.firstOrNull { it is Inet4Address && !it.isLoopbackAddress && it.isSiteLocalAddress }
            as? Inet4Address)?.hostAddress ?: "192.168.43.1"
    } catch (_: Exception) {
        "192.168.43.1"
    }
}
