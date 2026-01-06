package com.example.lockinapp.vpn

data class TcpPacket(
    val dstPort: Int,
    val payloadOffset: Int,
    val payloadLen: Int
) {
    companion object {
        fun parse(packet: ByteArray, length: Int): TcpPacket? {
            if (length < 40) return null
            val vihl = packet[0].toInt() and 0xFF
            val ihl = (vihl and 0x0F) * 4
            val proto = packet[9].toInt() and 0xFF
            if (proto != 6) return null

            val tcpOffset = ihl
            val dstPort = ((packet[tcpOffset + 2].toInt() and 0xFF) shl 8) or (packet[tcpOffset + 3].toInt() and 0xFF)

            val dataOffset = ((packet[tcpOffset + 12].toInt() shr 4) and 0xF) * 4
            val payloadOffset = tcpOffset + dataOffset
            if (payloadOffset >= length) return null

            return TcpPacket(
                dstPort = dstPort,
                payloadOffset = payloadOffset,
                payloadLen = length - payloadOffset
            )
        }
    }
}
