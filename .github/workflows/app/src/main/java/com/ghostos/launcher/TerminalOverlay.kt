package com.ghostos.launcher

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.text.Editable
import android.text.InputType
import android.text.SpannableString
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.text.style.ForegroundColorSpan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TerminalOverlay(private val host: MainActivity) : LinearLayout(host) {
    private val out = TextView(host)
    private val scroll = ScrollView(host)
    val input = EditText(host)
    private var greeted = false

    init {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Palette.bg)
        setPadding(host.dp(12), host.dp(8), host.dp(12), host.dp(8))
        isClickable = true

        val top = LinearLayout(host)
        top.orientation = LinearLayout.HORIZONTAL
        top.addView(term(host, "ghostos://terminal", 12f, Palette.mid), LinearLayout.LayoutParams(0, WRAP, 1f))
        val x = term(host, "[x]", 14f, Palette.fg)
        x.setPadding(host.dp(12), host.dp(6), 0, host.dp(6))
        x.setOnClickListener { host.closeOverlay() }
        top.addView(x)
        addView(top, LinearLayout.LayoutParams(MATCH, WRAP))

        out.typeface = Typeface.MONOSPACE
        out.setTextColor(Palette.fg)
        out.textSize = 12f
        out.setLineSpacing(0f, 1.15f)
        out.setTextIsSelectable(false)
        scroll.addView(out, ViewGroup.LayoutParams(MATCH, WRAP))
        addView(scroll, LinearLayout.LayoutParams(MATCH, 0, 1f))

        val row = LinearLayout(host)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.background = boxBg(host, Palette.bg, Palette.dim)
        row.setPadding(host.dp(8), 0, host.dp(8), 0)
        row.addView(term(host, "root@ghost:~\$ ", 12f, Palette.bright))
        input.typeface = Typeface.MONOSPACE
        input.setTextColor(Palette.fg)
        input.textSize = 13f
        input.background = null
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        input.setSingleLine(true)
        input.imeOptions = EditorInfo.IME_ACTION_GO
        input.setOnEditorActionListener { _, _, _ ->
            submit()
            true
        }
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                Sfx.click()
            }
        })
        row.addView(input, LinearLayout.LayoutParams(0, host.dp(44), 1f))
        addView(row, LinearLayout.LayoutParams(MATCH, WRAP))
    }

    fun onOpen() {
        input.setText("")
        if (!greeted) {
            greeted = true
            say("GhostOS terminal v1.0 // type 'help'", Palette.mid)
        }
    }

    private fun say(s: String, color: Int = Palette.fg) {
        val sp = SpannableString(s + "\n")
        sp.setSpan(ForegroundColorSpan(color), 0, sp.length, 0)
        out.append(sp)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun submit() {
        val raw = input.text.toString().trim()
        input.setText("")
        if (raw.isEmpty()) return
        val first = raw.split(Regex("\\s+"))[0].lowercase()
        say("\$ " + (if (first == "vault") "vault ****" else raw), Palette.bright)
        try {
            exec(raw)
        } catch (e: Exception) {
            say("error: " + (e.message ?: "unknown"), 0xFFFF5555.toInt())
        }
    }

    private fun toggle(name: String, args: List<String>, cur: Boolean, set: (Boolean) -> Unit) {
        val v = when (args.firstOrNull()?.lowercase()) {
            "on", "1", "true" -> true
            "off", "0", "false" -> false
            null -> !cur
            else -> {
                say("usage: $name on|off")
                return
            }
        }
        set(v)
        say(name + ": " + (if (v) "on" else "off"))
        host.refreshAll()
    }

    private fun choose(name: String, args: List<String>, options: List<String>, cur: String, set: (String) -> Unit) {
        val a = args.firstOrNull()?.lowercase()
        if (a == null) {
            say(name + ": " + cur + "   (" + options.joinToString("|") + ")")
            return
        }
        if (a !in options) {
            say("usage: " + name + " " + options.joinToString("|"))
            return
        }
        set(a)
        say(name + ": " + a)
        host.refreshAll()
    }

    private fun exec(raw: String) {
        val parts = raw.split(Regex("\\s+"))
        val cmd = parts[0].lowercase()
        val args = parts.drop(1)
        val rest = args.joinToString(" ")
        when (cmd) {
            "help", "?" -> help()
            "clear", "cls" -> {
                out.text = ""
            }
            "exit", "quit" -> host.closeOverlay()
            "whoami" -> say("root (ghost)")
            "neofetch" -> neofetch()
            "time", "date" -> say(SimpleDateFormat("EEE dd MMM yyyy HH:mm:ss", Locale.ENGLISH).format(Date()))
            "battery", "bat" -> {
                val s = SystemStats.read(host)
                val chg = if (s.charging) " (charging)" else ""
                say("battery " + s.batPct + "%" + chg + "  temp " + "%.1f".format(s.tempC) + "C")
            }
            "ram" -> {
                val s = SystemStats.read(host)
                say("ram " + s.ramUsedMb + "/" + s.ramTotalMb + " MB (" + s.ramPct + "%)")
            }
            "storage", "disk" -> {
                val s = SystemStats.read(host)
                say("storage " + s.stoPct + "% used, " + "%.1f".format(s.stoFreeGb) + " GB free")
            }
            "net", "wifi" -> say("net: " + SystemStats.netString(host))
            "uptime" -> say("up " + SystemStats.read(host).uptime)
            "stats" -> {
                val s = SystemStats.read(host)
                say("BAT " + bar(s.batPct) + " " + s.batPct + "%")
                say("RAM " + bar(s.ramPct) + " " + s.ramPct + "%")
                say("STO " + bar(s.stoPct) + " " + s.stoPct + "%")
                say("NET " + s.net + "   UP " + s.uptime)
            }
            "apps" -> say(AppRepo.visible().size.toString() + " apps, " + AppRepo.hiddenApps().size + " hidden")
            "open", "run", "launch" -> {
                if (rest.isEmpty()) {
                    say("usage: open <app>")
                } else {
                    val a = AppRepo.find(rest)
                    if (a == null) {
                        say("not found: $rest")
                    } else {
                        say("launching " + a.label + "...")
                        host.launch(a)
                    }
                }
            }
            "search", "google" -> {
                if (rest.isEmpty()) say("usage: search <text>") else host.webSearch(rest)
            }
            "call", "dial" -> {
                host.launchIntent(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(rest))))
            }
            "flashlight", "torch", "light" -> {
                val on: Boolean? = when (args.firstOrNull()?.lowercase()) {
                    "on" -> true
                    "off" -> false
                    else -> null
                }
                val ok = host.setTorch(on)
                say(if (ok) "flashlight: " + (if (host.torch) "on" else "off") else "flashlight unavailable")
            }
            "theme" -> {
                val a = args.firstOrNull()?.lowercase()
                if (a == null || a !in Palette.names) {
                    say("theme: " + Prefs.theme + "   (" + Palette.names.joinToString("|") + ")")
                } else {
                    say("switching to $a...")
                    host.applyTheme(a)
                }
            }
            "rain" -> toggle("rain", args, Prefs.rain) { Prefs.rain = it }
            "boot" -> toggle("boot", args, Prefs.boot) { Prefs.boot = it }
            "sound" -> {
                toggle("sound", args, Prefs.sound) { Prefs.sound = it }
                Sfx.beep()
            }
            "shake" -> toggle("shake", args, Prefs.shake) { Prefs.shake = it }
            "dtlock" -> toggle("dtlock", args, Prefs.dtLock) { Prefs.dtLock = it }
            "ticker" -> choose("ticker", args, listOf("off", "counts", "full"), Prefs.ticker) { Prefs.ticker = it }
            "icons" -> choose("icons", args, listOf("mono", "text", "color"), Prefs.icons) { Prefs.icons = it }
            "notif" -> {
                say("opening notification access...")
                host.openNotifAccess()
            }
            "settings" -> host.openAndroidSettings()
            "lock" -> host.lockScreen()
            "pin", "unpin" -> {
                val a = AppRepo.find(rest)
                if (a == null) say("not found: $rest") else {
                    host.setPinned(a.pkg, cmd == "pin")
                    say(cmd + "ned " + a.label)
                }
            }
            "hide" -> {
                val a = AppRepo.find(rest)
                if (a == null) say("not found: $rest") else {
                    host.setHidden(a.pkg, true)
                    say("hidden: " + a.label + " (now in vault)")
                }
            }
            "unhide" -> {
                val a = AppRepo.find(rest, AppRepo.hiddenApps())
                if (a == null) say("not in vault: $rest") else {
                    host.setHidden(a.pkg, false)
                    say("unhidden: " + a.label)
                }
            }
            "vault" -> vault(args)
            "hack", "prank" -> host.playPrank()
            "reboot" -> {
                host.closeOverlay(true)
                host.playBoot()
            }
            else -> say("command not found: $cmd  (try 'help')", 0xFFFF5555.toInt())
        }
    }

    private fun vault(args: List<String>) {
        if (args.isEmpty()) {
            say(if (Vault.isSet()) "vault: pin set. use 'vault <pin>'" else "vault: no pin. use 'vault setpin <pin>'")
            return
        }
        if (args[0].lowercase() == "setpin") {
            val pin = args.getOrNull(1)
            if (pin == null || pin.length < 4) {
                say("pin must be 4+ characters")
                return
            }
            if (Vault.isSet()) {
                val old = args.getOrNull(2)
                if (old == null || Vault.hash(old) != Prefs.vaultHash) {
                    say("denied. use: vault setpin <new> <old>")
                    return
                }
            }
            Prefs.vaultHash = Vault.hash(pin)
            say("vault pin set")
            return
        }
        if (!host.tryVault(args[0])) {
            say(if (Vault.isSet()) "access denied" else "no pin yet. use 'vault setpin <pin>'", 0xFFFF5555.toInt())
        }
    }

    private fun neofetch() {
        val s = SystemStats.read(host)
        val art = listOf(
            "  .-\"\"\"-.  ",
            " / .===. \\ ",
            " \\/ 6 6 \\/ ",
            " ( \\___/ ) ",
            "_ooo__V__ooo"
        )
        val info = listOf(
            "root@ghostos",
            "------------",
            "device  " + Build.MODEL,
            "android " + Build.VERSION.RELEASE + " api " + Build.VERSION.SDK_INT,
            "ram     " + s.ramUsedMb + "/" + s.ramTotalMb + "MB",
            "disk    " + s.stoPct + "% used",
            "uptime  " + s.uptime,
            "theme   " + Prefs.theme
        )
        for (i in 0 until maxOf(art.size, info.size)) {
            say((art.getOrNull(i) ?: "").padEnd(14) + (info.getOrNull(i) ?: ""))
        }
    }

    private fun help() {
        val c = Palette.mid
        say("-- system --", c)
        say("stats battery ram storage net uptime neofetch time")
        say("-- apps --", c)
        say("open <app>  search <text>  call <num>  apps")
        say("pin/unpin <app>  hide/unhide <app>")
        say("-- tools --", c)
        say("flashlight [on|off]  lock  notif  settings  hack  reboot")
        say("-- config --", c)
        say("theme <green|amber|red|cyan>")
        say("rain|boot|sound|shake|dtlock [on|off]")
        say("ticker <off|counts|full>  icons <mono|text|color>")
        say("-- vault --", c)
        say("vault setpin <pin> [old]   vault <pin>")
        say("clear  exit")
    }
}
