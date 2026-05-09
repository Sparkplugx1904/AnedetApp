# Build Instructions - Anemia Detector Android App

## Prerequisites

1. **Android Studio** (Latest stable version - Arctic Fox or newer)
2. **JDK 17** (Required for Gradle 8.x)
3. **Android SDK**:
   - Minimum SDK: API 30 (Android 11)
   - Target SDK: API 35 (Android 15)
   - Compile SDK: API 35
4. **Git** (for version control)

## Setup Steps

### 1. Clone the Repository

```bash
git clone <repository-url>
cd AnedetApp
```

### 2. Copy TFLite Models

**CRITICAL:** The app will not work without these models!

Copy the following models from the `AnedetAI` folder:

#### Segmentation Model:
```bash
# Source
Conjunctiva Segmentation\Models\best_int8.tflite

# Destination
app/src/main/assets/models/segments/best_int8.tflite
```

#### Classification Model:
```bash
# Source
AnedetAI/Anemia Classify/Models/yolo26m-CLAHE/tflite/best_float32.tflite

# Destination
app/src/main/assets/models/classify/best_float32.tflite
```

**Windows PowerShell:**
```powershell
Copy-Item "Conjunctiva Segmentation\Models\best_int8.tflite" -Destination "app/src/main/assets/models/segments/best_int8.tflite"

Copy-Item "AnedetAI/Anemia Classify/Models/yolo26m-CLAHE/tflite/best_float32.tflite" -Destination "app/src/main/assets/models/classify/best_float32.tflite"
```

### 3. Open Project in Android Studio

1. Launch Android Studio
2. Select "Open an Existing Project"
3. Navigate to the `AnedetApp` folder
4. Click "OK"
5. Wait for Gradle sync to complete

### 4. Verify Dependencies

Check that all dependencies are downloaded:
- CameraX
- TensorFlow Lite
- OpenCV for Android
- Jetpack Compose
- Hilt
- Room
- DataStore

If Gradle sync fails, try:
```bash
./gradlew clean
./gradlew build --refresh-dependencies
```

### 5. Build the App

#### Debug Build (for testing):
```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

#### Release Build (for distribution):
```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

### 6. Install on Device

#### Via Android Studio:
1. Connect Android device via USB
2. Enable USB Debugging on device
3. Click "Run" button (green play icon)
4. Select your device

#### Via Command Line:
```bash
# Install debug APK
./gradlew installDebug

# Or manually with adb
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Device Requirements

### Minimum Requirements:
- Android 11 (API 30) or higher
- Camera (rear camera recommended)
- 2GB RAM minimum
- 100MB free storage

### Recommended:
- Android 12 (API 31) or higher
- 4GB RAM or more
- Good lighting conditions for camera

## Testing

### Run Unit Tests:
```bash
./gradlew test
```

### Run Instrumented Tests:
```bash
./gradlew connectedAndroidTest
```

### Manual Testing Checklist:
- [ ] Camera preview works at ≥30 FPS
- [ ] Segmentation overlay appears when conjunctiva detected
- [ ] Single capture mode works (tap camera button)
- [ ] Live inference warning dialog appears
- [ ] Live inference mode works (updates every 1 second)
- [ ] Results sheet shows both scores
- [ ] Save functionality works
- [ ] History screen displays saved examinations
- [ ] Settings screen allows language change
- [ ] Onboarding appears on first launch only
- [ ] Dark mode works correctly
- [ ] All three languages work (ID, EN, TH)

## Troubleshooting

### Issue: Gradle sync fails
**Solution:**
```bash
./gradlew clean
./gradlew --refresh-dependencies
```

### Issue: OpenCV initialization failed
**Solution:**
- Check that OpenCV dependency is correct in `build.gradle.kts`
- Verify version: `com.quickbirdstudios:opencv:4.5.3.0`

### Issue: Model not found error
**Solution:**
- Verify models are in correct locations:
  - `app/src/main/assets/models/segments/best_int8.tflite`
  - `app/src/main/assets/models/classify/best_float32.tflite`
- Check file sizes (segmentation ~2-5MB, classification ~20-50MB)

### Issue: Camera permission denied
**Solution:**
- Go to device Settings → Apps → Anemia Detector → Permissions
- Enable Camera permission

### Issue: App crashes on launch
**Solution:**
1. Check Logcat for error messages
2. Verify all dependencies are installed
3. Clean and rebuild: `./gradlew clean assembleDebug`
4. Check that minSdk matches device Android version

### Issue: Low FPS or slow inference
**Solution:**
- Test on a more powerful device
- Ensure device is not in power-saving mode
- Close other apps running in background
- Check that GPU delegate is NOT enabled (CPU only per spec)

## Performance Optimization

### For Debug Builds:
- Disable R8/ProGuard for faster builds
- Use `assembleDebug` instead of `assembleRelease`

### For Release Builds:
- Enable ProGuard/R8 minification
- Use `assembleRelease` with signing config
- Test on multiple devices

## APK Signing (for Release)

### Generate Keystore:
```bash
keytool -genkey -v -keystore anemia-detector.keystore -alias anemia-key -keyalg RSA -keysize 2048 -validity 10000
```

### Sign APK:
```bash
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 -keystore anemia-detector.keystore app/build/outputs/apk/release/app-release-unsigned.apk anemia-key
```

### Verify Signature:
```bash
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk
```

## Distribution

### Internal Testing:
1. Build release APK
2. Share APK file directly
3. Users must enable "Install from Unknown Sources"

### Google Play Store (if needed):
1. Create signed AAB (Android App Bundle):
   ```bash
   ./gradlew bundleRelease
   ```
2. Upload to Play Console
3. Follow Play Store guidelines

## Project Structure

```
AnedetApp/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── assets/
│   │   │   │   └── models/          # TFLite models (MUST BE ADDED)
│   │   │   ├── java/
│   │   │   │   └── com/example/anemiadetector/
│   │   │   │       ├── data/        # Data layer
│   │   │   │       ├── di/          # Dependency injection
│   │   │   │       ├── domain/      # Use cases
│   │   │   │       ├── ml/          # ML models & preprocessing
│   │   │   │       ├── ui/          # UI screens
│   │   │   │       └── utils/       # Utilities
│   │   │   ├── res/                 # Resources
│   │   │   └── AndroidManifest.xml
│   │   └── test/                    # Unit tests
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── CLAUDE.md                        # Full specification
├── IMPLEMENTATION_STATUS.md         # Implementation progress
└── BUILD_INSTRUCTIONS.md            # This file
```

## Next Steps After Build

1. **Test thoroughly** on multiple devices
2. **Profile performance** using Android Profiler
3. **Optimize** if needed (memory, CPU usage)
4. **Add analytics** (optional)
5. **Prepare documentation** for users
6. **Create user manual** in 3 languages

## Support

For issues or questions:
1. Check CLAUDE.md for specification details
2. Check IMPLEMENTATION_STATUS.md for known issues
3. Review Logcat for error messages
4. Check GitHub issues (if applicable)

## License

[Add license information here]

## Contributors

[Add contributor information here]
