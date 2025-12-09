# NPU Runtime Libraries

This directory contains vendor-specific NPU (Neural Processing Unit) runtime libraries for AI inference acceleration.

## Structure

```
jniLibs/
├── merge_libs.sh              # Script to merge libraries into single folder
├── merged/arm64-v8a/          # Output folder with all libraries (generated)
├── qualcomm_runtime_v69/      # Snapdragon 888, 778G (HTP v69)
├── qualcomm_runtime_v73/      # Snapdragon 8 Gen 1, 8+ Gen 1, 7 Gen 1 (HTP v73)
├── qualcomm_runtime_v75/      # Snapdragon 8 Gen 2 (HTP v75)
├── qualcomm_runtime_v79/      # Snapdragon 8 Gen 3, 8s Gen 3 (HTP v79)
├── google_tensor_runtime/     # Google Tensor (Pixel 6, 7, 8, 9)
└── mediatek_runtime/          # MediaTek Dimensity
```

## Library Naming Convention

Libraries are renamed with version suffix to avoid conflicts:

### Qualcomm (per HTP version)
- `libQnnSystemV69.so`, `libQnnSystemV73.so`, `libQnnSystemV75.so`, `libQnnSystemV79.so`
- `libQnnHtpV69.so`, `libQnnHtpV73.so`, `libQnnHtpV75.so`, `libQnnHtpV79.so`
- `libQnnHtpV69Stub.so`, `libQnnHtpV73Stub.so`, etc.
- `libQnnHtpV69Skel.so`, `libQnnHtpV73Skel.so`, etc.
- `libLiteRtDispatch_QualcommV69.so`, `libLiteRtDispatch_QualcommV73.so`, etc.

### Google Tensor
- `libLiteRtDispatch_GoogleTensor.so`

### MediaTek
- `libLiteRtDispatch_Mediatek.so`

## Build Process

1. `merge_libs.sh` is run during CI/CD to merge all libraries into `merged/arm64-v8a/`
2. Gradle picks up libraries from `merged/` folder
3. Each library is versioned separately to support all HTP versions

## How it works at runtime

1. `DeviceUtils.kt` detects the device chipset and HTP version
2. `NpuRuntimeLoader.kt` loads only the libraries matching detected HTP version
3. `LlmChatModelHelper.kt` uses the loaded NPU backend for inference

## Fallback chain

NPU → NNAPI → GPU → CPU

If NPU libraries fail to load, the app falls back to standard GPU/CPU inference.
