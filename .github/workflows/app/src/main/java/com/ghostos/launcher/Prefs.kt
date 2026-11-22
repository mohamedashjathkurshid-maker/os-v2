package com.ghostos.launcher

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

object Prefs {
    private var sp: SharedPreferences? = null

    fun init(ctx: Context) {
        if (sp == null) {
            sp = ctx.applicationContext.getSharedPreferences("ghostos", Context.MODE_PRIVATE)
        }
    }

    private fun p(): SharedPreferences = sp ?: throw IllegalStateException("Prefs not initialised")
    private fun bool(k: String, d: Boolean): Boolean = p().getBoolean(k, d)
    private fun putBool(k: String, v: Boolean) { p().edit().putBoolean(k, v).apply() }
    private fun str(k: String, d: String): String = p().getString(k, d) ?: d
    private fun putStr(k: String, v: String) { p().edit().putString(k, v).apply() }

    var theme: String
        get() = str("theme", "green")
        set(v) { putStr("theme", v) }

    var rain: Boolean
        get() = bool("rain", true)
        set(v) { putBool("rain", v) }

    var boot: Boolean
        get() = bool("boot", true)
        set(v) { putBool("boot", v) }

    var sound: Boolean
        get() = bool("sound", false)
        set(v) { putBool("sound", v) }

    var shake: Boolean
        get() = bool("shake", true)
        set(v) { putBool("shake", v) }

    var dtLock: Boolean
        get() = bool("dtlock", true)
        set(v) { putBool("dtlock", v) }

    var pinnedInit: Boolean
        get() = bool("pinnedInit", false)
        set(v) { putBool("pinnedInit", v) }

    // off | counts | full
    var ticker: String
        get() = str("ticker", "full")
        set(v) { putStr("ticker", v) }

    // mono | text | color
    var icons: String
        get() = str("icons", "mono")
        set(v) { putStr("icons", v) }

    var vaultHash: String
        get() = str("vaultHash", "")
        set(v) { putStr("vaultHash", v) }

    var hidden: Set<String>
        get() = str("hidden", "").split(",").filter { it.isNotEmpty() }.toSet()
        set(v) { putStr("hidden", v.joinToString(",")) }

    var pinned: List<String>
        get() = str("pinned", "").split(",").filter { it.isNotEmpty() }
        set(v) { putStr("pinned", v.joinToString(",")) }
}

object Vault {
    fun hash(pin: String): String {
        val d = MessageDigest.getInstance("SHA-256").digest(("ghostos:" + pin).toByteArray())
        return d.joinToString("") { "%02x".format(it) }
    }

    fun isSet(): Boolean = Prefs.vaultHash.isNotEmpty()
}
