# Monitor AnedetApp logcat with useful filters (PowerShell version)

Write-Host "🔍 Monitoring AnedetApp..." -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

# Clear logcat first
adb logcat -c

# Monitor with filters
adb logcat `
  CameraScreen:D `
  CameraViewModel:D `
  ConjunctivaSegmentor:D `
  AnemiaClassifier:D `
  InferenceRepository:D `
  MediaStoreUtils:D `
  AndroidRuntime:E `
  System.err:E `
  "*:S"
