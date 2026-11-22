package com.ghostos.launcher

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

/** Short screen-glitch effect played when an app is opened. */
class GlitchView(ctx: Context) : View(ctx) {
    private val p = Paint()
    private val rnd = java.util.Random()
    private var active = false
    var target: View? = null

    init {
        visibility = GONE
        isClickable = false
    }

    fun play(durationMs: Long = 260L, onEnd: () -> Unit) {
        visibility = VISIBLE
        active = true
        val a = ValueAnimator.ofFloat(0f, 1f)
        a.duration = durationMs
        a.addUpdateListener {
            target?.translationX = if (it.animatedFraction < 0.9f) (rnd.nextInt(21) - 10).toFloat() else 0f
            invalidate()
        }
        a.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                active = false
                target?.translationX = 0f
                visibility = GONE
                onEnd()
            }
        })
        a.start()
    }

    override fun onDraw(canvas: Canvas) {
        if (!active || width == 0 || height == 0) return
        repeat(9) {
            val y = rnd.nextInt(height)
            val h = 2 + rnd.nextInt(height / 20 + 1)
            val x = rnd.nextInt(width / 2 + 1)
            val w = width / 3 + rnd.nextInt(width / 2 + 1)
            p.color = if (rnd.nextBoolean()) Palette.fg else Palette.bright
            p.alpha = 90 + rnd.nextInt(120)
            canvas.drawRect(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat(), p)
        }
    }
}
