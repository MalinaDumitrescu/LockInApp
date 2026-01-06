package com.example.lockinapp.vpn

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.core.content.ContextCompat
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class DnsBlockVpnService : VpnService() {

    @Volatile private var running = false
    private var worker: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (running) return Service.START_STICKY
        
        startForeground(1, VpnNotification.build(this))
        running = true
        
        worker = Thread {
            try {
                runVpn()
            } catch (e: Exception) {
                running = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        worker?.start()
        return Service.START_STICKY
    }

    override fun onDestroy() {
        running = false
        worker?.interrupt()
        worker = null
        super.onDestroy()
    }

    private fun runVpn() {
        val builder = Builder()
            .setSession("LockInApp DNS Blocker")
            .addAddress("10.0.0.2", 32)
            .addDnsServer("1.1.1.1")
            .addDnsServer("8.8.8.8")
            .addRoute("1.1.1.1", 32)
            .addRoute("8.8.8.8", 32)

        val tun = builder.establish() ?: return

        val input = FileInputStream(tun.fileDescriptor)
        val output = FileOutputStream(tun.fileDescriptor)

        val buffer = ByteArray(32767)

        val socket = DatagramSocket()
        protect(socket)

        try {
            while (running) {
                val len = try {
                    input.read(buffer)
                } catch (e: Exception) {
                    -1
                }
                if (len <= 0) break

                val udp = Packet.parseUdpIpv4(buffer, len)
                if (udp != null) {
                    if (udp.dstPort != 53) continue

                    val q = Dns.parseQuery(buffer, udp.payloadOffset, udp.payloadLen) ?: continue
                    val domain = q.qname

                    if (DomainBlocker.isBlockedSite(domain)) {
                        val respDns = Dns.buildNxdomainResponse(buffer, udp.payloadOffset, udp.payloadLen, q)
                        val outPkt = buffer.copyOf(len)

                        Packet.swapSrcDstIpv4AndUdp(outPkt, udp)

                        val newUdpLen = 8 + respDns.size
                        outPkt[udp.udpOffset + 4] = ((newUdpLen ushr 8) and 0xFF).toByte()
                        outPkt[udp.udpOffset + 5] = (newUdpLen and 0xFF).toByte()

                        val totalLen = udp.ipHeaderLen + newUdpLen
                        Packet.setIpv4TotalLength(outPkt, totalLen)

                        System.arraycopy(respDns, 0, outPkt, udp.payloadOffset, respDns.size)

                        Packet.updateIpv4HeaderChecksum(outPkt, udp.ipHeaderLen)

                        output.write(outPkt, 0, totalLen)
                        continue
                    }

                    val dstAddr = intToInet(udp.dstIp)

                    val queryBytes = ByteArray(udp.payloadLen)
                    System.arraycopy(buffer, udp.payloadOffset, queryBytes, 0, udp.payloadLen)

                    socket.soTimeout = 2000
                    try {
                        socket.send(DatagramPacket(queryBytes, queryBytes.size, dstAddr, 53))

                        val respBuf = ByteArray(4096)
                        val recvPacket = DatagramPacket(respBuf, respBuf.size)
                        socket.receive(recvPacket)

                        val respLen = recvPacket.length
                        val outPkt = buffer.copyOf(len)

                        Packet.swapSrcDstIpv4AndUdp(outPkt, udp)

                        val newUdpLen = 8 + respLen
                        outPkt[udp.udpOffset + 4] = ((newUdpLen ushr 8) and 0xFF).toByte()
                        outPkt[udp.udpOffset + 5] = (newUdpLen and 0xFF).toByte()

                        val totalLen = udp.ipHeaderLen + newUdpLen
                        Packet.setIpv4TotalLength(outPkt, totalLen)

                        System.arraycopy(respBuf, 0, outPkt, udp.payloadOffset, respLen)

                        Packet.updateIpv4HeaderChecksum(outPkt, udp.ipHeaderLen)

                        output.write(outPkt, 0, totalLen)
                    } catch (e: Exception) {
                        // ignore timeout/network errors
                    }
                    continue
                }

                val tcp = TcpPacket.parse(buffer, len) ?: continue
                if (tcp.dstPort != 443) continue
                if (tcp.payloadLen <= 0) continue

                val sni = TlsSni.extractSni(buffer, tcp.payloadOffset, tcp.payloadLen) ?: continue
                if (DomainBlocker.isDoHEndpoint(sni)) {
                    continue
                }
            }
        } finally {
            socket.close()
            input.close()
            output.close()
            tun.close()
        }
    }

    private fun intToInet(ip: Int): InetAddress {
        val b1 = (ip ushr 24) and 0xFF
        val b2 = (ip ushr 16) and 0xFF
        val b3 = (ip ushr 8) and 0xFF
        val b4 = ip and 0xFF
        return InetAddress.getByAddress(byteArrayOf(b1.toByte(), b2.toByte(), b3.toByte(), b4.toByte()))
    }

    companion object {
        fun start(ctx: Context) {
            val i = Intent(ctx, DnsBlockVpnService::class.java)
            ContextCompat.startForegroundService(ctx, i)
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, DnsBlockVpnService::class.java))
        }
    }
}
