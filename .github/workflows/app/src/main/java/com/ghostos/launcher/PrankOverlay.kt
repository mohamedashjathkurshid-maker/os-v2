package com.ghostos.launcher

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.LinearLayout

/** Fake "breach" screen. It only prints random text. Nothing real happens. Tap to exit. */
class PrankOverlay(ctx: Context) : LinearLayout(ctx) {
    private val h = Handler(Looper.getMainLooper())
    private val rnd = java.util.Random()
    private val head = term(ctx, "[ !! ] BREACH MODE // PRANK\nnothing real is happening", 13f, 0xFFFFB000.toInt())
    private val log = term(ctx, "", 11f, Palette.fg)
    private val bars = term(ctx, "", 12f, Palette.bright)
    private val lines = ArrayList<String>()
    private var p1 = 0
    private var p2 = 0
    private var running = false

    private val phrases = arrayOf(
        "bypassing firewall", "injecting payload", "cracking hash", "spoofing mac address",
        "opening reverse shell", "decrypting vault", "tracing route", "escalating privileges",
        "dumping memory", "disabling logs", "forging token", "scanning ports"
    )

    private val loop = object : Runnable {
        override fun run() {
            if (!running) return
            tick()
            h.postDelayed(this, 90)
        }
    }

    init {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Palette.bg)
        setPadding(ctx.dp(16), ctx.dp(24), ctx.dp(16), ctx.dp(16))
        isClickable = true
        addView(head, LinearLayout.LayoutParams(MATCH, WRAP))
        addView(bars, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = ctx.dp(14) })
        addView(log, LinearLayout.LayoutParams(MATCH, 0, 1f).apply { topMargin = ctx.dp(14) })
        addView(term(ctx, "tap anywhere to exit", 12f, Palette.mid), LinearLayout.LayoutParams(MATCH, WRAP))
    }

    private fun hex(n: Int): String {
        val sb = StringBuilder()
        repeat(n) { sb.append("0123456789abcdef"[rnd.nextInt(16)]) }
        return sb.toString()
    }

    private fun tick() {
        lines.add("[" + hex(4) + "] " + phrases[rnd.nextInt(phrases.size)] + " " + hex(6) + ".. ok")
        while (lines.size > 22) lines.removeAt(0)
        p1 = minOf(100, p1 + rnd.nextInt(4))
        p2 = minOf(100, p2 + rnd.nextInt(3))
        if (p1 == 100 && p2 == 100) {
            lines.add("> ROOT SHELL OPENED (just kidding)")
            p1 = 0
            p2 = 0
        }
        log.text = lines.joinToString("\n")
        bars.text = "bypass  " + bar(p1, 16) + " " + p1 + "%\n" + "decrypt " + bar(p2, 16) + " " + p2 + "%"
        Sfx.click()
    }

    fun start() {
        lines.clear()
        p1 = 0
        p2 = 0
        running = true
        h.post(loop)
    }

    fun stop() {
        running = false
        h.removeCallbacks(loop)
    }
}
