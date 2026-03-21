# Lottery KH - Cambodian Lottery Results App (លទ្ធផលឆ្នោត)

Android app displaying Khmer lottery results for Vietnam and Malaysia draws.

## Prerequisites

- **Android Studio** (Hedgehog 2023.1+ recommended)
- **JDK 17**
- **Android SDK** (API 34)

## Build & Run Locally

```bash
# Clone/open the project in Android Studio, then:
cd lottery-kh
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug
```

## Build Release AAB (for Google Play)

### Step 1: Generate a Signing Keystore

```bash
keytool -genkeypair -v -keystore app/keystore/release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias lottery-kh
```

You'll be prompted for passwords. **Save these securely ? you cannot recover them.**

### Step 2: Configure Signing

Edit `app/build.gradle.kts` and uncomment the signing config:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("keystore/release.jks")
        storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "your_password"
        keyAlias = "lottery-kh"
        keyPassword = System.getenv("KEY_PASSWORD") ?: "your_password"
    }
}
```

Also uncomment in `buildTypes > release`:
```kotlin
signingConfig = signingConfigs.getByName("release")
```

### Step 3: Build the AAB

```bash
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

---

## Publish to Google Play (Internal Testing - No Review Required)

### One-Time Setup

1. **Create a Google Play Developer Account** at https://play.google.com/console
   - One-time $25 registration fee
   
2. **Create a new app** in Google Play Console:
   - App name: "លទ្ធផលឆ្នោត"
   - Default language: Khmer
   - App type: App
   - Free or Paid: Free

### Upload for Internal Testing (No Review!)

1. Go to **Google Play Console** ? Your App
2. Navigate to **Testing** ? **Internal testing**
3. Click **Create new release**
4. Upload the `app-release.aab` file
5. Add release notes (e.g., "Initial internal test build")
6. Click **Review release** ? **Start rollout to Internal testing**

### Add Internal Testers

1. Go to **Internal testing** ? **Testers** tab
2. Create an email list with tester email addresses
3. Share the **opt-in link** with testers
4. Testers click the link ? install from Play Store

**Internal testing releases are available immediately ? no Google review required!**

### Important Notes

- Internal testing supports up to **100 testers**
- Testers must use the **same Google account** as the email you added
- The app won't appear in Play Store search ? only accessible via the opt-in link
- You can push updates instantly without any review

---

## Testing Tracks Summary

| Track | Testers | Review Required | Visibility |
|-------|---------|----------------|------------|
| **Internal** | Up to 100 | **No** | Opt-in link only |
| Closed | Custom list | Yes | Opt-in link only |
| Open | Anyone | Yes | Play Store listing |
| Production | Everyone | Yes | Public |

## Project Structure

```
lottery-kh/
??? app/
?   ??? build.gradle.kts          # App build config
?   ??? proguard-rules.pro        # ProGuard rules
?   ??? src/main/
?       ??? AndroidManifest.xml
?       ??? java/com/lotterykh/app/
?       ?   ??? MainActivity.kt        # Main entry point
?       ?   ??? LotteryFragment.kt     # Displays lottery draws
?       ?   ??? LotteryPagerAdapter.kt # ViewPager adapter
?       ?   ??? data/
?       ?   ?   ??? SampleData.kt      # Sample lottery data
?       ?   ??? model/
?       ?       ??? LotteryModels.kt   # Data classes
?       ??? res/
?           ??? layout/                # XML layouts
?           ??? values/                # Colors, strings, themes
?           ??? drawable/              # Drawables & icons
??? build.gradle.kts              # Root build file  
??? settings.gradle.kts
??? gradle.properties
```
