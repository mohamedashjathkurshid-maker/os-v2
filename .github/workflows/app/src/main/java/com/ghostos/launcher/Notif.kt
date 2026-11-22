package com.ghostos.launcher

import android.app.Notification
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

object NotifStore {
    class Item(val app: String, val pkg: String, val title: String)

    @Volatile
    var items: List<Item> = emptyList()

    @Volatile
    var connected = false

    var onChange: (() -> Unit)? = null

    fun isEnabled(ctx: Context): Boolean {
        val s = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners") ?: return false
        return s.contains(ctx.packageName)
    }
}

class GhostNotificationService : NotificationListenerService() {
    override fun onListenerConnected() {
        NotifStore.connected = true
        refresh()
    }

    override fun onListenerDisconnected() {
        NotifStore.connected = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refresh()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refresh()
    }

    private fun refresh() {
        val pm = packageManager
        val out = ArrayList<NotifStore.Item>()
        try {
            val arr = activeNotifications
            if (arr != null) {
                for (n in arr) {
                    if (n.isOngoing) continue
                    if (n.packageName == packageName) continue
                    val label = try {
                        pm.getApplicationLabel(pm.getApplicationInfo(n.packageName, 0)).toString()
                    } catch (e: Exception) {
                        n.packageName
                    }
                    val title = n.notification?.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
                    out.add(NotifStore.Item(label, n.packageName, title))
                }
            }
        } catch (e: Exception) {
        }
        NotifStore.items = out
        Handler(Looper.getMainLooper()).post { NotifStore.onChange?.invoke() }
    }
}
