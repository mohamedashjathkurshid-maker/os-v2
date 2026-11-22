package com.ghostos.launcher

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.Toast

class MainActivity : Activity() {
    lateinit var root: GestureRoot
    lateinit var rain: RainView
    lateinit var home: HomeView
    lateinit var drawer: DrawerOverlay
    lateinit var vault: DrawerOverlay
    lateinit var terminal: TerminalOverlay
    lateinit var prank: PrankOverlay
    lateinit var boot: BootOverlay
    lateinit var glitch: GlitchView

    private var current: View? = null
    private var resumed = false
    var torch = false
        private set
    private var lastShake = 0L
    private var firstSpike = 0L

    private val shakeListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            val v = event?.values ?: return
            val g = Math.sqrt((v[0] * v[0] + v[1] * v[1] + v[2] * v[2]).toDouble()) / 9.81
            if (g > 2.6) {
                val now = SystemClock.elapsedRealtime()
                if (now - lastShake < 1500) return
                val dt = now - firstSpike
                if (dt < 120) return
                if (dt <= 800) {
                    lastShake = now
                    firstSpike = 0
                    setTorch(null)
                    home.note("flashlight " + (if (torch) "on" else "off"))
                } else {
                    firstSpike = now
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val pkgReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            reloadApps()
        }
    }

    private val userReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (Prefs.boot && resumed && !boot.isRunning) boot.play(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        Palette.apply(Prefs.theme)
        window.statusBarColor = Palette.bg
        window.navigationBarColor = Palette.bg

        root = GestureRoot(this)
        root.setBackgroundColor(Palette.bg)
        rain = RainView(this)
        home = HomeView(this)
        drawer = DrawerOverlay(this, false)
        vault = DrawerOverlay(this, true)
        terminal = TerminalOverlay(this)
        prank = PrankOverlay(this)
        boot = BootOverlay(this)
        glitch = GlitchView(this)
        glitch.target = root

        val full = { FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT) }
        root.addView(rain, full())
        root.addView(home, full())
        for (v in listOf<View>(drawer, vault, terminal, prank)) {
            v.visibility = View.GONE
            root.addView(v, full())
        }
        root.addView(boot, full())
        root.addView(glitch, full())
        setContentView(root)

        root.onSwipe = { up ->
            if (current == null && !boot.isRunning) {
                if (up) openDrawer() else openTerminal()
            }
        }

        val pf = IntentFilter()
        pf.addAction(Intent.ACTION_PACKAGE_ADDED)
        pf.addAction(Intent.ACTION_PACKAGE_REMOVED)
        pf.addAction(Intent.ACTION_PACKAGE_CHANGED)
        pf.addDataScheme("package")
        reg(pkgReceiver, pf)
        reg(userReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))

        reloadApps()
        if (savedInstanceState == null && Prefs.boot) boot.play(false)
    }

    private fun reg(r: BroadcastReceiver, f: IntentFilter) {
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(r, f, Context.RECEIVER_NOT_EXPORTED)
        else registerReceiver(r, f)
    }

    fun reloadApps() {
        Thread {
            AppRepo.load(this)
            AppRepo.ensureDefaults(this)
            runOnUiThread { refreshAll() }
        }.start()
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        NotifStore.onChange = { home.refreshTicker() }
        home.start()
        if (Prefs.rain) {
            rain.visibility = View.VISIBLE
            rain.start()
        } else {
            rain.visibility = View.GONE
        }
        registerShake()
    }

    override fun onPause() {
        super.onPause()
        resumed = false
        NotifStore.onChange = null
        home.stop()
        rain.stop()
        unregisterShake()
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(pkgReceiver) } catch (e: Exception) {}
        try { unregisterReceiver(userReceiver) } catch (e: Exception) {}
        Sfx.release()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Home button pressed: back to the home screen
        if (current != null) closeOverlay(true)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (boot.isRunning) {
            boot.finish()
            return
        }
        if (current != null) closeOverlay()
    }

    // ---------- overlays ----------

    fun openOverlay(v: View) {
        current?.let {
            if (it !== v) {
                if (it === prank) prank.stop()
                it.visibility = View.GONE
            }
        }
        v.alpha = 0f
        v.visibility = View.VISIBLE
        v.animate().alpha(1f).setDuration(160).start()
        current = v
        if (v === prank) prank.start()
    }

    fun closeOverlay(instant: Boolean = false) {
        val v = current ?: return
        current = null
        hideKeyboard()
        if (v === prank) prank.stop()
        if (instant) {
            v.animate().cancel()
            v.visibility = View.GONE
            return
        }
        v.animate().alpha(0f).setDuration(140).withEndAction {
            if (current !== v) v.visibility = View.GONE
        }.start()
    }

    fun openDrawer() {
        drawer.refresh()
        openOverlay(drawer)
        showKeyboard(drawer.searchBox)
    }

    fun openTerminal() {
        terminal.onOpen()
        openOverlay(terminal)
        showKeyboard(terminal.input)
    }

    fun playPrank() {
        openOverlay(prank)
    }

    fun playBoot() {
        boot.play(false)
    }

    fun tryVault(pin: String): Boolean {
        if (!Vault.isSet()) return false
        if (Vault.hash(pin) != Prefs.vaultHash) {
            Sfx.beep()
            return false
        }
        closeOverlay(true)
        vault.refresh()
        openOverlay(vault)
        return true
    }

    private fun showKeyboard(v: View) {
        v.requestFocus()
        v.postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT)
        }, 180)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(root.windowToken, 0)
    }

    // ---------- launching ----------

    fun launch(app: AppInfo) {
        Sfx.beep()
        closeOverlay(true)
        glitch.play {
            try {
                AppRepo.launch(this, app)
            } catch (e: Exception) {
                toast("cannot open " + app.label)
            }
        }
    }

    fun launchIntent(i: Intent) {
        Sfx.beep()
        closeOverlay(true)
        glitch.play {
            startSafe(i)
        }
    }

    fun startSafe(i: Intent) {
        try {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        } catch (e: Exception) {
            toast("no app can handle that")
        }
    }

    fun webSearch(q: String) {
        launchIntent(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(q))))
    }

    fun appInfo(pkg: String) {
        startSafe(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")))
    }

    fun uninstall(pkg: String) {
        startSafe(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")))
    }

    fun openNotifAccess() {
        startSafe(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    fun openAndroidSettings() {
        startSafe(Intent(Settings.ACTION_SETTINGS))
    }

    fun toast(s: String) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    }

    // ---------- state changes ----------

    fun refreshAll() {
        home.refreshPinned()
        home.refreshTicker()
        home.refreshStats()
        if (Prefs.rain) {
            rain.visibility = View.VISIBLE
            if (resumed) rain.start()
        } else {
            rain.stop()
            rain.visibility = View.GONE
        }
        drawer.refresh()
        vault.refresh()
        if (resumed) registerShake()
    }

    fun setPinned(pkg: String, on: Boolean) {
        val cur = Prefs.pinned.toMutableList()
        if (on) {
            if (pkg in cur) return
            if (cur.size >= 8) {
                toast("home is full (8 max)")
                return
            }
            cur.add(pkg)
        } else {
            cur.remove(pkg)
        }
        Prefs.pinned = cur
        toast(if (on) "pinned" else "unpinned")
        refreshAll()
    }

    fun setHidden(pkg: String, hide: Boolean) {
        val h = Prefs.hidden.toMutableSet()
        if (hide) {
            h.add(pkg)
            val p = Prefs.pinned.toMutableList()
            if (p.remove(pkg)) Prefs.pinned = p
        } else {
            h.remove(pkg)
        }
        Prefs.hidden = h
        toast(if (hide) "moved to vault" else "removed from vault")
        refreshAll()
    }

    fun applyTheme(name: String) {
        if (name in Palette.names) {
            Prefs.theme = name
            recreate()
        }
    }

    fun showSettingsMenu() {
        val themeNext = Palette.names[(Palette.names.indexOf(Prefs.theme) + 1) % Palette.names.size]
        val tickerNext = when (Prefs.ticker) {
            "full" -> "counts"
            "counts" -> "off"
            else -> "full"
        }
        val iconsNext = when (Prefs.icons) {
            "mono" -> "text"
            "text" -> "color"
            else -> "mono"
        }
        TermMenu.show(
            this, "ghostos // config", listOf(
                "rain: " + onOff(Prefs.rain) to { Prefs.rain = !Prefs.rain; refreshAll() },
                "boot sequence: " + onOff(Prefs.boot) to { Prefs.boot = !Prefs.boot },
                "sounds: " + onOff(Prefs.sound) to { Prefs.sound = !Prefs.sound; Sfx.beep() },
                "shake flashlight: " + onOff(Prefs.shake) to { Prefs.shake = !Prefs.shake; registerShake() },
                "double-tap lock: " + onOff(Prefs.dtLock) to { Prefs.dtLock = !Prefs.dtLock },
                "ticker: " + Prefs.ticker + " > " + tickerNext to { Prefs.ticker = tickerNext; refreshAll() },
                "icons: " + Prefs.icons + " > " + iconsNext to { Prefs.icons = iconsNext; refreshAll() },
                "theme: " + Prefs.theme + " > " + themeNext to { applyTheme(themeNext) },
                "notification access" to { openNotifAccess() },
                "open terminal" to { openTerminal() }
            )
        )
    }

    private fun onOff(b: Boolean): String = if (b) "on" else "off"

    // ---------- lock screen (device admin, force-lock only) ----------

    fun onDoubleTapLock() {
        if (Prefs.dtLock) lockScreen() else home.note("double-tap lock is off")
    }

    fun lockScreen() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val cn = ComponentName(this, AdminReceiver::class.java)
        if (dpm.isAdminActive(cn)) {
            dpm.lockNow()
        } else {
            val i = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            i.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, cn)
            i.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "GhostOS only uses this to lock the screen when you double-tap the clock.")
            startSafe(i)
        }
    }

    // ---------- flashlight + shake ----------

    fun setTorch(on: Boolean?): Boolean {
        return try {
            val cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = cm.cameraIdList.firstOrNull {
                cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return false
            val v = on ?: !torch
            cm.setTorchMode(id, v)
            torch = v
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun registerShake() {
        unregisterShake()
        if (!Prefs.shake) return
        val sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val s = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        sm.registerListener(shakeListener, s, SensorManager.SENSOR_DELAY_UI)
    }

    private fun unregisterShake() {
        try {
            val sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            sm.unregisterListener(shakeListener)
        } catch (e: Exception) {
        }
    }
}
