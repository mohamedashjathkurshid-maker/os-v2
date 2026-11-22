package com.ghostos.launcher

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeView(private val host: MainActivity) : LinearLayout(host) {
    private val h = Handler(Looper.getMainLooper())
    private val header = term(host, "", 11f, Palette.mid)
    private val clock = term(host, "--:--", 54f, Palette.fg)
    private val sec = term(host, ":--", 16f, Palette.mid)
    private val date = term(host, "", 12f, Palette.mid)
    private val hud = term(host, "", 12f, Palette.fg)
    private val tickerTitle = term(host, "> notifications", 11f, Palette.mid)
    private val ticker = term(host, "", 12f, Palette.fg)
    private val pinnedBox = LinearLayout(host)
    private lateinit var tickerPanel: LinearLayout

    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val secFmt = SimpleDateFormat("ss", Locale.getDefault())
    private val dateFmt = SimpleDateFormat("EEE dd.MM.yyyy", Locale.ENGLISH)
    private val defaultHeader = "GHOSTOS v1.0 :: " + Build.MODEL.lowercase() + " :: root"

    private var running = false
    private var lastTime = ""
    private var n = 0
    private var lastTicker = ""
    private var lastTap = 0L
    private var noteReset: Runnable? = null

    private val loop = object : Runnable {
        override fun run() {
            if (!running) return
            updateClock()
            if (n % 5 == 0) refreshStats()
            if (n % 15 == 0) refreshTicker()
            n++
            h.postDelayed(this, 1000)
        }
    }

    private fun panel(vararg v: View): LinearLayout {
        val p = LinearLayout(host)
        p.orientation = LinearLayout.VERTICAL
        p.background = boxBg(host, withAlpha(Palette.bg, 215), Palette.dim)
        p.setPadding(host.dp(10), host.dp(8), host.dp(10), host.dp(8))
        for (x in v) p.addView(x)
        return p
    }

    private fun lp(top: Int = 0): LinearLayout.LayoutParams {
        val l = LinearLayout.LayoutParams(MATCH, WRAP)
        l.topMargin = host.dp(top)
        return l
    }

    init {
        orientation = LinearLayout.VERTICAL
        setPadding(host.dp(16), host.dp(8), host.dp(16), host.dp(8))
        isLongClickable = true
        setOnLongClickListener {
            host.showSettingsMenu()
            true
        }

        addView(header, lp())

        // clock: double-tap = lock screen, long-press = prank mode
        val timeBlock = LinearLayout(host)
        timeBlock.orientation = LinearLayout.VERTICAL
        timeBlock.setPadding(0, host.dp(6), 0, host.dp(6))
        val clockRow = LinearLayout(host)
        clockRow.orientation = LinearLayout.HORIZONTAL
        clockRow.gravity = Gravity.BOTTOM
        clockRow.addView(clock)
        sec.setPadding(host.dp(6), 0, 0, host.dp(8))
        clockRow.addView(sec)
        timeBlock.addView(clockRow)
        timeBlock.addView(date)
        timeBlock.setOnClickListener {
            val now = SystemClock.elapsedRealtime()
            if (now - lastTap < 350) {
                lastTap = 0
                host.onDoubleTapLock()
            } else {
                lastTap = now
            }
        }
        timeBlock.setOnLongClickListener {
            host.playPrank()
            true
        }
        addView(timeBlock, lp(2))

        addView(panel(hud), lp(8))

        tickerPanel = panel(tickerTitle, ticker)
        addView(tickerPanel, lp(8))

        addView(View(host), LinearLayout.LayoutParams(MATCH, 0, 1f))

        pinnedBox.orientation = LinearLayout.VERTICAL
        addView(pinnedBox, lp())

        val dock = LinearLayout(host)
        dock.orientation = LinearLayout.HORIZONTAL
        dock.background = boxBg(host, withAlpha(Palette.bg, 215), Palette.dim)
        dock.addView(dockBtn("[tel]") { host.launchIntent(Intent(Intent.ACTION_DIAL)) }, LinearLayout.LayoutParams(0, WRAP, 1f))
        dock.addView(
            dockBtn("[web]") { host.launchIntent(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))) },
            LinearLayout.LayoutParams(0, WRAP, 1f)
        )
        dock.addView(dockBtn("[app]") { host.openDrawer() }, LinearLayout.LayoutParams(0, WRAP, 1f))
        addView(dock, lp(8))
    }

    private fun dockBtn(label: String, onClick: () -> Unit): View {
        val t = term(host, label, 15f, Palette.fg)
        t.gravity = Gravity.CENTER
        t.setPadding(0, host.dp(14), 0, host.dp(14))
        t.setOnClickListener {
            Sfx.click()
            onClick()
        }
        return t
    }

    fun start() {
        if (running) return
        running = true
        n = 0
        lastTime = ""
        header.scramble(defaultHeader, 700)
        refreshPinned()
        refreshTicker()
        refreshStats()
        h.post(loop)
    }

    fun stop() {
        running = false
        h.removeCallbacks(loop)
    }

    /** Briefly shows a message in the header line. */
    fun note(text: String) {
        header.scramble(text, 300)
        noteReset?.let { h.removeCallbacks(it) }
        val r = Runnable { header.scramble(defaultHeader, 400) }
        noteReset = r
        h.postDelayed(r, 2400)
    }

    private fun updateClock() {
        val now = Date()
        val t = timeFmt.format(now)
        if (t != lastTime) {
            lastTime = t
            clock.scramble(t, 350)
            date.text = dateFmt.format(now).uppercase()
        }
        sec.text = ":" + secFmt.format(now)
    }

    fun refreshStats() {
        val s = SystemStats.read(host)
        val chg = if (s.charging) " +" else ""
        hud.text = "BAT " + bar(s.batPct) + " " + s.batPct + "%" + chg + "\n" +
            "RAM " + bar(s.ramPct) + " " + s.ramPct + "%\n" +
            "STO " + bar(s.stoPct) + " " + s.stoPct + "%\n" +
            "NET " + s.net + "\n" +
            "TMP " + "%.1f".format(s.tempC) + "C   UP " + s.uptime
    }

    fun refreshTicker() {
        val mode = Prefs.ticker
        if (mode == "off") {
            tickerPanel.visibility = View.GONE
            return
        }
        tickerPanel.visibility = View.VISIBLE
        val text: String
        if (!NotifStore.isEnabled(host)) {
            text = "access off. long-press screen >\nnotification access"
        } else {
            val items = NotifStore.items
            if (items.isEmpty()) {
                text = "inbox clean. no new signals."
            } else if (mode == "counts") {
                val groups = LinkedHashMap<String, Int>()
                for (i in items) groups[i.app] = (groups[i.app] ?: 0) + 1
                val lines = groups.entries.take(3).map { "> " + it.key.lowercase().take(18) + " (" + it.value + ")" }
                val more = groups.size - 3
                text = lines.joinToString("\n") + (if (more > 0) "\n> +" + more + " more" else "")
            } else {
                val lines = items.take(3).map { "> " + it.app.lowercase().take(10) + ": " + it.title.take(24) }
                val more = items.size - 3
                text = lines.joinToString("\n") + (if (more > 0) "\n> +" + more + " more" else "")
            }
        }
        if (text != lastTicker) {
            lastTicker = text
            ticker.scramble(text, 400)
        }
    }

    fun refreshPinned() {
        pinnedBox.removeAllViews()
        val apps = Prefs.pinned.mapNotNull { pkg -> AppRepo.all.firstOrNull { it.pkg == pkg } }.take(8)
        for (r in 0 until 2) {
            val row = LinearLayout(host)
            row.orientation = LinearLayout.HORIZONTAL
            for (c in 0 until 4) {
                val idx = r * 4 + c
                val cell: View = if (idx < apps.size) cellFor(apps[idx]) else View(host)
                row.addView(cell, LinearLayout.LayoutParams(0, WRAP, 1f))
            }
            pinnedBox.addView(row, LinearLayout.LayoutParams(MATCH, WRAP))
        }
    }

    private fun cellFor(app: AppInfo): View {
        val c = LinearLayout(host)
        c.orientation = LinearLayout.VERTICAL
        c.gravity = Gravity.CENTER_HORIZONTAL
        c.setPadding(0, host.dp(6), 0, host.dp(6))
        val size = host.dp(46)
        val mode = Prefs.icons
        val iconView: View
        if (mode == "text") {
            val t = term(host, app.label.take(2).uppercase(), 15f, Palette.fg)
            t.gravity = Gravity.CENTER
            iconView = t
        } else {
            val iv = ImageView(host)
            iv.setImageDrawable(app.freshIcon())
            iv.scaleType = ImageView.ScaleType.FIT_CENTER
            iv.setPadding(host.dp(7), host.dp(7), host.dp(7), host.dp(7))
            if (mode == "mono") iv.setColorFilter(Mono.filter())
            iconView = iv
        }
        iconView.background = boxBg(host, withAlpha(Palette.bg, 215), if (mode == "mono" || mode == "text") Palette.fg else Palette.dim)
        c.addView(iconView, LinearLayout.LayoutParams(size, size))
        val l = term(host, app.label.lowercase(), 11f, Palette.mid)
        l.maxLines = 1
        l.ellipsize = TextUtils.TruncateAt.END
        l.gravity = Gravity.CENTER
        l.setPadding(host.dp(2), host.dp(4), host.dp(2), 0)
        c.addView(l, LinearLayout.LayoutParams(MATCH, WRAP))
        c.setOnClickListener {
            Sfx.click()
            host.launch(app)
        }
        c.setOnLongClickListener {
            TermMenu.show(
                host, app.label, listOf(
                    "unpin from home" to { host.setPinned(app.pkg, false) },
                    "hide (move to vault)" to { host.setHidden(app.pkg, true) },
                    "app info" to { host.appInfo(app.pkg) }
                )
            )
            true
        }
        return c
    }
}
