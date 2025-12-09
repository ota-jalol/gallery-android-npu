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

package com.google.ai.edge.gallery.ui.llmchat

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.ai.edge.gallery.common.NpuLoadResult
import com.google.ai.edge.gallery.common.NpuRuntimeLoader
import com.google.ai.edge.gallery.common.NpuVendor
import com.google.ai.edge.gallery.common.cleanUpMediapipeTaskErrorMessage
import com.google.ai.edge.gallery.data.Accelerator
import com.google.ai.edge.gallery.data.ConfigKeys
import com.google.ai.edge.gallery.data.DEFAULT_MAX_TOKEN
import com.google.ai.edge.gallery.data.DEFAULT_TEMPERATURE
import com.google.ai.edge.gallery.data.DEFAULT_TOPK
import com.google.ai.edge.gallery.data.DEFAULT_TOPP
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import java.io.ByteArrayOutputStream
import java.util.concurrent.CancellationException

private const val TAG = "AGLlmChatModelHelper"

typealias ResultListener = (partialResult: String, done: Boolean) -> Unit

typealias CleanUpListener = () -> Unit

data class LlmModelInstance(
  val engine: Engine,
  var conversation: Conversation,
  val backendLabel: String,
)

object LlmChatModelHelper {
  // Indexed by model name.
  private val cleanUpListeners: MutableMap<String, CleanUpListener> = mutableMapOf()

  fun initialize(
    context: Context,
    model: Model,
    supportImage: Boolean,
    supportAudio: Boolean,
    onDone: (String) -> Unit,
    systemMessage: Message? = null,
    tools: List<Any> = listOf(),
  ) {
    // Prepare options.
    val maxTokens =
      model.getIntConfigValue(key = ConfigKeys.MAX_TOKENS, defaultValue = DEFAULT_MAX_TOKEN)
    val topK = model.getIntConfigValue(key = ConfigKeys.TOPK, defaultValue = DEFAULT_TOPK)
    val topP = model.getFloatConfigValue(key = ConfigKeys.TOPP, defaultValue = DEFAULT_TOPP)
    val temperature =
      model.getFloatConfigValue(key = ConfigKeys.TEMPERATURE, defaultValue = DEFAULT_TEMPERATURE)
    val accelerator =
      model.getStringConfigValue(key = ConfigKeys.ACCELERATOR, defaultValue = Accelerator.GPU.label)
    Log.d(TAG, "Initializing...")
    val shouldEnableImage = supportImage
    val shouldEnableAudio = supportAudio
    Log.d(TAG, "Enable image: $shouldEnableImage, enable audio: $shouldEnableAudio")

    // Try to load vendor-specific NPU runtime
    val npuLoadResult = NpuRuntimeLoader.loadNpuRuntime(context)
    val npuBackend = resolveNpuBackend(npuLoadResult)
    val nnapiBackend = resolveNnapiBackendOrNull()

    val backendCandidates: List<Backend> =
      when (accelerator) {
        Accelerator.CPU.label -> listOf(Backend.CPU)
        Accelerator.GPU.label -> listOf(Backend.GPU, Backend.CPU)
        Accelerator.NPU.label -> {
          // Priority: Vendor NPU -> NNAPI -> GPU -> CPU
          listOfNotNull(npuBackend, nnapiBackend, Backend.GPU, Backend.CPU)
        }
        else -> listOf(Backend.CPU)
      }.distinct()

    val modelPath = model.getPath(context = context)

    var lastError: Exception? = null
    for (candidateBackend in backendCandidates) {
      try {
        val engineConfig =
          EngineConfig(
            modelPath = modelPath,
            backend = candidateBackend,
            visionBackend = if (shouldEnableImage) Backend.GPU else null, // must be GPU for Gemma 3n
            audioBackend = if (shouldEnableAudio) Backend.CPU else null, // must be CPU for Gemma 3n
            maxNumTokens = maxTokens,
            cacheDir =
              if (modelPath.startsWith("/data/local/tmp"))
                context.getExternalFilesDir(null)?.absolutePath
              else null,
          )

        val engine = Engine(engineConfig)
        engine.initialize()

        val conversation =
          engine.createConversation(
            ConversationConfig(
              samplerConfig =
                SamplerConfig(
                  topK = topK,
                  topP = topP.toDouble(),
                  temperature = temperature.toDouble(),
                ),
              systemMessage = systemMessage,
              tools = tools,
            )
          )
        val backendLabel = buildBackendLabel(candidateBackend, nnapiBackend, npuBackend, npuLoadResult)
        Log.d(TAG, "Using backend: $backendLabel")
        model.instance =
          LlmModelInstance(engine = engine, conversation = conversation, backendLabel = backendLabel)
        onDone("")
        return
      } catch (e: Exception) {
        lastError = e
        Log.w(TAG, "Failed to initialize backend $candidateBackend: ${e.message}")
      }
    }

    onDone(cleanUpMediapipeTaskErrorMessage(lastError?.message ?: "Unknown error"))
  }

  private fun resolveNnapiBackendOrNull(): Backend? {
    // First try to find NNAPI by reflection
    val possibleNames = listOf("NNAPI", "NPU", "DSP", "HEXAGON")
    
    for (name in possibleNames) {
      try {
        val backendField = Backend::class.java.getDeclaredField(name)
        val backend = backendField.get(null) as? Backend
        if (backend != null) {
          Log.d(TAG, "Found NPU-like backend: $name")
          return backend
        }
      } catch (e: NoSuchFieldException) {
        // Try next name
      } catch (e: Exception) {
        Log.w(TAG, "Failed to access backend $name: ${e.message}")
      }
    }
    
    // Log all available backends for debugging
    try {
      val backends = Backend::class.java.enumConstants
      if (backends != null) {
        Log.d(TAG, "Available backends: ${backends.joinToString { it.toString() }}")
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to list backends: ${e.message}")
    }
    
    Log.w(TAG, "No NNAPI/NPU backend found in LiteRT")
    return null
  }

  /**
   * Resolve vendor-specific NPU backend based on load result.
   */
  private fun resolveNpuBackend(loadResult: NpuLoadResult): Backend? {
    if (loadResult !is NpuLoadResult.Success) {
      Log.d(TAG, "NPU runtime not loaded: $loadResult")
      return null
    }

    // Try to get vendor-specific backend from LiteRT
    val possibleNames = when (loadResult.vendor) {
      NpuVendor.QUALCOMM -> listOf("QNN_HTP", "QNN", "HEXAGON", "DSP", "NPU", "NNAPI")
      NpuVendor.GOOGLE_TENSOR -> listOf("GOOGLE_TENSOR", "TENSOR", "TPU", "NPU", "NNAPI")
      NpuVendor.MEDIATEK -> listOf("MEDIATEK_APU", "APU", "NPU", "NNAPI")
      else -> listOf("NPU", "NNAPI")
    }

    for (name in possibleNames) {
      try {
        val backendField = Backend::class.java.getDeclaredField(name)
        val backend = backendField.get(null) as? Backend
        if (backend != null) {
          Log.d(TAG, "Vendor NPU backend found: $name for ${loadResult.vendor}")
          return backend
        }
      } catch (e: NoSuchFieldException) {
        // Try next name
      } catch (e: Exception) {
        Log.w(TAG, "Failed to access vendor backend $name: ${e.message}")
      }
    }

    Log.w(TAG, "Vendor backend not found for ${loadResult.vendor}, available backends logged above")
    return null
  }

  private fun buildBackendLabel(
    backend: Backend,
    nnapiBackend: Backend?,
    npuBackend: Backend?,
    npuLoadResult: NpuLoadResult
  ): String {
    return when {
      backend == Backend.CPU -> Accelerator.CPU.label
      backend == Backend.GPU -> Accelerator.GPU.label
      nnapiBackend != null && backend === nnapiBackend -> "${Accelerator.NPU.label} (NNAPI)"
      npuBackend != null && backend === npuBackend -> {
        when (npuLoadResult) {
          is NpuLoadResult.Success -> "${Accelerator.NPU.label} (${npuLoadResult.description})"
          else -> Accelerator.NPU.label
        }
      }
      else -> backend.toString()
    }
  }

  fun resetConversation(
    model: Model,
    supportImage: Boolean,
    supportAudio: Boolean,
    systemMessage: Message? = null,
    tools: List<Any> = listOf(),
  ) {
    try {
      Log.d(TAG, "Resetting conversation for model '${model.name}'")

      val instance = model.instance as LlmModelInstance? ?: return
      instance.conversation.close()

      val engine = instance.engine
      val topK = model.getIntConfigValue(key = ConfigKeys.TOPK, defaultValue = DEFAULT_TOPK)
      val topP = model.getFloatConfigValue(key = ConfigKeys.TOPP, defaultValue = DEFAULT_TOPP)
      val temperature =
        model.getFloatConfigValue(key = ConfigKeys.TEMPERATURE, defaultValue = DEFAULT_TEMPERATURE)
      val shouldEnableImage = supportImage
      val shouldEnableAudio = supportAudio
      Log.d(TAG, "Enable image: $shouldEnableImage, enable audio: $shouldEnableAudio")

      val newConversation =
        engine.createConversation(
          ConversationConfig(
            samplerConfig =
              SamplerConfig(
                topK = topK,
                topP = topP.toDouble(),
                temperature = temperature.toDouble(),
              ),
            systemMessage = systemMessage,
            tools = tools,
          )
        )
      instance.conversation = newConversation

      Log.d(TAG, "Resetting done")
    } catch (e: Exception) {
      Log.d(TAG, "Failed to reset conversation", e)
    }
  }

  fun cleanUp(model: Model, onDone: () -> Unit) {
    if (model.instance == null) {
      return
    }

    val instance = model.instance as LlmModelInstance

    try {
      instance.conversation.close()
    } catch (e: Exception) {
      Log.e(TAG, "Failed to close the conversation: ${e.message}")
    }

    try {
      instance.engine.close()
    } catch (e: Exception) {
      Log.e(TAG, "Failed to close the engine: ${e.message}")
    }

    val onCleanUp = cleanUpListeners.remove(model.name)
    if (onCleanUp != null) {
      onCleanUp()
    }
    model.instance = null

    onDone()
    Log.d(TAG, "Clean up done.")
  }

  fun runInference(
    model: Model,
    input: String,
    resultListener: ResultListener,
    cleanUpListener: CleanUpListener,
    onError: (message: String) -> Unit = {},
    images: List<Bitmap> = listOf(),
    audioClips: List<ByteArray> = listOf(),
  ) {
    val instance = model.instance as LlmModelInstance

    // Set listener.
    if (!cleanUpListeners.containsKey(model.name)) {
      cleanUpListeners[model.name] = cleanUpListener
    }

    val conversation = instance.conversation

    val contents = mutableListOf<Content>()
    for (image in images) {
      contents.add(Content.ImageBytes(image.toPngByteArray()))
    }
    for (audioClip in audioClips) {
      contents.add(Content.AudioBytes(audioClip))
    }
    // add the text after image and audio for the accurate last token
    if (input.trim().isNotEmpty()) {
      contents.add(Content.Text(input))
    }

    conversation.sendMessageAsync(
      Message.of(contents),
      object : MessageCallback {
        override fun onMessage(message: Message) {
          resultListener(message.toString(), false)
        }

        override fun onDone() {
          resultListener("", true)
        }

        override fun onError(throwable: Throwable) {
          if (throwable is CancellationException) {
            Log.i(TAG, "The inference is cancelled.")
            resultListener("", true)
          } else {
            Log.e(TAG, "onError", throwable)
            onError("Error: ${throwable.message}")
          }
        }
      },
    )
  }

  private fun Bitmap.toPngByteArray(): ByteArray {
    val stream = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
  }
}
