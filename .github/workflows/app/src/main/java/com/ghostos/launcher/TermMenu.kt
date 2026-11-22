package com.ghostos.launcher

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.widget.LinearLayout

object TermMenu {
    fun show(host: Activity, title: String, items: List<Pair<String, () -> Unit>>) {
        val d = Dialog(host)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val box = LinearLayout(host)
        box.orientation = LinearLayout.VERTICAL
        box.background = boxBg(host, Palette.bg, Palette.fg)
        box.setPadding(host.dp(14), host.dp(12), host.dp(14), host.dp(10))
        box.addView(term(host, "// " + title, 12f, Palette.mid))
        for ((label, action) in items) {
            val t = term(host, "> " + label, 14f, Palette.fg)
            t.setPadding(0, host.dp(12), 0, host.dp(4))
            t.setOnClickListener {
                Sfx.click()
                d.dismiss()
                action()
            }
            box.addView(t)
        }
        d.setContentView(box)
        d.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        d.show()
        d.window?.setLayout((host.resources.displayMetrics.widthPixels * 0.86f).toInt(), WRAP)
    }
}
