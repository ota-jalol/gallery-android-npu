# NPU Support in Google AI Edge Gallery

## Overview

This document describes the Neural Processing Unit (NPU) support added to the Google AI Edge Gallery application. NPU support enables hardware-accelerated machine learning inference on compatible Android devices with dedicated neural processing hardware.

## What is an NPU?

A Neural Processing Unit (NPU) is a specialized processor designed specifically for machine learning tasks. NPUs provide:

- **Faster Inference**: Optimized hardware for neural network operations
- **Lower Power Consumption**: More efficient than CPU/GPU for ML workloads
- **Dedicated Processing**: Offloads ML tasks from main CPU/GPU

## Implementation Details

### Accelerator Options

The app now supports three accelerator types:

1. **CPU** - Software-based inference on the device's CPU
2. **GPU** - Hardware acceleration using the device's GPU
3. **NPU** - Hardware acceleration using the device's NPU (via NNAPI)

### Technical Architecture

NPU support is implemented through Android's Neural Networks API (NNAPI), which provides a unified interface for hardware acceleration across different device manufacturers.

#### Code Changes

The following components have been updated to support NPU:

1. **Accelerator Enum** (`Types.kt`)
   - Added `NPU` accelerator type alongside CPU and GPU

2. **LLM Chat Model Helper** (`LlmChatModelHelper.kt`)
   - Maps NPU accelerator to `Backend.NNAPI` for LiteRT execution
   - Enables NPU backend selection when initializing models

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

#### Selecting NPU Acceleration

When running a model:

1. Open the model configuration (gear icon)
2. Select "NPU" from the "Choose accelerator" options
3. The model will be reinitialized to use NPU acceleration

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

NPU support requires:

- **Android API Level 27+** for basic NNAPI support
- **Android API Level 29+** recommended for optimal NNAPI features
- **Compatible Hardware**: Device must have an NPU or support NNAPI acceleration
  - Many modern Android devices from manufacturers like Samsung, Huawei, Google, Qualcomm, and MediaTek include NPU support

### Checking NPU Availability

The Android NNAPI will automatically fallback to CPU if NPU is not available. Users can verify performance improvements by:

1. Running inference with GPU accelerator
2. Running inference with NPU accelerator
3. Comparing inference speed metrics displayed in the app

## Performance Considerations

### When to Use NPU

NPU acceleration is most beneficial for:

- Large Language Models (LLMs)
- Frequent inference operations
- Battery-constrained scenarios
- Sustained ML workloads

### NPU vs GPU vs CPU

| Accelerator | Performance | Power Efficiency | Compatibility |
|------------|-------------|------------------|---------------|
| CPU | Baseline | Low | Universal |
| GPU | High (graphics) | Medium | Most devices |
| NPU | High (ML tasks) | High | Modern devices |

## Limitations

1. **Device Support**: Not all Android devices have NPU hardware
2. **NNAPI Variations**: Performance may vary across device manufacturers
3. **Model Compatibility**: Some model architectures may not be fully optimized for NPU
4. **Fallback Behavior**: If NPU is unavailable, NNAPI may fall back to CPU/GPU

## Troubleshooting

### NPU Selection Not Improving Performance

If NPU selection doesn't improve performance:

1. **Check Device Support**: Your device may not have NPU hardware
2. **Verify NNAPI Version**: Update to latest Android version if possible
3. **Try Different Models**: Some models may not be optimized for NPU
4. **Compare Metrics**: Check TTFT and decode speed in the app

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
