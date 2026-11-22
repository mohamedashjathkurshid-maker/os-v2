package com.ghostos.launcher

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.FrameLayout

/** Fake hacker boot log. Tap to skip. */
class BootOverlay(ctx: Context) : FrameLayout(ctx) {
    private val tv = term(ctx, "", 12f, Palette.fg)
    private val h = Handler(Looper.getMainLooper())

    var isRunning = false
        private set

    init {
        setBackgroundColor(Palette.bg)
        tv.setPadding(ctx.dp(20), ctx.dp(60), ctx.dp(20), 0)
        addView(tv, FrameLayout.LayoutParams(MATCH, WRAP))
        isClickable = true
        setOnClickListener { finish() }
        visibility = View.GONE
    }

    private fun lines(quick: Boolean): List<String> {
        val ok = "[ OK ] "
        return if (quick) {
            listOf("GHOSTOS :: re-auth", ok + "session restored", ok + "hud online", "", "> ACCESS GRANTED")
        } else {
            listOf(
                "GHOSTOS v1.0 :: " + Build.MODEL.lowercase(),
                ok + "kernel " + (System.getProperty("os.version") ?: "?"),
                ok + "android " + Build.VERSION.RELEASE + " (api " + Build.VERSION.SDK_INT + ")",
                ok + "mount /sdcard",
                ok + "net wlan0",
                ok + "hud online",
                ok + "vault locked",
                ok + "shell ready",
                "",
                "loading " + bar(100, 16) + " 100%",
                "> ACCESS GRANTED"
            )
        }
    }

    fun play(quick: Boolean) {
        h.removeCallbacksAndMessages(null)
        isRunning = true
        visibility = View.VISIBLE
        alpha = 1f
        tv.text = ""
        val ls = lines(quick)
        var i = 0
        val step = object : Runnable {
            override fun run() {
                if (!isRunning) return
                if (i < ls.size) {
                    tv.append(ls[i] + "\n")
                    Sfx.click()
                    i++
                    h.postDelayed(this, if (quick) 70L else 130L)
                } else {
                    Sfx.beep()
                    h.postDelayed({ finish() }, 450L)
                }
            }
        }
        h.post(step)
    }

    fun finish() {
        if (!isRunning) return
        isRunning = false
        h.removeCallbacksAndMessages(null)
        animate().alpha(0f).setDuration(220).withEndAction {
            if (!isRunning) visibility = View.GONE
        }.start()
    }
}
