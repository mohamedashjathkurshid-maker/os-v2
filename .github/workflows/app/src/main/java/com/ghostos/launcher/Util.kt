package com.ghostos.launcher

import android.content.Context
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import android.view.ViewGroup
import android.widget.TextView
import kotlin.random.Random

const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

fun withAlpha(color: Int, a: Int): Int =
    Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))

fun boxBg(ctx: Context, fill: Int, stroke: Int, strokeDp: Int = 1): GradientDrawable {
    val g = GradientDrawable()
    g.setColor(fill)
    g.setStroke(ctx.dp(strokeDp), stroke)
    g.cornerRadius = 0f
    return g
}

fun term(ctx: Context, text: String = "", sizeSp: Float = 12f, color: Int = Palette.fg): TextView {
    val t = TextView(ctx)
    t.typeface = Typeface.MONOSPACE
    t.setTextColor(color)
    t.textSize = sizeSp
    t.text = text
    t.includeFontPadding = false
    t.setLineSpacing(0f, 1.15f)
    return t
}

fun bar(pct: Int, width: Int = 10): String {
    val p = pct.coerceIn(0, 100)
    val n = (p * width + 50) / 100
    return "█".repeat(n) + "░".repeat(width - n)
}

private val SCR = "01#\$%&@=+*<>?/|abcdef".toCharArray()

/** Text "decrypt" effect: random characters that settle into the target text. */
fun TextView.scramble(target: String, durationMs: Long = 450L) {
    (tag as? Runnable)?.let { removeCallbacks(it) }
    val start = SystemClock.uptimeMillis()
    val view = this
    val r = object : Runnable {
        override fun run() {
            val p = ((SystemClock.uptimeMillis() - start).toFloat() / durationMs).coerceIn(0f, 1f)
            if (p >= 1f) {
                view.text = target
                return
            }
            val reveal = (target.length * p).toInt()
            val sb = StringBuilder()
            for (i in target.indices) {
                val c = target[i]
                if (i < reveal || c == ' ' || c == '\n') sb.append(c)
                else sb.append(SCR[Random.nextInt(SCR.size)])
            }
            view.text = sb.toString()
            view.postDelayed(this, 35)
        }
    }
    tag = r
    post(r)
}

/** Monochrome icon filter tinted with the current theme colour. */
object Mono {
    private var cached: ColorMatrixColorFilter? = null
    private var cachedFor = 0

    fun filter(): ColorMatrixColorFilter {
        val c = cached
        if (c != null && cachedFor == Palette.fg) return c
        val r = Color.red(Palette.fg) / 255f
        val g = Color.green(Palette.fg) / 255f
        val b = Color.blue(Palette.fg) / 255f
        val m = ColorMatrix()
        m.setSaturation(0f)
        m.postConcat(
            ColorMatrix(
                floatArrayOf(
                    1.5f, 0f, 0f, 0f, 40f,
                    0f, 1.5f, 0f, 0f, 40f,
                    0f, 0f, 1.5f, 0f, 40f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
        m.postConcat(
            ColorMatrix(
                floatArrayOf(
                    r, 0f, 0f, 0f, 0f,
                    0f, g, 0f, 0f, 0f,
                    0f, 0f, b, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
        val f = ColorMatrixColorFilter(m)
        cached = f
        cachedFor = Palette.fg
        return f
    }
}

/** Tiny retro sound effects. Off by default. */
object Sfx {
    private var tg: ToneGenerator? = null

    private fun gen(): ToneGenerator? {
        if (tg == null) {
            tg = try {
                ToneGenerator(AudioManager.STREAM_SYSTEM, 35)
            } catch (e: Exception) {
                null
            }
        }
        return tg
    }

    fun click() {
        if (!Prefs.sound) return
        try { gen()?.startTone(ToneGenerator.TONE_DTMF_D, 18) } catch (e: Exception) {}
    }

    fun beep() {
        if (!Prefs.sound) return
        try { gen()?.startTone(ToneGenerator.TONE_PROP_ACK, 120) } catch (e: Exception) {}
    }

    fun release() {
        try { tg?.release() } catch (e: Exception) {}
        tg = null
    }
}
