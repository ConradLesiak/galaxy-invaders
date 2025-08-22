# Galaxy Invaders
Arcade shoot ’em up built with **Java + LibGDX**. Fast, readable code, smooth scaling with a `FitViewport`, and tuned for both **desktop** and **web (HTML)** builds—perfect for itch.io.

> Package: `com.rgs.galaxyinvaders`  
> Window Title: **Galaxy Invaders**  
> Default virtual size: **800 × 480** (letterboxed via `FitViewport`)

---

## 🎮 Gameplay at a Glance
- Classic top‑down space shooter.
- **Player & Enemies** use PNG sprites (`player1.png`, `enemy1.png`); **Boss** uses `boss1.png`.
- **Bullets** are simple **circles** (player = light green, enemy = red).
- **Shield** renders as a 50% transparent ring when active.
- **Enemy/boss hit flash**: sprite overlays with a brief red tint (no full‑screen flash).
- **Power‑ups** drop as you defeat enemies and **when you hit the boss**:
  - **Rapid** (auto‑fire burst)
  - **Spread** (3‑way shots)
  - **Shield** (absorbs a hit)
  - **1‑Up** (extra life)
- **Boss tuning**:
  - Boss HP is **halved** versus the old baseline (faster kills).
  - Score increases on **every boss hit** (+10), not just on kill.
  - **Boss #1** uses **2 patterns** (Aimed Volley + Fan).
  - **Boss #2+** unlock up to **3 patterns** (adds Spiral).
  - **All bosses attack slower** than default (longer cooldowns).
  - Global cap on active boss bullets for fairness.
- **Collision forgiveness**: bullet hitboxes are ~60% of their visual size.
- **Difficulty**: starts easier and ramps up gently as you defeat bosses.

---

## ⌨️ Controls
- **Move**: `A/D` or `←/→`
- **Fire**: `SPACE` (auto-fire when Rapid is active)
- **Pause**: `P` or `ENTER`
- **Pause / Menu**: `ESC` opens pause; **ESC again (while paused)** → Main Menu
- **Game Over**: `ENTER` = Retry, `ESC` = Main Menu

You can tweak keys in code if you prefer a different layout.

---

## 🗂️ Project Structure (typical LibGDX multi-module)
```
/core        -> shared game code (this is where most logic lives)
/lwjgl3      -> desktop launcher (or /desktop in older templates)
/html        -> HTML (GWT) target for web builds
/android     -> Android app module (optional)
/assets      -> game assets (images, sounds, etc.)
```
> Module names may vary (`lwjgl3` vs `desktop`). Adjust commands accordingly.

---

## 🖼️ Assets Setup
**Required filenames** (as coded by default):
- `assets/player1.png` — player ship
- `assets/enemy1.png`  — enemy ship(s)
- `assets/boss1.png`   — boss ship

> If your PNGs are in a subfolder (e.g., `assets/sprites/player1.png`), either **move them to `assets/` root**, or update the asset paths in your loader to include the subfolder prefix (e.g., `"sprites/player1.png"`). In LibGDX’s `Internal` file handling, paths are relative to the `assets/` root.

Optional: add explosion spritesheets if you have them; the game will fall back gracefully if none are found.

---

## 🏗️ Build & Run

### Prereqs
- **JDK 17+** recommended
- Android Studio or IntelliJ IDEA with Gradle
- LibGDX project generated with **core** + **lwjgl3/desktop** (+ **html** if you want web build)

### Desktop (Downloadable ZIP for itch)
**Gradle Task** (from project root):
- **Windows PowerShell / Android Studio Terminal**
  ```powershell
  .\gradlew.bat lwjgl3:dist     # or: desktop:dist for older templates
  ```
- **macOS/Linux**
  ```bash
  ./gradlew lwjgl3:dist         # or: desktop:dist
  ```

**Output**: `lwjgl3/build/distributions/<project>-<version>.zip`  
Unzip and run the provided `.bat` (Windows) or `.sh` (macOS/Linux).

> Ensure the desktop module includes assets as resources. In `lwjgl3/build.gradle`:
> ```gradle
> plugins { id "application" }
> mainClassName = "com.rgs.galaxyinvaders.lwjgl3.Lwjgl3Launcher" // adjust if different
> sourceSets.main.resources.srcDirs = [ "../assets" ]
> ```

### HTML (Play-in-browser on itch.io)
1) Build:
   ```powershell
   .\gradlew.bat html:dist
   ```
   Output: `html/build/dist/` (contains `index.html`).
2) **Zip the contents** of `html/build/dist` (so `index.html` is at the **root** of the zip).
3) On itch.io:
   - **Kind:** HTML
   - Upload the zip
   - Check **“This file will be played in the browser.”**
   - Optional: enable **Fullscreen**, or size the embed to **800×480**.

**Local test** (optional):
```powershell
cd .\html\build\dist
python -m http.server 8000
# Open http://localhost:8000
```

### Android (APK)
Android Studio → **Build > Generate Signed Bundle / APK…** → APK → `android` module → Release.  
Upload the APK to itch (users will sideload).

---

## 💡 Tuning & Configuration
Open `GameScreen.java` (and `Assets.java` if present). Common knobs:
- `BULLET_HITBOX_SCALE` — lower = easier (e.g., `0.50f`).
- `BOSS_BULLET_LIMIT` — lower = fewer on-screen boss bullets.
- `ATTACK_SLOW_FACTOR` — higher = slower boss attack cadence.
- Pattern unlocks per boss level — see `Boss.update()` block.
- Power-up drop rates — tweak `maybeDrop(...)` probabilities.

Viewport: `FitViewport(800, 480)` gives predictable gameplay area with letterboxing on resize.

---

## 🧪 Known Good Settings
- **First boss**: only 2 or 5 projectiles per volley, 2 patterns total (Aimed, Fan).
- **Second boss+**: up to 3 patterns (adds Spiral), still at a slower global cadence.
- **Shield**: 50% transparent ring when active.
- **Hit feedback**: red sprite tint on impact (no screen flash).

---

## ❗ Troubleshooting
- **`gradlew` not recognized** (PowerShell):
  ```powershell
  cd "E:\Development\Galaxy Invaders"
  .\gradlew.bat html:dist   # or lwjgl3:dist
  ```
  If `gradlew.bat` is missing, run the **wrapper** task in Android Studio (Gradle tool window → *build setup* → `wrapper`) or install Gradle and run `gradle wrapper` once.
- **Black screen / missing textures (desktop)**: assets not packaged. Confirm:
  ```gradle
  sourceSets.main.resources.srcDirs = [ "../assets" ]
  ```
  Rebuild with `lwjgl3:dist`.
- **itch HTML upload shows blank page**: you zipped the folder instead of its **contents**, or an asset path is wrong. Rebuild with `clean html:dist` and re-zip the **contents** of `dist`.
- **macOS won’t launch**: user may need to right‑click → Open the first time; signing/notarization is optional for itch.
- **Java not found**: either bundle a small JRE alongside the desktop ZIP (place as `/jre` and edit the script), or ask users to install Java 17+.

---

## 🔗 Links
- **itch.io page**: (https://conradlesiak.itch.io/galaxy-invaders)
- **Releases / downloads**: use the `lwjgl3:dist` ZIP
- **Web build**: upload `html/build/dist` **contents** as an HTML zip

---

## 📝 License
This project is released under the **MIT License** (or pick your license).

```
MIT License

Copyright (c) 2025 YOUR NAME

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

---

## 🙌 Credits
- Code & game design: **Conrad Lesiak**
- Framework: **LibGDX**
- Icons/sprites: (`player1.png`, `enemy1.png`, `boss1.png`)
- Thanks to the LibGDX community

---

## 🗓️ Changelog (example)
- **v0.3** — Boss pattern caps per level; slower cadence; ESC pause/menu; smaller hitboxes.
- **v0.2** — Power‑ups from boss hits; boss HP halved; scoring on boss hit.
- **v0.1** — First playable: player/enemy/boss, bullets, collisions, viewport, menu.
