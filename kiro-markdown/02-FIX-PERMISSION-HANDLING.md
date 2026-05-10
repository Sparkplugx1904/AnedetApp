# Fix Permission Handling - Stuck di Permission Screen

## Masalah
Aplikasi stuck di halaman "Izin Kamera Diperlukan" meskipun user sudah memberikan permission dari Settings.

## Root Cause
1. **Accompanist Permissions Library** tidak reliable untuk detect permission changes saat app resume dari Settings
2. `permissionsState.allPermissionsGranted` tidak trigger recompose setelah user kembali dari Settings
3. Tombol "Buka Pengaturan" hanya memanggil `launchMultiplePermissionRequest()` lagi, bukan membuka Settings

## Solusi

### 1. Ganti Accompanist dengan Manual Permission Handling
**Sebelum:**
```kotlin
val permissionsState = rememberMultiplePermissionsState(
    permissions = listOf(Manifest.permission.CAMERA, ...)
)
```

**Sesudah:**
```kotlin
var hasPermissions by remember { 
    mutableStateOf(PermissionUtils.hasAllPermissions(context))
}

val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
) { permissions ->
    hasPermissions = permissions.values.all { it }
}
```

### 2. Tambahkan Lifecycle Observer untuk Re-check Permission
```kotlin
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            Log.d("CameraScreen", "App resumed, re-checking permissions")
            hasPermissions = PermissionUtils.hasAllPermissions(context)
        }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
        lifecycleOwner.lifecycle.removeObserver(observer)
    }
}
```

### 3. Perbaiki PermissionDeniedScreen
**Tambahkan 2 tombol:**
1. **"Izinkan Akses Kamera"** - Request permission langsung
2. **"Buka Pengaturan"** - Membuka App Settings

```kotlin
Button(onClick = onRequestPermission) {
    Text(stringResource(R.string.permission_request))
}

OutlinedButton(
    onClick = {
        context.startActivity(
            PermissionUtils.createAppSettingsIntent(context)
        )
    }
) {
    Text(stringResource(R.string.permission_open_settings))
}
```

## Flow Sekarang

1. **First time** → Permission dialog muncul otomatis
2. **User tolak** → Muncul screen dengan 2 tombol
3. **User klik "Izinkan Akses Kamera"** → Request permission lagi
4. **User klik "Buka Pengaturan"** → Buka App Settings
5. **User enable permission di Settings** → Kembali ke app
6. **ON_RESUME triggered** → Re-check permission → `hasPermissions = true`
7. **Recompose** → Tampilkan CameraPreview ✅

## Testing
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

1. Buka app → Tolak permission
2. Klik "Buka Pengaturan"
3. Enable Camera permission
4. Kembali ke app
5. ✅ App harus langsung masuk ke CameraScreen

## Files Changed
- `app/src/main/java/com/example/anemiadetector/ui/camera/CameraScreen.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-en/strings.xml`

## Dependencies Removed
- ❌ `com.google.accompanist:accompanist-permissions` (tidak reliable)
- ✅ Menggunakan native Compose `rememberLauncherForActivityResult`
