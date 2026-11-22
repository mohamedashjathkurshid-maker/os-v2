package com.ghostos.launcher

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock

object SystemStats {
    class Snap(
        val batPct: Int,
        val charging: Boolean,
        val tempC: Float,
        val ramPct: Int,
        val ramUsedMb: Long,
        val ramTotalMb: Long,
        val stoPct: Int,
        val stoFreeGb: Float,
        val net: String,
        val uptime: String
    )

    fun read(ctx: Context): Snap {
        val bi = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = bi?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = bi?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val bat = if (level >= 0 && scale > 0) level * 100 / scale else 0
        val st = bi?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
        val temp = (bi?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f

        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val ramPct = if (mi.totalMem > 0) ((mi.totalMem - mi.availMem) * 100 / mi.totalMem).toInt() else 0

        val sf = StatFs(Environment.getDataDirectory().path)
        val tot = sf.totalBytes
        val free = sf.availableBytes
        val stoPct = if (tot > 0) ((tot - free) * 100 / tot).toInt() else 0

        val secs = SystemClock.elapsedRealtime() / 1000
        val up = "" + (secs / 3600) + "h" + ((secs % 3600) / 60) + "m"

        return Snap(
            bat, charging, temp, ramPct,
            (mi.totalMem - mi.availMem) / 1048576L, mi.totalMem / 1048576L,
            stoPct, free / 1073741824f, netString(ctx), up
        )
    }

    fun netString(ctx: Context): String {
        return try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            when {
                caps == null -> "offline"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    @Suppress("DEPRECATION")
                    val rssi = wm.connectionInfo?.rssi ?: -127
                    if (rssi > -127) "wifi " + rssi + "dBm" else "wifi"
                }
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "mobile data"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
                else -> "online"
            }
        } catch (e: Exception) {
            "n/a"
        }
    }
}
