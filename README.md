# ផ្ទះ ឆ្នោតយួន (Home Lottery)

Android app for Khmer, Vietnam, MinhNam, Thai and ThaiSou lottery results, daily 2D/3D predictions and live result streams.

- **Package:** `com.homelottery.app`
- **Current version:** 1.4.0 (version code 12)
- **Google Play:** https://play.google.com/store/apps/details?id=com.homelottery.app
- **Privacy Policy:** https://kblkhmer.github.io/app/privacy-policy.html
- **Terms of Service:** https://kblkhmer.github.io/app/terms-of-service.html

## Features

- **5 lotteries** — Khmer, Vietnam, MinhNam, Thai and ThaiSou results, every draw time of the day
- **Latest results** — the home screen shows the most recent draw for each lottery
- **Date navigation** — browse past results with the previous/next date switcher
- **Predictions** — daily 2D and 3D numbers
- **YouTube Live** — the channel's live result streams inside the app, with a back button
- **Result alerts** — notification when a new draw is published (can be turned off in the menu)
- **Update alerts** — Google Play in-app updates plus a background check about every 3 hours
- **Ads** — popup, home banner and "Live Slots" ads between draw cards, managed from the admin panel
- **Khmer and English** — language switch in the toolbar; Hanuman and Kantumruy Pro fonts
- **Modern UI** — edge-to-edge layout with the brand gradient header and colour lottery tiles

## Requirements

- **Android Studio** (latest stable, with support for Android Gradle Plugin 9.4)
- **JDK 17 or newer** — Android Studio's bundled JBR works
- **Android SDK** — compileSdk / targetSdk 36, minSdk 24 (Android 7.0)
- **Gradle** 9.7.1 (downloaded automatically by the wrapper)

## Build & Run Locally

```bash
./gradlew assembleDebug
./gradlew installDebug   # install on a connected device or emulator
```

On Windows, use `gradlew.bat`. If `JAVA_HOME` points to a missing JDK, set it to Android Studio's JBR for the build:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

## Build a Release for Google Play

### 1. Signing setup (once per computer)

The release build is signed with the upload key at `app/keystore/upload.jks`. The keystore and its passwords are **never committed** — `app/keystore/` and `local.properties` are git-ignored.

Put the keystore at `app/keystore/upload.jks`, then add the passwords to `local.properties` in the project root:

```properties
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=lottery-kh
RELEASE_KEY_PASSWORD=your_key_password
```

### 2. Bump the version

In `app/build.gradle.kts`, increase `versionCode` (Play rejects a code it has seen before) and update `versionName`.

### 3. Build the bundle

```bash
./gradlew bundleRelease
```

| Output | Path | Upload to |
|--------|------|-----------|
| App bundle | `app/build/outputs/bundle/release/app-release.aab` | Play Console release |
| R8 mapping | `app/build/outputs/mapping/release/mapping.txt` | Play Console (deobfuscation file) |

Always test the release build before uploading (`./gradlew assembleRelease`, then install `app/build/outputs/apk/release/app-release.apk`) — R8 only runs on release builds.

> `gradle.properties` sets `android.r8.strictFullModeForKeepRules=false`. With AGP 9's strict keep rules, WorkManager/Room constructors are removed and the release build crashes on launch.

### 4. Publish

1. Play Console → your app → **Testing → Internal testing** → **Create new release**
2. Upload the `.aab` (and `mapping.txt` when asked), add release notes, roll out
3. Test on a real phone from the opt-in link, then **promote the release to Production**

| Track | Testers | Review required | Visibility |
|-------|---------|-----------------|------------|
| Internal | Up to 100 | No | Opt-in link only |
| Closed | Custom list | Yes | Opt-in link only |
| Open | Anyone | Yes | Play Store listing |
| Production | Everyone | Yes | Public |

## Project Structure

```
├── app/
│   ├── build.gradle.kts                  # App build config (version, signing, dependencies)
│   ├── proguard-rules.pro                # R8 keep rules
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── ic_launcher-playstore.png     # 512×512 Play Store icon
│       ├── java/com/lotterykh/app/
│       │   ├── BaseActivity.kt           # Locale, edge-to-edge and system bar insets
│       │   ├── MainActivity.kt           # Home: header, lottery tiles, latest results
│       │   ├── LotteryActivity.kt        # Results screen with date switcher
│       │   ├── LotteryFragment.kt        # Draw cards + "Live Slots" ads
│       │   ├── LotteryPagerAdapter.kt    # ViewPager2 adapter
│       │   ├── PredictionsActivity.kt    # 2D/3D predictions
│       │   ├── PredictionNumberAdapter.kt
│       │   ├── YoutubeLiveActivity.kt    # In-app YouTube Live (WebView)
│       │   ├── AdPopupDialog.kt          # Popup ad
│       │   ├── ImageLoaders.kt           # Shared Coil image loader
│       │   ├── LocaleHelper.kt           # Khmer / English switch
│       │   ├── PlayIntegrityHelper.kt    # Play Integrity token
│       │   ├── data/                     # LotteryApi, AdsApi, PredictionsApi
│       │   ├── model/                    # Data classes
│       │   ├── results/                  # New-result alerts (worker, notifier, prefs)
│       │   └── update/                   # Play in-app updates and update alerts
│       └── res/                          # Layouts, drawables, fonts, strings (km + en)
├── docs/                                 # Privacy Policy & Terms (GitHub Pages)
├── build.gradle.kts                      # Root build file (AGP 9.4.1)
├── settings.gradle.kts
└── gradle.properties
```

## API Endpoints

| Endpoint | Description |
|----------|-------------|
| `https://bkhlottery.cc/admin/api/result.php?date=YYYY-MM-DD` | Results for all lotteries (`kh`, `tc`, `th`, `mn`, `ts`) |
| `https://bkhlottery.cc/admin/api/ads.php` | Ads (`popup`, `banner`, `banner2` = Live Slots) |
| `https://bkhlottery.cc/admin/api/predictions.php` | Daily 2D/3D predictions |

All endpoints must return JSON. If the host's bot protection ("One moment, please…" page) is turned on for `/admin/api/` or `/uploads/ads/`, the app shows no data — keep those paths excluded.
