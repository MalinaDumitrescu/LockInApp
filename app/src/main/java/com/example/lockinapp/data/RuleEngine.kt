package com.example.lockinapp.data

import android.content.Context
import java.util.Calendar

object RuleEngine {
    fun shouldBlockNow(ctx: Context, packageName: String): Boolean {
        val blocked = Prefs.getBlockedPackages(ctx)
        if (!blocked.contains(packageName)) return false

        val now = System.currentTimeMillis()
        val unlockUntil = Prefs.getUnlockUntil(ctx, packageName)
        if (unlockUntil > now) return false

        return when (Prefs.getMode(ctx, packageName)) {
            "INDEFINITE" -> true
            "DURATION" -> Prefs.getLockedUntil(ctx, packageName) > now
            "INTERVAL" -> isNowInInterval(ctx, packageName)
            else -> true
        }
    }

    fun applyDurationMinutes(ctx: Context, packageName: String, minutes: Int) {
        val until = System.currentTimeMillis() + minutes * 60_000L
        Prefs.setLockedUntil(ctx, packageName, until)
        Prefs.setMode(ctx, packageName, "DURATION")
    }

    private fun isNowInInterval(ctx: Context, packageName: String): Boolean {
        val cal = Calendar.getInstance()
        val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val start = Prefs.getStartMinute(ctx, packageName)
        val end = Prefs.getEndMinute(ctx, packageName)

        return if (start == end) {
            true
        } else if (start < end) {
            nowMin in start until end
        } else {
            nowMin >= start || nowMin < end
        }
    }
}
