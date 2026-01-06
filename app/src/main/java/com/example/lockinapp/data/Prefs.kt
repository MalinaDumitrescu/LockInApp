package com.example.lockinapp.data

import android.content.Context

object Prefs {
    private const val FILE = "lockinapp_prefs"

    private const val KEY_BLOCKED_PACKAGES = "blocked_packages"
    private const val KEY_PW_HASH = "pw_hash"
    private const val KEY_PW_SALT = "pw_salt"

    private const val KEY_MODE_PREFIX = "mode_"
    private const val KEY_LOCKED_UNTIL_PREFIX = "locked_until_"
    private const val KEY_START_MIN_PREFIX = "start_min_"
    private const val KEY_END_MIN_PREFIX = "end_min_"
    private const val KEY_UNLOCK_UNTIL_PREFIX = "unlock_until_"

    fun getBlockedPackages(ctx: Context): Set<String> {
        return ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getStringSet(KEY_BLOCKED_PACKAGES, emptySet()) ?: emptySet()
    }

    fun setBlockedPackages(ctx: Context, pkgs: Set<String>) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_BLOCKED_PACKAGES, pkgs)
            .apply()
    }

    fun getPasswordHash(ctx: Context): String? =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_PW_HASH, null)

    fun getPasswordSalt(ctx: Context): String? =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_PW_SALT, null)

    fun setPasswordHashSalt(ctx: Context, hash: String, salt: String) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PW_HASH, hash)
            .putString(KEY_PW_SALT, salt)
            .apply()
    }

    fun setUnlockUntil(ctx: Context, packageName: String, unlockUntilMillis: Long) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_UNLOCK_UNTIL_PREFIX + packageName, unlockUntilMillis)
            .apply()
    }

    fun getUnlockUntil(ctx: Context, packageName: String): Long {
        return ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getLong(KEY_UNLOCK_UNTIL_PREFIX + packageName, 0L)
    }

    fun setMode(ctx: Context, packageName: String, mode: String) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE_PREFIX + packageName, mode)
            .apply()
    }

    fun getMode(ctx: Context, packageName: String): String {
        return ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_MODE_PREFIX + packageName, "INDEFINITE") ?: "INDEFINITE"
    }

    fun setLockedUntil(ctx: Context, packageName: String, untilMillis: Long) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LOCKED_UNTIL_PREFIX + packageName, untilMillis)
            .apply()
    }

    fun getLockedUntil(ctx: Context, packageName: String): Long {
        return ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getLong(KEY_LOCKED_UNTIL_PREFIX + packageName, 0L)
    }

    fun setInterval(ctx: Context, packageName: String, startMinute: Int, endMinute: Int) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_START_MIN_PREFIX + packageName, startMinute)
            .putInt(KEY_END_MIN_PREFIX + packageName, endMinute)
            .apply()
    }

    fun getStartMinute(ctx: Context, packageName: String): Int {
        return ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getInt(KEY_START_MIN_PREFIX + packageName, 22 * 60)
    }

    fun getEndMinute(ctx: Context, packageName: String): Int {
        return ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getInt(KEY_END_MIN_PREFIX + packageName, 7 * 60)
    }
}
