package com.example.lockinapp.vpn

import java.nio.ByteBuffer
import kotlin.math.min

data class DnsQuery(
    val id: Int,
    val qname: String,
    val qtype: Int,
    val qclass: Int,
    val questionEndOffset: Int
)

object Dns {
    fun parseQuery(udpPayload: ByteArray, offset: Int, length: Int): DnsQuery? {
        if (length < 12) return null
        val buf = ByteBuffer.wrap(udpPayload, offset, length)

        val id = buf.short.toInt() and 0xFFFF
        val flags = buf.short.toInt() and 0xFFFF
        val qd = buf.short.toInt() and 0xFFFF
        buf.short; buf.short; buf.short

        val isQuery = (flags and 0x8000) == 0
        if (!isQuery) return null
        if (qd != 1) return null

        var idx = 12
        val name = StringBuilder()
        while (idx < length) {
            val len = udpPayload[offset + idx].toInt() and 0xFF
            idx += 1
            if (len == 0) break
            if (len > 63) return null
            if (idx + len > length) return null
            if (name.isNotEmpty()) name.append('.')
            name.append(String(udpPayload, offset + idx, len))
            idx += len
        }
        if (idx + 4 > length) return null
        val qtype = ((udpPayload[offset + idx].toInt() and 0xFF) shl 8) or (udpPayload[offset + idx + 1].toInt() and 0xFF)
        val qclass = ((udpPayload[offset + idx + 2].toInt() and 0xFF) shl 8) or (udpPayload[offset + idx + 3].toInt() and 0xFF)
        idx += 4

        return DnsQuery(
            id = id,
            qname = name.toString(),
            qtype = qtype,
            qclass = qclass,
            questionEndOffset = idx
        )
    }

    fun buildNxdomainResponse(originalQuery: ByteArray, offset: Int, length: Int, q: DnsQuery): ByteArray {
        val out = ByteArray(min(length, 512))
        System.arraycopy(originalQuery, offset, out, 0, min(length, out.size))

        out[2] = (0x81).toByte()
        out[3] = (0x83).toByte()

        out[6] = 0x00
        out[7] = 0x00
        out[8] = 0x00
        out[9] = 0x00
        out[10] = 0x00
        out[11] = 0x00

        return out
    }
}
