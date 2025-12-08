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

**Important Note**: Direct NPU acceleration for Large Language Models (LLMs) is not yet available in the LiteRT library. When NPU is selected, the app currently uses GPU acceleration as a fallback, which provides the best available performance. This is a framework limitation, not a device limitation.

### Accelerator Options

The app supports three accelerator types:

1. **CPU** - Software-based inference on the device's CPU
2. **GPU** - Hardware acceleration using the device's GPU  
3. **NPU** - Currently falls back to GPU (NPU support pending LiteRT updates)

### Technical Architecture

The NPU option is implemented at the UI and configuration level, with backend fallback to GPU:

#### Code Changes

The following components have been updated to support NPU selection:

1. **Accelerator Enum** (`Types.kt`)
   - Added `NPU` accelerator type alongside CPU and GPU

2. **LLM Chat Model Helper** (`LlmChatModelHelper.kt`)
   - NPU selection currently maps to GPU backend (with logging)
   - Ready for future LiteRT NPU support
   - Falls back gracefully when NPU is selected

3. **Model Manager** (`ModelManagerViewModel.kt`)
   - Handles NPU accelerator mapping from imported model configurations
   - Supports parsing NPU from model allowlist JSON files

4. **Model Import Dialog** (`ModelImportDialog.kt`)
   - Added NPU as an option in the accelerator selection UI
   - Users can now select NPU as a compatible accelerator for imported models

5. **Model Allowlist** (`ModelAllowlist.kt`)
   - Parses "npu" from JSON configuration files
   - Enables model definitions to specify NPU compatibility

## Usage

### For Users

> **Note**: Currently, selecting NPU will use GPU acceleration due to LiteRT library limitations. This provides the best available performance until native NPU support is added to LiteRT.

#### Selecting NPU Acceleration

When running a model:

1. Open the model configuration (gear icon)
2. Select "NPU" from the "Choose accelerator" options
3. The model will be reinitialized (currently using GPU backend)
4. Future LiteRT updates will enable true NPU acceleration without app changes

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

1. **LiteRT Framework Limitation**: NPU acceleration for LLMs is not yet available in LiteRT library
   - Currently falls back to GPU when NPU is selected
   - GPU provides the best available performance for LLMs
   - Infrastructure ready for future LiteRT NPU support
2. **Device Support**: Not all Android devices have NPU hardware
3. **Future Updates**: True NPU acceleration will be enabled when LiteRT adds support
4. **Current Performance**: Selecting NPU currently equals GPU performance

## Troubleshooting

### NPU Selection Not Improving Performance

NPU selection currently uses GPU backend due to LiteRT limitations:

1. **Current Behavior**: NPU selection maps to GPU acceleration
2. **Expected Performance**: Same as GPU (best available for LLMs)
3. **Check Logs**: Look for "NPU selected but not directly supported" message
4. **Future Updates**: Will automatically use NPU when LiteRT adds support

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
