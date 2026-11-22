package com.ghostos.launcher

import android.content.Context
import android.view.MotionEvent
import android.widget.FrameLayout
import kotlin.math.abs

/** Root container: detects quick vertical swipes without stealing touches from children. */
class GestureRoot(ctx: Context) : FrameLayout(ctx) {
    var onSwipe: ((Boolean) -> Unit)? = null // true = swipe up, false = swipe down
    private var x0 = 0f
    private var y0 = 0f
    private var t0 = 0L

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                x0 = ev.rawX
                y0 = ev.rawY
                t0 = ev.eventTime
            }
            MotionEvent.ACTION_UP -> {
                val dx = ev.rawX - x0
                val dy = ev.rawY - y0
                val dt = ev.eventTime - t0
                if (dt < 650 && abs(dy) > context.dp(90) && abs(dy) > abs(dx) * 1.5f) {
                    onSwipe?.invoke(dy < 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}
