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

import android.os.Build
import android.util.Log

private const val TAG = "AGDeviceUtils"

/**
 * Enum representing supported NPU hardware vendors.
 */
enum class NpuVendor {
  QUALCOMM,
  MEDIATEK,
  GOOGLE_TENSOR,
  UNKNOWN
}

/**
 * Enum representing Qualcomm HTP (Hexagon Tensor Processor) versions.
 * Maps to specific Snapdragon chipset generations.
 */
enum class QualcommHtpVersion(val version: Int) {
  V69(69),  // Snapdragon 888, 778G
  V73(73),  // Snapdragon 8 Gen 1, 8+ Gen 1, 7 Gen 1
  V75(75),  // Snapdragon 8 Gen 2
  V79(79),  // Snapdragon 8 Gen 3, 8s Gen 3
  UNKNOWN(0)
}

/**
 * Data class representing detected NPU information.
 */
data class NpuInfo(
  val vendor: NpuVendor,
  val qualcommHtpVersion: QualcommHtpVersion = QualcommHtpVersion.UNKNOWN,
  val libraryName: String = "",
  val isSupported: Boolean = false
)

/**
 * Utility object for detecting device hardware and NPU capabilities.
 */
object DeviceUtils {

  /**
   * Detects the NPU vendor and capabilities of the current device.
   */
  fun detectNpu(): NpuInfo {
    val hardware = Build.HARDWARE.lowercase()
    val board = Build.BOARD.lowercase()
    val soc = getSystemProperty("ro.soc.model").lowercase()
    val chipset = getSystemProperty("ro.hardware.chipname").lowercase()

    Log.d(TAG, "Device detection - Hardware: $hardware, Board: $board, SoC: $soc, Chipset: $chipset")

    // Check for Google Tensor
    if (isGoogleTensor(hardware, board, soc)) {
      Log.d(TAG, "Detected Google Tensor NPU")
      return NpuInfo(
        vendor = NpuVendor.GOOGLE_TENSOR,
        libraryName = "libLiteRtDispatch_GoogleTensor.so",
        isSupported = true
      )
    }

    // Check for Qualcomm Snapdragon
    val htpVersion = detectQualcommHtpVersion(hardware, board, soc, chipset)
    if (htpVersion != QualcommHtpVersion.UNKNOWN) {
      Log.d(TAG, "Detected Qualcomm NPU with HTP v${htpVersion.version}")
      return NpuInfo(
        vendor = NpuVendor.QUALCOMM,
        qualcommHtpVersion = htpVersion,
        libraryName = "libLiteRtDispatch_Qualcomm.so",
        isSupported = true
      )
    }

    // Check for MediaTek
    if (isMediaTek(hardware, board, soc)) {
      Log.d(TAG, "Detected MediaTek NPU")
      return NpuInfo(
        vendor = NpuVendor.MEDIATEK,
        libraryName = "libLiteRtDispatch_Mediatek.so",
        isSupported = true
      )
    }

    Log.d(TAG, "No supported NPU detected")
    return NpuInfo(vendor = NpuVendor.UNKNOWN, isSupported = false)
  }

  /**
   * Check if the device has a Google Tensor chip.
   */
  private fun isGoogleTensor(hardware: String, board: String, soc: String): Boolean {
    return hardware.contains("tensor") ||
        board.contains("tensor") ||
        soc.contains("tensor") ||
        soc.contains("gs") || // Google Silicon (gs101, gs201, gs301)
        Build.SOC_MODEL.lowercase().contains("tensor")
  }

  /**
   * Detect the Qualcomm HTP version based on SoC information.
   */
  private fun detectQualcommHtpVersion(
    hardware: String,
    board: String,
    soc: String,
    chipset: String
  ): QualcommHtpVersion {
    val combined = "$hardware $board $soc $chipset ${Build.SOC_MODEL.lowercase()}"

    // Snapdragon 8 Gen 3 / 8s Gen 3 -> HTP v79
    if (combined.contains("sm8650") || combined.contains("sm8635") ||
        combined.contains("8 gen 3") || combined.contains("8s gen 3")) {
      return QualcommHtpVersion.V79
    }

    // Snapdragon 8 Gen 2 -> HTP v75
    if (combined.contains("sm8550") || combined.contains("8 gen 2")) {
      return QualcommHtpVersion.V75
    }

    // Snapdragon 8 Gen 1 / 8+ Gen 1 / 7 Gen 1 -> HTP v73
    if (combined.contains("sm8450") || combined.contains("sm8475") ||
        combined.contains("sm7450") || combined.contains("8 gen 1") ||
        combined.contains("7 gen 1")) {
      return QualcommHtpVersion.V73
    }

    // Snapdragon 888 / 778G -> HTP v69
    if (combined.contains("sm8350") || combined.contains("sm7325") ||
        combined.contains("888") || combined.contains("778")) {
      return QualcommHtpVersion.V69
    }

    // Generic Qualcomm check
    if (combined.contains("qcom") || combined.contains("qualcomm") ||
        combined.contains("snapdragon") || combined.contains("sdm") ||
        combined.contains("msm")) {
      // Default to v73 for unknown Qualcomm devices (most common modern version)
      Log.w(TAG, "Unknown Qualcomm SoC, defaulting to HTP v73")
      return QualcommHtpVersion.V73
    }

    return QualcommHtpVersion.UNKNOWN
  }

  /**
   * Check if the device has a MediaTek chip.
   */
  private fun isMediaTek(hardware: String, board: String, soc: String): Boolean {
    val combined = "$hardware $board $soc ${Build.SOC_MODEL.lowercase()}"
    return combined.contains("mt") ||
        combined.contains("mediatek") ||
        combined.contains("dimensity")
  }

  /**
   * Get a system property value.
   */
  private fun getSystemProperty(key: String): String {
    return try {
      val clazz = Class.forName("android.os.SystemProperties")
      val method = clazz.getMethod("get", String::class.java)
      method.invoke(null, key) as? String ?: ""
    } catch (e: Exception) {
      Log.w(TAG, "Failed to get system property $key: ${e.message}")
      ""
    }
  }

  /**
   * Get a human-readable description of the device's NPU capabilities.
   */
  fun getNpuDescription(): String {
    val npuInfo = detectNpu()
    return when {
      !npuInfo.isSupported -> "No NPU detected"
      npuInfo.vendor == NpuVendor.GOOGLE_TENSOR -> "Google Tensor NPU"
      npuInfo.vendor == NpuVendor.QUALCOMM -> "Qualcomm HTP v${npuInfo.qualcommHtpVersion.version}"
      npuInfo.vendor == NpuVendor.MEDIATEK -> "MediaTek APU"
      else -> "Unknown NPU"
    }
  }
}
