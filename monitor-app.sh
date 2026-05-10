#!/bin/bash
# Monitor AnedetApp logcat with useful filters

echo "🔍 Monitoring AnedetApp..."
echo "================================"
echo ""

# Clear logcat first
adb logcat -c

# Monitor with filters
adb logcat \
  -s CameraScreen:D \
  -s CameraViewModel:D \
  -s ConjunctivaSegmentor:D \
  -s AnemiaClassifier:D \
  -s InferenceRepository:D \
  -s MediaStoreUtils:D \
  -s AndroidRuntime:E \
  -s System.err:E \
  | grep -v "WorkSourceUtil"
