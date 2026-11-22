package com.ghostos.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.provider.MediaStore
import android.provider.Settings

class AppInfo(val label: String, val pkg: String, val activity: String, val icon: Drawable) {
    /** Each view gets its own drawable so colour filters never leak between views. */
    fun freshIcon(): Drawable = icon.constantState?.newDrawable()?.mutate() ?: icon
}

object AppRepo {
    @Volatile
    var all: List<AppInfo> = emptyList()

    @Suppress("DEPRECATION")
    fun load(ctx: Context) {
        val pm = ctx.packageManager
        val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val res = pm.queryIntentActivities(q, 0)
        val list = ArrayList<AppInfo>()
        for (ri in res) {
            try {
                list.add(
                    AppInfo(
                        ri.loadLabel(pm).toString(),
                        ri.activityInfo.packageName,
                        ri.activityInfo.name,
                        ri.loadIcon(pm)
                    )
                )
            } catch (e: Exception) {
            }
        }
        all = list.filter { it.pkg != ctx.packageName }.sortedBy { it.label.lowercase() }
    }

    fun visible(): List<AppInfo> {
        val h = Prefs.hidden
        return all.filter { it.pkg !in h }
    }

    fun hiddenApps(): List<AppInfo> {
        val h = Prefs.hidden
        return all.filter { it.pkg in h }
    }

    fun find(q: String, src: List<AppInfo> = visible()): AppInfo? {
        val s = q.trim().lowercase()
        if (s.isEmpty()) return null
        return src.firstOrNull { it.label.lowercase() == s }
            ?: src.firstOrNull { it.label.lowercase().startsWith(s) }
            ?: src.firstOrNull { it.label.lowercase().contains(s) }
    }

    fun launch(ctx: Context, app: AppInfo) {
        val i = Intent(Intent.ACTION_MAIN)
        i.addCategory(Intent.CATEGORY_LAUNCHER)
        i.setClassName(app.pkg, app.activity)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        ctx.startActivity(i)
    }

    /** First run: pin sensible defaults (browser, messages, camera, gallery, ...). */
    fun ensureDefaults(ctx: Context) {
        if (Prefs.pinnedInit || all.isEmpty()) return
        val pm = ctx.packageManager
        val probes = listOf(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING),
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_GALLERY),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR),
            Intent(Settings.ACTION_SETTINGS),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)
        )
        val out = ArrayList<String>()
        for (i in probes) {
            val pkg = pm.resolveActivity(i, 0)?.activityInfo?.packageName
            if (pkg != null && all.any { it.pkg == pkg } && pkg !in out) out.add(pkg)
        }
        Prefs.pinned = out.take(8)
        Prefs.pinnedInit = true
    }
}
