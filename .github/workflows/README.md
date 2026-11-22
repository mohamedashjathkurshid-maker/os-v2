# OVERCLOCK//UNIFIED v0.2 — HUD launcher (Concept E)

Custom hacker-themed overlay UI for Android. D-core hybrid:
live system HUD + command console + stealth edge tab + app fan.

## What's new in v0.2
- Real FPS counter (dumpsys-based; see "FPS note" below)
- Live battery % on the HUD
- Stealth mode: `stealth on` hides console + tab, HUD survives alone
- Flashlight command: `torch on` / `torch off`
- `ip` command shows your Wi-Fi address
- Magnetic edge-snap: drag HUD or tab near an edge, it pins itself
- Boot persistence: flip "Auto-start HUD on boot" in the control panel
- First CPU/RAM reading no longer shows 0%
- Fan icons keep their neon frame; fan bug fixed
- Faster refresh (800ms)

## Build with GitHub — no PC tools needed
1. Upload this folder to a GitHub repo (drag the whole folder onto
   Add file → Upload files → Commit).
2. Actions tab → "Build APK" runs (~5–10 min first time).
3. Click the green run → Artifacts → download `overclock-unified-apk`
   → inside is `app-debug.apk`. Install on the phone.

## First run
1. Open app → START HUD → grant "Display over other apps" → START again.
2. Optional: enable "Auto-start HUD on boot".

## Controls
- HUD: tap = sysboost · hold = kill background · drag = move (snaps to edges).
- Console: `help` `open shapr3d` `boost` `killall` `status` `torch on/off`
  `stealth on/off` `ip` `hud off` `dock`.
- Amber tab: tap = app fan · drag = reposition (snaps to edge).

## FPS note
FPS needs `dumpsys` access. Either root the phone, or once connect it
to any PC with adb and run:
    adb shell pm grant com.ghostgrid.overclock android.permission.DUMP
Without that, fps shows "--" (everything else works fine).

## Notes
- `boost` switches CPU governor only with root; otherwise purges RAM.
- Works fully on stock A07 — bake into the ROM later as system/priv-app.
