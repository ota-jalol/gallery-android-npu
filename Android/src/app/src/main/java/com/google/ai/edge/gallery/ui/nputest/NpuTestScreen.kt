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

package com.google.ai.edge.gallery.ui.nputest

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.common.DeviceUtils
import com.google.ai.edge.gallery.common.NpuLoadResult
import com.google.ai.edge.gallery.common.NpuRuntimeLoader
import com.google.ai.edge.gallery.common.NpuVendor
import com.google.ai.edge.litertlm.Backend

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NpuTestScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var logs by remember { mutableStateOf(listOf<LogEntry>()) }
    var isLoading by remember { mutableStateOf(false) }

    fun addLog(level: LogLevel, message: String) {
        logs = logs + LogEntry(level, message)
    }

    fun runNpuTest() {
        isLoading = true
        logs = emptyList()

        // Device Info
        addLog(LogLevel.INFO, "=== Device Information ===")
        addLog(LogLevel.INFO, "Brand: ${Build.BRAND}")
        addLog(LogLevel.INFO, "Model: ${Build.MODEL}")
        addLog(LogLevel.INFO, "Device: ${Build.DEVICE}")
        addLog(LogLevel.INFO, "Hardware: ${Build.HARDWARE}")
        addLog(LogLevel.INFO, "Board: ${Build.BOARD}")
        addLog(LogLevel.INFO, "SOC Model: ${Build.SOC_MODEL}")
        addLog(LogLevel.INFO, "SOC Manufacturer: ${Build.SOC_MANUFACTURER}")
        addLog(LogLevel.INFO, "Android API: ${Build.VERSION.SDK_INT}")

        // NPU Detection
        addLog(LogLevel.INFO, "")
        addLog(LogLevel.INFO, "=== NPU Detection ===")
        val npuInfo = DeviceUtils.detectNpu()
        addLog(
            if (npuInfo.isSupported) LogLevel.SUCCESS else LogLevel.WARNING,
            "NPU Supported: ${npuInfo.isSupported}"
        )
        addLog(LogLevel.INFO, "Vendor: ${npuInfo.vendor}")
        if (npuInfo.vendor == NpuVendor.QUALCOMM) {
            addLog(LogLevel.INFO, "Qualcomm HTP Version: v${npuInfo.qualcommHtpVersion.version}")
        }
        addLog(LogLevel.INFO, "Library: ${npuInfo.libraryName}")

        // NPU Runtime Loading
        addLog(LogLevel.INFO, "")
        addLog(LogLevel.INFO, "=== NPU Runtime Loading ===")
        NpuRuntimeLoader.reset() // Reset to test fresh
        val loadResult = NpuRuntimeLoader.loadNpuRuntime(context)
        when (loadResult) {
            is NpuLoadResult.Success -> {
                addLog(LogLevel.SUCCESS, "NPU Runtime Loaded Successfully!")
                addLog(LogLevel.SUCCESS, "Vendor: ${loadResult.vendor}")
                addLog(LogLevel.SUCCESS, "Description: ${loadResult.description}")
            }
            is NpuLoadResult.NotSupported -> {
                addLog(LogLevel.WARNING, "NPU Not Supported: ${loadResult.reason}")
            }
            is NpuLoadResult.Failed -> {
                addLog(LogLevel.ERROR, "NPU Loading Failed: ${loadResult.error}")
            }
        }

        // LiteRT Backend Check
        addLog(LogLevel.INFO, "")
        addLog(LogLevel.INFO, "=== LiteRT Backends ===")
        try {
            val backends = Backend::class.java.enumConstants
            if (backends != null) {
                addLog(LogLevel.INFO, "Available backends: ${backends.size}")
                backends.forEach { backend ->
                    addLog(LogLevel.INFO, "  - $backend")
                }
            } else {
                addLog(LogLevel.WARNING, "No backends found (enumConstants is null)")
            }
        } catch (e: Exception) {
            addLog(LogLevel.ERROR, "Failed to list backends: ${e.message}")
        }

        // Check specific backends
        addLog(LogLevel.INFO, "")
        addLog(LogLevel.INFO, "=== Backend Availability ===")
        val backendNames = listOf(
            "CPU", "GPU", "NNAPI", "NPU", "DSP", "HEXAGON",
            "QNN", "QNN_HTP", "GOOGLE_TENSOR", "TENSOR", "TPU",
            "MEDIATEK_APU", "APU"
        )
        backendNames.forEach { name ->
            try {
                val field = Backend::class.java.getDeclaredField(name)
                val backend = field.get(null)
                if (backend != null) {
                    addLog(LogLevel.SUCCESS, "$name: Available ✓")
                } else {
                    addLog(LogLevel.WARNING, "$name: Field exists but null")
                }
            } catch (e: NoSuchFieldException) {
                addLog(LogLevel.DEBUG, "$name: Not found")
            } catch (e: Exception) {
                addLog(LogLevel.ERROR, "$name: Error - ${e.message}")
            }
        }

        // Native Libraries Check
        addLog(LogLevel.INFO, "")
        addLog(LogLevel.INFO, "=== Native Library Loading Test ===")
        
        // Check system libraries first (provided by device, not bundled)
        addLog(LogLevel.INFO, "-- System Libraries (device-provided) --")
        val systemLibs = listOf(
            "cdsprpc" to "Compute DSP RPC (required for NPU)",
            "adsprpc" to "Audio DSP RPC"
        )
        var hasCdsprpc = false
        systemLibs.forEach { (lib, desc) ->
            try {
                System.loadLibrary(lib)
                addLog(LogLevel.SUCCESS, "lib$lib.so: Available ✓ ($desc)")
                if (lib == "cdsprpc") hasCdsprpc = true
            } catch (e: UnsatisfiedLinkError) {
                val msg = e.message?.take(50) ?: "not found"
                addLog(LogLevel.WARNING, "lib$lib.so: Not available")
                addLog(LogLevel.DEBUG, "  → $msg")
            }
        }
        
        if (!hasCdsprpc && npuInfo.vendor == NpuVendor.QUALCOMM) {
            addLog(LogLevel.WARNING, "")
            addLog(LogLevel.WARNING, "⚠️ libcdsprpc.so not accessible!")
            addLog(LogLevel.INFO, "This is a system library required for DSP/NPU.")
            addLog(LogLevel.INFO, "Possible reasons:")
            addLog(LogLevel.INFO, "  - SELinux restrictions")
            addLog(LogLevel.INFO, "  - Vendor namespace isolation")
            addLog(LogLevel.INFO, "  - Library not in accessible path")
        }

        // App-bundled libraries
        addLog(LogLevel.INFO, "")
        addLog(LogLevel.INFO, "-- App Libraries (bundled in APK) --")
        val librariesToTest = if (npuInfo.vendor == NpuVendor.QUALCOMM) {
            val v = npuInfo.qualcommHtpVersion.version
            // Note: Stub and Skel are loaded internally by QNN runtime, not directly
            // Stub requires libcdsprpc.so (system lib)
            // Skel runs on DSP, not loaded via System.loadLibrary
            listOf(
                "QnnSystemV$v" to "QNN System runtime",
                "QnnHtpV$v" to "QNN HTP backend"
            )
        } else if (npuInfo.vendor == NpuVendor.GOOGLE_TENSOR) {
            listOf("LiteRtDispatch_GoogleTensor" to "Google Tensor dispatch")
        } else if (npuInfo.vendor == NpuVendor.MEDIATEK) {
            listOf("LiteRtDispatch_Mediatek" to "MediaTek dispatch")
        } else {
            emptyList()
        }

        librariesToTest.forEach { (lib, desc) ->
            try {
                System.loadLibrary(lib)
                addLog(LogLevel.SUCCESS, "lib$lib.so: Loaded ✓ ($desc)")
            } catch (e: UnsatisfiedLinkError) {
                val msg = e.message ?: ""
                when {
                    msg.contains("not found") -> {
                        val missing = msg.substringAfter("library ").substringBefore(" not found")
                        addLog(LogLevel.ERROR, "lib$lib.so: Missing dependency: $missing")
                    }
                    else -> addLog(LogLevel.ERROR, "lib$lib.so: ${msg.take(60)}...")
                }
            } catch (e: Exception) {
                addLog(LogLevel.ERROR, "lib$lib.so: ${e.message}")
            }
        }
        
        // Info about Stub/Skel
        if (npuInfo.vendor == NpuVendor.QUALCOMM) {
            val v = npuInfo.qualcommHtpVersion.version
            addLog(LogLevel.INFO, "")
            addLog(LogLevel.INFO, "-- DSP Libraries (loaded internally by QNN) --")
            addLog(LogLevel.DEBUG, "libQnnHtpV${v}Stub.so: Requires libcdsprpc.so")
            addLog(LogLevel.DEBUG, "libQnnHtpV${v}Skel.so: Runs on Hexagon DSP")
        }

        // Summary and explanation
        addLog(LogLevel.INFO, "")
        addLog(LogLevel.INFO, "=== Summary ===")
        
        // Check if LiteRT has NPU backend
        val hasNpuBackend = try {
            val backends = Backend::class.java.enumConstants
            backends?.any { it.toString().contains("NPU") || it.toString().contains("QNN") } == true
        } catch (e: Exception) { false }
        
        if (!hasNpuBackend) {
            addLog(LogLevel.WARNING, "LiteRT LLM library only supports CPU & GPU backends")
            addLog(LogLevel.INFO, "NPU acceleration requires:")
            addLog(LogLevel.INFO, "  1. LiteRT with NPU/QNN backend support")
            addLog(LogLevel.INFO, "  2. Model compiled for NPU (QNN/NNAPI)")
            addLog(LogLevel.INFO, "  3. Device with libcdsprpc.so (DSP RPC)")
            addLog(LogLevel.INFO, "")
            addLog(LogLevel.INFO, "Current fallback: GPU > CPU")
        } else {
            addLog(LogLevel.SUCCESS, "LiteRT has NPU backend support!")
        }

        addLog(LogLevel.INFO, "")
        addLog(LogLevel.INFO, "=== Test Complete ===")
        isLoading = false
    }

    LaunchedEffect(Unit) {
        runNpuTest()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NPU Test & Logs") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { runNpuTest() }, enabled = !isLoading) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Logs
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E1E))
                    .padding(8.dp)
            ) {
                val verticalScrollState = rememberScrollState()
                val horizontalScrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(verticalScrollState)
                        .horizontalScroll(horizontalScrollState)
                ) {
                    logs.forEach { entry ->
                        Text(
                            text = entry.message,
                            color = entry.level.color,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (entry.message.startsWith("===")) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

enum class LogLevel(val color: Color) {
    DEBUG(Color(0xFF808080)),
    INFO(Color(0xFFE0E0E0)),
    SUCCESS(Color(0xFF4CAF50)),
    WARNING(Color(0xFFFF9800)),
    ERROR(Color(0xFFF44336))
}

data class LogEntry(
    val level: LogLevel,
    val message: String
)
