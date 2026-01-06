package com.example.lockinapp.vpn

object DomainBlocker {

    private val blockedSites = setOf(
        "instagram.com",
        "www.instagram.com",
        "facebook.com",
        "www.facebook.com",
        "m.facebook.com",
        "youtube.com",
        "www.youtube.com",
        "m.youtube.com",
        "youtu.be",
        "googlevideo.com",
        "ytimg.com",
        "fbcdn.net",
        "cdninstagram.com"
    )

    private val dohEndpoints = setOf(
        "dns.google",
        "cloudflare-dns.com",
        "mozilla.cloudflare-dns.com",
        "dns.quad9.net",
        "dns.adguard.com",
        "family.cloudflare-dns.com",
        "security.cloudflare-dns.com"
    )

    fun isBlockedSite(domain: String): Boolean {
        val d = domain.lowercase().trimEnd('.')
        if (blockedSites.contains(d)) return true
        val parts = d.split(".")
        if (parts.size >= 2) {
            val apex = parts.takeLast(2).joinToString(".")
            if (blockedSites.contains(apex)) return true
        }
        return false
    }

    fun isDoHEndpoint(domain: String): Boolean {
        val d = domain.lowercase().trimEnd('.')
        if (dohEndpoints.contains(d)) return true
        val parts = d.split(".")
        if (parts.size >= 2) {
            val apex = parts.takeLast(2).joinToString(".")
            if (dohEndpoints.contains(apex)) return true
        }
        return false
    }
}
