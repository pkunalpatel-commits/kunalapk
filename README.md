# OI Spike Detector — Android APK

Android version of the NSE Option **OI Spike Detector** (same core logic as the desktop app).

## Features

- Live OI spike scan via **Dhan Option Chain API**
- Candle interval **1 / 3 / 5 / 15 / 30** min (+ optional classic 1m/5m/10m)
- **Telegram** alerts (multiple chat IDs)
- Foreground service (keeps scanning in background)
- Android notifications on spikes
- Settings: thresholds, ATM range, symbols (NIFTY / BANKNIFTY / FINNIFTY)

## Build APK with GitHub (recommended)

1. Create a new GitHub repository.
2. Upload this entire `oi_spike_android` folder as the repo root (or push via git).
3. On GitHub → **Actions** → run workflow **Build APK** (or push to `main`).
4. When the job finishes → open the run → **Artifacts** → download **OI-Spike-Detector-debug**.
5. Install the `.apk` on your phone (enable *Install from unknown sources*).

### Push from your PC

```bash
cd oi_spike_android
git init
git add .
git commit -m "OI Spike Detector Android"
git branch -M main
git remote add origin https://github.com/YOUR_USER/YOUR_REPO.git
git push -u origin main
```

Then open **Actions** and download the APK artifact.

## Build locally (Android Studio)

1. Install [Android Studio](https://developer.android.com/studio).
2. **File → Open** → select this `oi_spike_android` folder.
3. Wait for Gradle sync.
4. **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. APK path: `app/build/outputs/apk/debug/app-debug.apk`.

## First run on phone

1. Open app → **Settings**.
2. Enter **Dhan Client ID** + **Access Token**.
3. Enter **Telegram** bot token + chat ID (optional).
4. Set thresholds / candle interval / symbols → **Save**.
5. Back to main screen → **Start**.
6. Allow notifications if asked.

Keep the phone awake or disable battery optimization for this app so the scanner is not killed.

## Historical Scan (on phone)

1. Open app → **Historical Scan**
2. Symbol, From/To dates (`YYYY-MM-DD`), candle interval, ATM ±, CE/PE
3. **Scan Historical** — uses same 1m/5m/10m thresholds as Settings
4. Results list + log

## Not in this APK (desktop-only for now)

- Order book L3 depth
- Market profile / depth history

## Security

API tokens are stored in app private SharedPreferences. Do not share your APK with tokens already saved.
