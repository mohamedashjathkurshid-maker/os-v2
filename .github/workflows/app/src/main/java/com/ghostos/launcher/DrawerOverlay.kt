package com.ghostos.launcher

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.graphics.Color
import android.graphics.drawable.ColorDrawable

/** App drawer as a searchable command list. Also used for the hidden vault (vaultMode = true). */
class DrawerOverlay(private val host: MainActivity, private val vaultMode: Boolean) : LinearLayout(host) {
    private class Row(val label: String, val app: AppInfo?, val action: (() -> Unit)?)

    private val title = term(host, "", 12f, Palette.mid)
    val searchBox = EditText(host)
    private val list = ListView(host)
    private var rows: List<Row> = emptyList()

    private class RowView(ctx: Context) : LinearLayout(ctx) {
        val icon = ImageView(ctx)
        val label = term(ctx, "", 15f, Palette.fg)

        init {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(ctx.dp(4), ctx.dp(7), ctx.dp(4), ctx.dp(7))
            addView(icon, LinearLayout.LayoutParams(ctx.dp(30), ctx.dp(30)))
            label.setPadding(ctx.dp(12), 0, 0, 0)
            addView(label, LinearLayout.LayoutParams(MATCH, WRAP))
        }
    }

    private val adapter = object : BaseAdapter() {
        override fun getCount(): Int = rows.size
        override fun getItem(position: Int): Any = rows[position]
        override fun getItemId(position: Int): Long = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val v = (convertView as? RowView) ?: RowView(host)
            val r = rows[position]
            v.label.text = "> " + r.label.lowercase()
            val app = r.app
            if (app != null && Prefs.icons != "text") {
                v.icon.visibility = View.VISIBLE
                v.icon.setImageDrawable(app.freshIcon())
                if (Prefs.icons == "mono") v.icon.setColorFilter(Mono.filter())
            } else {
                v.icon.visibility = View.GONE
            }
            return v
        }
    }

    init {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Palette.bg)
        setPadding(host.dp(12), host.dp(8), host.dp(12), host.dp(8))
        isClickable = true

        val top = LinearLayout(host)
        top.orientation = LinearLayout.HORIZONTAL
        top.addView(title, LinearLayout.LayoutParams(0, WRAP, 1f))
        val x = term(host, "[x]", 14f, Palette.fg)
        x.setPadding(host.dp(12), host.dp(6), 0, host.dp(6))
        x.setOnClickListener { host.closeOverlay() }
        top.addView(x)
        addView(top, LinearLayout.LayoutParams(MATCH, WRAP))

        val srow = LinearLayout(host)
        srow.orientation = LinearLayout.HORIZONTAL
        srow.gravity = Gravity.CENTER_VERTICAL
        srow.background = boxBg(host, Palette.bg, Palette.dim)
        srow.setPadding(host.dp(8), 0, host.dp(8), 0)
        srow.addView(term(host, if (vaultMode) "vault> " else "search> ", 13f, Palette.bright))
        searchBox.typeface = android.graphics.Typeface.MONOSPACE
        searchBox.setTextColor(Palette.fg)
        searchBox.textSize = 14f
        searchBox.background = null
        searchBox.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        searchBox.setSingleLine(true)
        searchBox.imeOptions = EditorInfo.IME_ACTION_GO
        searchBox.setOnEditorActionListener { _, _, _ ->
            submit()
            true
        }
        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                Sfx.click()
                filter(s?.toString() ?: "")
            }
        })
        srow.addView(searchBox, LinearLayout.LayoutParams(0, host.dp(44), 1f))
        addView(srow, LinearLayout.LayoutParams(MATCH, WRAP))

        list.adapter = adapter
        list.divider = null
        list.dividerHeight = 0
        list.cacheColorHint = Color.TRANSPARENT
        list.selector = ColorDrawable(Color.TRANSPARENT)
        list.setOnItemClickListener { _, _, pos, _ ->
            Sfx.click()
            if (pos in rows.indices) activate(rows[pos])
        }
        list.setOnItemLongClickListener { _, _, pos, _ ->
            val app = rows.getOrNull(pos)?.app
            if (app != null) {
                val pinned = app.pkg in Prefs.pinned
                TermMenu.show(
                    host, app.label, listOf(
                        (if (pinned) "unpin from home" else "pin to home") to { host.setPinned(app.pkg, !pinned) },
                        (if (vaultMode) "unhide (leave vault)" else "hide (move to vault)") to { host.setHidden(app.pkg, !vaultMode) },
                        "app info" to { host.appInfo(app.pkg) },
                        "uninstall" to { host.uninstall(app.pkg) }
                    )
                )
            }
            true
        }
        addView(list, LinearLayout.LayoutParams(MATCH, 0, 1f))
    }

    fun refresh() {
        title.scramble(if (vaultMode) "ghostos://vault [unlocked]" else "ghostos://apps", 400)
        searchBox.setText("")
        filter("")
    }

    private fun source(): List<AppInfo> = if (vaultMode) AppRepo.hiddenApps() else AppRepo.visible()

    private fun filter(q: String) {
        val ql = q.trim().lowercase()
        val src = source()
        val matches = if (ql.isEmpty()) src else src.filter { it.label.lowercase().contains(ql) }
        val r = ArrayList<Row>()
        for (a in matches) r.add(Row(a.label, a, null))
        if (!vaultMode && ql.isNotEmpty() && !isVaultCmd(ql)) {
            val query = q.trim()
            r.add(Row("web: " + query, null) { host.webSearch(query) })
        }
        rows = r
        adapter.notifyDataSetChanged()
    }

    private fun isVaultCmd(q: String): Boolean = q.startsWith("vault ") || q.startsWith("sudo ")

    private fun submit() {
        val q = searchBox.text.toString().trim()
        if (!vaultMode && isVaultCmd(q.lowercase())) {
            val pin = q.substring(q.indexOf(' ') + 1).trim()
            if (!host.tryVault(pin)) title.scramble("access denied", 300)
            return
        }
        rows.firstOrNull()?.let { activate(it) }
    }

    private fun activate(r: Row) {
        val app = r.app
        if (app != null) host.launch(app) else r.action?.invoke()
    }
}
