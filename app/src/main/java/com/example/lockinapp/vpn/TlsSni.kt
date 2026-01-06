package com.example.lockinapp.vpn

object TlsSni {

    fun extractSni(payload: ByteArray, offset: Int, length: Int): String? {
        if (length < 5) return null
        if (payload[offset] != 0x16.toByte()) return null

        var pos = offset + 5
        if (pos + 1 >= offset + length) return null
        if (payload[pos] != 0x01.toByte()) return null

        pos += 38
        if (pos >= offset + length) return null

        val sessionIdLen = payload[pos].toInt() and 0xFF
        pos += 1 + sessionIdLen
        if (pos + 2 > offset + length) return null

        val cipherLen = ((payload[pos].toInt() and 0xFF) shl 8) or (payload[pos + 1].toInt() and 0xFF)
        pos += 2 + cipherLen
        if (pos >= offset + length) return null

        val compLen = payload[pos].toInt() and 0xFF
        pos += 1 + compLen
        if (pos + 2 > offset + length) return null

        val extLen = ((payload[pos].toInt() and 0xFF) shl 8) or (payload[pos + 1].toInt() and 0xFF)
        pos += 2
        val extEnd = pos + extLen

        while (pos + 4 <= extEnd) {
            val type = ((payload[pos].toInt() and 0xFF) shl 8) or (payload[pos + 1].toInt() and 0xFF)
            val len = ((payload[pos + 2].toInt() and 0xFF) shl 8) or (payload[pos + 3].toInt() and 0xFF)
            pos += 4

            if (type == 0x0000) {
                pos += 2
                val nameLen = ((payload[pos].toInt() and 0xFF) shl 8) or (payload[pos + 1].toInt() and 0xFF)
                pos += 2
                if (payload[pos] != 0x00.toByte()) return null
                pos += 1
                val hostLen = ((payload[pos].toInt() and 0xFF) shl 8) or (payload[pos + 1].toInt() and 0xFF)
                pos += 2
                return String(payload, pos, hostLen)
            }
            pos += len
        }
        return null
    }
}
