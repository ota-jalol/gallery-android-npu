# NPU Support in Google AI Edge Gallery

## Overview

This document describes the Neural Processing Unit (NPU) support added to the Google AI Edge Gallery application. NPU support enables hardware-accelerated machine learning inference on compatible Android devices with dedicated neural processing hardware.

## What is an NPU?

A Neural Processing Unit (NPU) is a specialized processor designed specifically for machine learning tasks. NPUs provide:

- **Faster Inference**: Optimized hardware for neural network operations
- **Lower Power Consumption**: More efficient than CPU/GPU for ML workloads
- **Dedicated Processing**: Offloads ML tasks from main CPU/GPU

## Implementation Details

### Current Status

**NNAPI Detection**: The app now attempts to use NNAPI backend for NPU acceleration. The implementation uses reflection to detect if NNAPI is available in the LiteRT library:

- If NNAPI backend is found: NPU acceleration via NNAPI is enabled
- If NNAPI is not available: Falls back to GPU (best available performance)

This approach ensures:
- Forward compatibility with future LiteRT versions that add NNAPI support
- Graceful degradation to GPU when NNAPI is unavailable
- Detailed logging for debugging and troubleshooting

### Accelerator Options

The app supports three accelerator types:

1. **CPU** - Software-based inference on the device's CPU
2. **GPU** - Hardware acceleration using the device's GPU  
3. **NPU** - Attempts NNAPI, falls back to GPU if unavailable

### Technical Architecture

The NPU implementation uses runtime reflection to detect NNAPI backend availability:

#### Code Changes

The following components have been updated to support NPU selection:

1. **Accelerator Enum** (`Types.kt`)
   - Added `NPU` accelerator type alongside CPU and GPU

2. **LLM Chat Model Helper** (`LlmChatModelHelper.kt`)
   - Uses reflection to detect NNAPI backend in LiteRT
   - Attempts to use NNAPI when NPU is selected
   - Falls back to GPU if NNAPI unavailable
   - Comprehensive logging for each scenario

3. **Model Manager** (`ModelManagerViewModel.kt`)
   - Handles NPU accelerator mapping from imported model configurations
   - Supports parsing NPU from model allowlist JSON files

4. **Model Import Dialog** (`ModelImportDialog.kt`)
   - Added NPU as an option in the accelerator selection UI
   - Users can now select NPU as a compatible accelerator for imported models

5. **Model Allowlist** (`ModelAllowlist.kt`)
   - Parses "npu" from JSON configuration files
   - Enables model definitions to specify NPU compatibility

6. **Dependencies** (`libs.versions.toml`, `build.gradle.kts`)
   - Added TensorFlow Lite 2.14.0 with full NNAPI support
   - Includes GPU delegate and support libraries

## Usage

### For Users

> **Note**: The app attempts to use NNAPI for NPU acceleration. Check the logs to see if NNAPI is available in your LiteRT version. If not available, GPU acceleration is used automatically.

#### Selecting NPU Acceleration

When running a model:

1. Open the model configuration (gear icon)
2. Select "NPU" from the "Choose accelerator" options
3. The model will be reinitialized
4. Check logs to confirm if NNAPI backend was used or if GPU fallback occurred
5. If NNAPI is available, true NPU acceleration will be enabled

#### Importing Models with NPU Support

When importing a custom model:

1. Open the model import dialog
2. Configure model settings
3. Under "Compatible accelerators", select NPU along with other accelerators
4. The model will be available with NPU acceleration option

### For Developers

#### Defining Models with NPU Support

In JSON model allowlist files:

```json
{
  "name": "Example Model",
  "defaultConfig": {
    "accelerators": "cpu,gpu,npu"
  }
}
```

#### Programmatically Setting Accelerators

```kotlin
val model = Model(
  name = "My Model",
  configs = createLlmChatConfigs(
    accelerators = listOf(
      Accelerator.CPU,
      Accelerator.GPU,
      Accelerator.NPU
    )
  )
)
```

## Device Compatibility

> **Current Status**: NPU option is available for selection, but uses GPU backend due to LiteRT framework limitations.

Future requirements for true NPU acceleration:

- **Android API Level 27+** for basic NNAPI support
- **Android API Level 29+** recommended for optimal NNAPI features
- **Compatible Hardware**: Device with NPU or NNAPI-compatible accelerator
  - Many modern Android devices from manufacturers like Samsung, Huawei, Google, Qualcomm, and MediaTek include NPU support
- **LiteRT Update**: Requires future LiteRT library with NPU backend support

### Current Behavior

The app currently uses GPU backend when NPU is selected. This provides:
- Best available performance for LLM inference
- Consistent behavior across all devices
- Ready infrastructure for future NPU support
- No performance penalty from NPU selection

## Performance Considerations

### When to Use NPU

NPU acceleration is most beneficial for:

- Large Language Models (LLMs)
- Frequent inference operations
- Battery-constrained scenarios
- Sustained ML workloads

### NPU vs GPU vs CPU (Current Implementation)

| Accelerator | Performance | Power Efficiency | Compatibility | Current Status |
|------------|-------------|------------------|---------------|----------------|
| CPU | Baseline | Low | Universal | ✅ Available |
| GPU | High | Medium | Most devices | ✅ Available |
| NPU | High (future) | High (future) | Modern devices | ⚠️ Uses GPU backend |

**Note**: NPU currently provides GPU-level performance. True NPU acceleration pending LiteRT support.

## Limitations

1. **LiteRT NNAPI Availability**: NNAPI backend may not be available in current LiteRT version
   - App uses reflection to detect availability
   - Falls back to GPU automatically if unavailable
   - Check logs for "NPU acceleration enabled via NNAPI" message
2. **Device Support**: Not all Android devices have NPU hardware
3. **NNAPI Compatibility**: Some LLM architectures may have limited NNAPI optimization
4. **Fallback Behavior**: GPU provides excellent performance when NNAPI unavailable

## Troubleshooting

### Checking NPU Status

To verify if NPU/NNAPI is being used:

1. **Enable Logging**: Use `adb logcat` or Android Studio Logcat
2. **Filter for TAG**: Search for "AGLlmChatModelHelper"
3. **Look for Messages**:
   - ✅ "NPU acceleration enabled via NNAPI backend" = NPU working
   - ⚠️ "NNAPI backend not available" = Using GPU fallback
   - ⚠️ "Error accessing NNAPI backend" = Check LiteRT version

### NPU Not Working

If you see fallback messages:

1. **LiteRT Version**: Current version may not include NNAPI backend
2. **Update LiteRT**: Wait for future LiteRT releases with NNAPI support
3. **GPU Performance**: GPU fallback provides excellent performance
4. **Device Compatibility**: Ensure device has NPU hardware

### Model Initialization Errors

If models fail to initialize with NPU:

1. Switch to GPU or CPU accelerator
2. Check device compatibility
3. Verify model format is supported by NNAPI
4. Review error logs for specific NNAPI errors

## Technical References

- [Android Neural Networks API (NNAPI)](https://developer.android.com/ndk/guides/neuralnetworks)
- [TensorFlow Lite NNAPI Delegate](https://www.tensorflow.org/lite/performance/nnapi)
- [LiteRT Documentation](https://ai.google.dev/edge/litert)
- [MediaPipe Tasks](https://developers.google.com/mediapipe)

## Future Enhancements

Potential improvements for NPU support:

- Automatic accelerator selection based on device capabilities
- Performance profiling and recommendations
- Extended NPU-specific optimizations
- Support for quantized models optimized for NPU
- Per-layer execution analytics

## Contributing

To contribute to NPU support:

1. Test on various devices with NPU hardware
2. Report compatibility issues
3. Suggest performance optimizations
4. Share benchmark results

## License

This feature is part of the Google AI Edge Gallery project and is licensed under the Apache License, Version 2.0.
