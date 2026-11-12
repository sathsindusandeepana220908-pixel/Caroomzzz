package com.caroomzzz.game

import java.io.*
import java.net.*
import java.util.concurrent.Executors

class LanManager(private val status:(String)->Unit, private val message:(String)->Unit) {
    private val pool=Executors.newCachedThreadPool()
    @Volatile private var socket: Socket?=null
    @Volatile private var writer: PrintWriter?=null
    @Volatile private var running=false
    fun host() {
        running=true
        pool.execute {
            try {
                val server=ServerSocket(47891)
                status("Hosting on ${localIp()} • waiting for player")
                pool.execute {
                    while(running) try {
                        val packet=DatagramPacket(ByteArray(256),256)
                        val ds=DatagramSocket(47892, InetAddress.getByName("0.0.0.0"))
                        ds.broadcast=true; ds.soTimeout=1500
                        try { ds.receive(packet); if(String(packet.data,0,packet.length).trim()=="CAROOMZZZ_DISCOVER") {
                            val reply=DatagramPacket("CAROOMZZZ_HOST".toByteArray(),13,packet.address,packet.port); ds.send(reply)
                        }} finally { ds.close() }
                    } catch (_:Exception) {}
                }
                while(running) {
                    val s=server.accept(); socket=s; writer=PrintWriter(BufferedWriter(OutputStreamWriter(s.getOutputStream())),true)
                    status("Player connected")
                    pool.execute { listen(s) }
                }
                server.close()
            } catch(e:Exception) { status("Host error: ${e.message}") }
        }
    }
    fun join(ip:String) {
        running=true
        pool.execute {
            try {
                val s=Socket(); s.connect(InetSocketAddress(ip.trim(),47891),5000); socket=s
                writer=PrintWriter(BufferedWriter(OutputStreamWriter(s.getOutputStream())),true)
                status("Connected to host")
                listen(s)
            } catch(e:Exception) { status("Could not join: ${e.message}") }
        }
    }
    fun discover(found:(String)->Unit) {
        pool.execute {
            try {
                val ds=DatagramSocket(); ds.broadcast=true; ds.soTimeout=1800
                val data="CAROOMZZZ_DISCOVER".toByteArray()
                ds.send(DatagramPacket(data,data.size,InetAddress.getByName("255.255.255.255"),47892))
                val buf=ByteArray(128); val p=DatagramPacket(buf,buf.size); ds.receive(p)
                found(p.address.hostAddress ?: ""); ds.close()
            } catch(_:Exception) { status("No host found. Enter host IP manually.") }
        }
    }
    private fun listen(s:Socket) {
        try { val r=BufferedReader(InputStreamReader(s.getInputStream())); while(running) { val line=r.readLine()?:break; message(line) } }
        catch(_:Exception) {} finally { status("Connection closed") }
    }
    fun send(line:String) { val out=writer ?: return; try { synchronized(out) { out.println(line) } } catch(_:Exception){} }
    fun close() { running=false; try { socket?.close() } catch(_:Exception){}; writer=null }
    private fun localIp():String = try { NetworkInterface.getNetworkInterfaces().toList().flatMap { it.inetAddresses.toList() }.first { !it.isLoopbackAddress && it is Inet4Address }.hostAddress ?: "192.168.43.1" } catch(_:Exception) { "192.168.43.1" }
}
