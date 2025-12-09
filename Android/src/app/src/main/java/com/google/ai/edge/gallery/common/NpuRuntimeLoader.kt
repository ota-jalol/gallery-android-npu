/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery.common

import android.content.Context
import android.util.Log

private const val TAG = "AGNpuRuntimeLoader"

/**
 * Result of NPU runtime loading attempt.
 */
sealed class NpuLoadResult {
  data class Success(val vendor: NpuVendor, val description: String) : NpuLoadResult()
  data class NotSupported(val reason: String) : NpuLoadResult()
  data class Failed(val error: String) : NpuLoadResult()
}

/**
 * Handles loading of vendor-specific NPU runtime libraries.
 */
object NpuRuntimeLoader {
  
  private var loadedVendor: NpuVendor? = null
  private var loadResult: NpuLoadResult? = null

  /**
   * Attempts to load the appropriate NPU runtime for this device.
   * Returns cached result if already attempted.
   */
  fun loadNpuRuntime(context: Context): NpuLoadResult {
    // Return cached result
    loadResult?.let { return it }

    val npuInfo = DeviceUtils.detectNpu()

    if (!npuInfo.isSupported) {
      val result = NpuLoadResult.NotSupported("Device does not have a supported NPU")
      loadResult = result
      return result
    }

    val result = when (npuInfo.vendor) {
      NpuVendor.QUALCOMM -> loadQualcommRuntime(npuInfo)
      NpuVendor.GOOGLE_TENSOR -> loadGoogleTensorRuntime()
      NpuVendor.MEDIATEK -> loadMediaTekRuntime()
      NpuVendor.UNKNOWN -> NpuLoadResult.NotSupported("Unknown NPU vendor")
    }

    loadResult = result
    if (result is NpuLoadResult.Success) {
      loadedVendor = npuInfo.vendor
    }

    return result
  }

  /**
   * Load Qualcomm HTP runtime libraries.
   * Libraries are named with version suffix: libQnnSystemV73.so, libQnnHtpV73.so, etc.
   */
  private fun loadQualcommRuntime(npuInfo: NpuInfo): NpuLoadResult {
    val version = npuInfo.qualcommHtpVersion.version
    if (version == 0) {
      return NpuLoadResult.NotSupported("Unknown Qualcomm HTP version")
    }

    return try {
      // Load Qualcomm QNN libraries in order
      // Libraries are renamed with version suffix to avoid conflicts
      val libraries = listOf(
        "QnnSystemV$version",           // libQnnSystemV73.so
        "QnnHtpV$version",              // libQnnHtpV73.so
        "QnnHtpV${version}Stub",        // libQnnHtpV73Stub.so
        "QnnHtpV${version}Skel",        // libQnnHtpV73Skel.so
        "LiteRtDispatch_QualcommV$version"  // libLiteRtDispatch_QualcommV73.so
      )

      for (lib in libraries) {
        try {
          System.loadLibrary(lib)
          Log.d(TAG, "Loaded library: lib$lib.so")
        } catch (e: UnsatisfiedLinkError) {
          Log.w(TAG, "Optional library not found: lib$lib.so")
          // Some libraries may be optional, continue loading
        }
      }

      Log.i(TAG, "Qualcomm NPU runtime v$version loaded successfully")
      NpuLoadResult.Success(
        vendor = NpuVendor.QUALCOMM,
        description = "Qualcomm HTP v$version"
      )
    } catch (e: Exception) {
      Log.e(TAG, "Failed to load Qualcomm runtime: ${e.message}")
      NpuLoadResult.Failed("Failed to load Qualcomm NPU runtime: ${e.message}")
    }
  }

  /**
   * Load Google Tensor runtime libraries.
   */
  private fun loadGoogleTensorRuntime(): NpuLoadResult {
    return try {
      System.loadLibrary("LiteRtDispatch_GoogleTensor")
      Log.i(TAG, "Google Tensor NPU runtime loaded successfully")
      NpuLoadResult.Success(
        vendor = NpuVendor.GOOGLE_TENSOR,
        description = "Google Tensor TPU"
      )
    } catch (e: UnsatisfiedLinkError) {
      Log.e(TAG, "Failed to load Google Tensor runtime: ${e.message}")
      NpuLoadResult.Failed("Google Tensor runtime not available: ${e.message}")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to load Google Tensor runtime: ${e.message}")
      NpuLoadResult.Failed("Failed to load Google Tensor NPU runtime: ${e.message}")
    }
  }

  /**
   * Load MediaTek APU runtime libraries.
   */
  private fun loadMediaTekRuntime(): NpuLoadResult {
    return try {
      System.loadLibrary("LiteRtDispatch_Mediatek")
      Log.i(TAG, "MediaTek NPU runtime loaded successfully")
      NpuLoadResult.Success(
        vendor = NpuVendor.MEDIATEK,
        description = "MediaTek APU"
      )
    } catch (e: UnsatisfiedLinkError) {
      Log.e(TAG, "Failed to load MediaTek runtime: ${e.message}")
      NpuLoadResult.Failed("MediaTek runtime not available: ${e.message}")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to load MediaTek runtime: ${e.message}")
      NpuLoadResult.Failed("Failed to load MediaTek NPU runtime: ${e.message}")
    }
  }

  /**
   * Check if NPU runtime is currently loaded.
   */
  fun isNpuRuntimeLoaded(): Boolean = loadedVendor != null

  /**
   * Get the currently loaded NPU vendor, if any.
   */
  fun getLoadedVendor(): NpuVendor? = loadedVendor

  /**
   * Get a description of the load result.
   */
  fun getLoadResultDescription(): String {
    return when (val result = loadResult) {
      is NpuLoadResult.Success -> "NPU: ${result.description}"
      is NpuLoadResult.NotSupported -> "NPU: Not available (${result.reason})"
      is NpuLoadResult.Failed -> "NPU: Failed (${result.error})"
      null -> "NPU: Not initialized"
    }
  }

  /**
   * Reset the loader state (for testing).
   */
  fun reset() {
    loadedVendor = null
    loadResult = null
  }
}
