# NPU Support Implementation Summary

## Overview
This document summarizes the changes made to add Neural Processing Unit (NPU) support to the Google AI Edge Gallery Android application.

## Problem Statement
The task was to add NPU support to the project (translated from Uzbek: "loyihaga npu qo'llab quvatlashini qo'shish kerak o'rganib chiq etiborli bo'l").

## Changes Made

### 1. Core Implementation Files

#### Types.kt
**Location**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/data/Types.kt`

**Change**: Added NPU to the Accelerator enum
```kotlin
enum class Accelerator(val label: String) {
  CPU(label = "CPU"),
  GPU(label = "GPU"),
  NPU(label = "NPU"),  // NEW
}
```

#### LlmChatModelHelper.kt
**Location**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/llmchat/LlmChatModelHelper.kt`

**Change**: Added NPU to NNAPI backend mapping
```kotlin
val preferredBackend =
  when (accelerator) {
    Accelerator.CPU.label -> Backend.CPU
    Accelerator.GPU.label -> Backend.GPU
    Accelerator.NPU.label -> Backend.NNAPI  // NEW
    else -> Backend.CPU
  }
```

**Impact**: When users select NPU accelerator, the LiteRT engine will use NNAPI backend for hardware acceleration.

#### ModelManagerViewModel.kt
**Location**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/modelmanager/ModelManagerViewModel.kt`

**Change**: Added NPU accelerator mapping for imported models
```kotlin
val accelerators: List<Accelerator> =
  info.llmConfig.compatibleAcceleratorsList.mapNotNull { acceleratorLabel ->
    when (acceleratorLabel.trim()) {
      Accelerator.GPU.label -> Accelerator.GPU
      Accelerator.CPU.label -> Accelerator.CPU
      Accelerator.NPU.label -> Accelerator.NPU  // NEW
      else -> null
    }
  }
```

**Impact**: Imported models can now specify NPU as a compatible accelerator.

#### ModelAllowlist.kt
**Location**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/data/ModelAllowlist.kt`

**Change**: Added NPU parsing from JSON configuration
```kotlin
for (item in items) {
  if (item == "cpu") {
    accelerators.add(Accelerator.CPU)
  } else if (item == "gpu") {
    accelerators.add(Accelerator.GPU)
  } else if (item == "npu") {  // NEW
    accelerators.add(Accelerator.NPU)
  }
}
```

**Impact**: Model allowlist JSON files can now specify "npu" in the accelerators field.

#### ModelImportDialog.kt
**Location**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/home/ModelImportDialog.kt`

**Change**: Added NPU to the UI accelerator selection
```kotlin
SegmentedButtonConfig(
  key = ConfigKeys.COMPATIBLE_ACCELERATORS,
  defaultValue = Accelerator.CPU.label,
  options = listOf(
    Accelerator.CPU.label, 
    Accelerator.GPU.label, 
    Accelerator.NPU.label  // NEW
  ),
  allowMultiple = true,
)
```

**Impact**: Users can now select NPU when importing custom models.

### 2. Documentation Files

#### README.md
**Change**: Added NPU support to technology highlights section

#### NPU_SUPPORT.md (NEW)
**Purpose**: Comprehensive documentation covering:
- NPU overview and benefits
- Implementation details
- Usage instructions for users and developers
- Device compatibility information
- Performance considerations
- Troubleshooting guide
- Technical references

#### DEVELOPMENT.md
**Change**: Added reference to NPU support documentation

## Technical Architecture

### NPU Integration Flow

1. **User Selection**: User selects NPU accelerator in model configuration
2. **Backend Mapping**: NPU selection maps to `Backend.NNAPI` in LiteRT
3. **NNAPI Layer**: Android NNAPI provides hardware abstraction
4. **NPU Hardware**: Physical NPU processes inference operations
5. **Fallback**: If NPU unavailable, NNAPI falls back to CPU/GPU

### Compatibility

- **Minimum Android Version**: API 27+ (NNAPI support)
- **Recommended Version**: API 29+ (optimal NNAPI features)
- **Hardware**: Requires device with NPU or NNAPI-compatible accelerator

## Testing Strategy

Since the project doesn't have an existing test infrastructure:
- Manual testing should be performed on devices with NPU support
- Test NPU selection in model configuration UI
- Verify performance improvements with benchmark metrics
- Confirm fallback behavior on devices without NPU

## Benefits

1. **Performance**: Faster inference on NPU-capable devices
2. **Efficiency**: Lower power consumption compared to CPU/GPU
3. **User Choice**: Flexibility to choose optimal accelerator
4. **Future-Ready**: Support for emerging NPU hardware

## Minimal Changes Approach

All changes follow the principle of minimal modification:
- Added only 6 lines of code across 5 files
- No breaking changes to existing functionality
- Backward compatible with existing models and configurations
- Documentation additions don't affect code execution

## Files Modified

1. `Android/src/app/src/main/java/com/google/ai/edge/gallery/data/Types.kt` (+1 line)
2. `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/llmchat/LlmChatModelHelper.kt` (+1 line)
3. `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/modelmanager/ModelManagerViewModel.kt` (+1 line)
4. `Android/src/app/src/main/java/com/google/ai/edge/gallery/data/ModelAllowlist.kt` (+2 lines)
5. `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/home/ModelImportDialog.kt` (+1 line)

## Files Created

1. `NPU_SUPPORT.md` (Comprehensive documentation)
2. `IMPLEMENTATION_SUMMARY.md` (This file)

## Files Updated (Documentation)

1. `README.md` (Added NPU to technology highlights)
2. `DEVELOPMENT.md` (Added reference to NPU documentation)

## Next Steps for Users

1. Update to the latest version with NPU support
2. Test NPU acceleration on compatible devices
3. Compare performance metrics (TTFT, decode speed)
4. Provide feedback on NPU performance
5. Report device-specific compatibility issues

## Conclusion

The NPU support implementation is complete with minimal, focused changes to the codebase. The implementation follows Android best practices by using NNAPI for hardware abstraction and provides comprehensive documentation for users and developers.
