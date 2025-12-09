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
   * 
   * Note: Stub and Skel files are NOT loaded directly:
   * - Stub requires libcdsprpc.so (system library, loaded by QNN internally)
   * - Skel runs on Hexagon DSP, not loaded via System.loadLibrary
   * 
   * LiteRtDispatch is loaded by LiteRT runtime, not by us directly.
   */
  private fun loadQualcommRuntime(npuInfo: NpuInfo): NpuLoadResult {
    val version = npuInfo.qualcommHtpVersion.version
    if (version == 0) {
      return NpuLoadResult.NotSupported("Unknown Qualcomm HTP version")
    }

    var loadedCount = 0
    val errors = mutableListOf<String>()

    // Core QNN libraries that we need to load
    // These are the main runtime libraries
    val coreLibraries = listOf(
      "QnnSystemV$version",           // libQnnSystemV73.so - QNN System runtime
      "QnnHtpV$version"               // libQnnHtpV73.so - QNN HTP backend
    )

    for (lib in coreLibraries) {
      try {
        System.loadLibrary(lib)
        Log.d(TAG, "Loaded library: lib$lib.so")
        loadedCount++
      } catch (e: UnsatisfiedLinkError) {
        val msg = e.message ?: "Unknown error"
        Log.w(TAG, "Failed to load lib$lib.so: $msg")
        errors.add("lib$lib.so: $msg")
      }
    }

    return if (loadedCount > 0) {
      Log.i(TAG, "Qualcomm NPU runtime v$version: loaded $loadedCount/${coreLibraries.size} libraries")
      NpuLoadResult.Success(
        vendor = NpuVendor.QUALCOMM,
        description = "Qualcomm HTP v$version ($loadedCount/${coreLibraries.size} libs)"
      )
    } else {
      NpuLoadResult.Failed("No QNN libraries could be loaded: ${errors.joinToString("; ")}")
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
