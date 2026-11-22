package com.ghostos.launcher

import android.graphics.Color

object Palette {
    var bg: Int = Color.parseColor("#020602")
    var fg: Int = Color.parseColor("#00FF41")
    var mid: Int = Color.parseColor("#138A2E")
    var dim: Int = Color.parseColor("#0D4A1A")
    var bright: Int = Color.parseColor("#B6FFC7")

    val names = listOf("green", "amber", "red", "cyan")

    fun apply(name: String): Boolean {
        when (name) {
            "green" -> set("#020602", "#00FF41", "#138A2E", "#0D4A1A", "#B6FFC7")
            "amber" -> set("#0D0900", "#FFB000", "#B37A00", "#4D3500", "#FFD166")
            "red" -> set("#0A0505", "#FF2A2A", "#A31C1C", "#4A1212", "#FFB3B3")
            "cyan" -> set("#03090A", "#00E5FF", "#0A8FA3", "#0B3D47", "#B3F5FF")
            else -> return false
        }
        return true
    }

    private fun set(b: String, f: String, m: String, d: String, br: String) {
        bg = Color.parseColor(b)
        fg = Color.parseColor(f)
        mid = Color.parseColor(m)
        dim = Color.parseColor(d)
        bright = Color.parseColor(br)
    }
}
