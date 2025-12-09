# NPU Runtime Libraries

This directory contains vendor-specific NPU (Neural Processing Unit) runtime libraries for AI inference acceleration.

## Structure

```
jniLibs/
├── qualcomm_runtime_v69/   # Snapdragon 888, 778G (HTP v69)
├── qualcomm_runtime_v73/   # Snapdragon 8 Gen 1, 8+ Gen 1, 7 Gen 1 (HTP v73)
├── qualcomm_runtime_v75/   # Snapdragon 8 Gen 2 (HTP v75)
├── qualcomm_runtime_v79/   # Snapdragon 8 Gen 3, 8s Gen 3 (HTP v79)
├── google_tensor_runtime/  # Google Tensor (Pixel 6, 7, 8, 9)
└── mediatek_runtime/       # MediaTek Dimensity
```

## Libraries

### Qualcomm (QNN - Qualcomm Neural Network)
- `libQnnSystem.so` - QNN system library
- `libQnnHtp.so` - Hexagon Tensor Processor runtime
- `libQnnHtpVxxStub.so` - HTP version-specific stub
- `libQnnHtpVxxSkel.so` - HTP version-specific skeleton
- `libLiteRtDispatch_Qualcomm.so` - LiteRT dispatch for Qualcomm

### Google Tensor
- `libLiteRtDispatch_GoogleTensor.so` - LiteRT dispatch for Tensor TPU

### MediaTek
- `libLiteRtDispatch_Mediatek.so` - LiteRT dispatch for MediaTek APU

## How it works

1. `DeviceUtils.kt` detects the device chipset and NPU vendor
2. `NpuRuntimeLoader.kt` loads the appropriate native libraries at runtime
3. `LlmChatModelHelper.kt` uses the loaded NPU backend for inference

## Fallback chain

NPU → NNAPI → GPU → CPU

If NPU libraries fail to load, the app falls back to standard GPU/CPU inference.
