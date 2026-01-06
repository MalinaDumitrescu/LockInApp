package com.example.lockinapp.vpn

import java.nio.ByteBuffer

data class UdpPacket(
    val srcIp: Int,
    val dstIp: Int,
    val srcPort: Int,
    val dstPort: Int,
    val ipHeaderLen: Int,
    val udpOffset: Int,
    val udpLen: Int,
    val payloadOffset: Int,
    val payloadLen: Int
)

object Packet {
    fun parseUdpIpv4(packet: ByteArray, length: Int): UdpPacket? {
        if (length < 20) return null
        val vihl = packet[0].toInt() and 0xFF
        val ver = vihl ushr 4
        if (ver != 4) return null
        val ihl = (vihl and 0x0F) * 4
        if (ihl < 20 || length < ihl + 8) return null

        val proto = packet[9].toInt() and 0xFF
        if (proto != 17) return null

        val srcIp = ByteBuffer.wrap(packet, 12, 4).int
        val dstIp = ByteBuffer.wrap(packet, 16, 4).int

        val udpOffset = ihl
        val srcPort = ((packet[udpOffset].toInt() and 0xFF) shl 8) or (packet[udpOffset + 1].toInt() and 0xFF)
        val dstPort = ((packet[udpOffset + 2].toInt() and 0xFF) shl 8) or (packet[udpOffset + 3].toInt() and 0xFF)
        val udpLen = ((packet[udpOffset + 4].toInt() and 0xFF) shl 8) or (packet[udpOffset + 5].toInt() and 0xFF)
        if (udpLen < 8) return null
        if (udpOffset + udpLen > length) return null

        val payloadOffset = udpOffset + 8
        val payloadLen = udpLen - 8

        return UdpPacket(
            srcIp = srcIp,
            dstIp = dstIp,
            srcPort = srcPort,
            dstPort = dstPort,
            ipHeaderLen = ihl,
            udpOffset = udpOffset,
            udpLen = udpLen,
            payloadOffset = payloadOffset,
            payloadLen = payloadLen
        )
    }

    fun swapSrcDstIpv4AndUdp(packet: ByteArray, udp: UdpPacket) {
        for (i in 0 until 4) {
            val tmp = packet[12 + i]
            packet[12 + i] = packet[16 + i]
            packet[16 + i] = tmp
        }

        val u = udp.udpOffset
        val sp0 = packet[u]
        val sp1 = packet[u + 1]
        packet[u] = packet[u + 2]
        packet[u + 1] = packet[u + 3]
        packet[u + 2] = sp0
        packet[u + 3] = sp1

        packet[u + 6] = 0x00
        packet[u + 7] = 0x00
    }

    fun setIpv4TotalLength(packet: ByteArray, totalLen: Int) {
        packet[2] = ((totalLen ushr 8) and 0xFF).toByte()
        packet[3] = (totalLen and 0xFF).toByte()
    }

    fun updateIpv4HeaderChecksum(packet: ByteArray, ipHeaderLen: Int) {
        packet[10] = 0x00
        packet[11] = 0x00
        val csum = ipv4Checksum(packet, 0, ipHeaderLen)
        packet[10] = ((csum ushr 8) and 0xFF).toByte()
        packet[11] = (csum and 0xFF).toByte()
    }

    private fun ipv4Checksum(buf: ByteArray, offset: Int, len: Int): Int {
        var sum = 0
        var i = 0
        while (i < len) {
            val hi = buf[offset + i].toInt() and 0xFF
            val lo = buf[offset + i + 1].toInt() and 0xFF
            sum += (hi shl 8) or lo
            sum = (sum and 0xFFFF) + (sum ushr 16)
            i += 2
        }
        sum = (sum and 0xFFFF) + (sum ushr 16)
        return sum.inv() and 0xFFFF
    }
}
