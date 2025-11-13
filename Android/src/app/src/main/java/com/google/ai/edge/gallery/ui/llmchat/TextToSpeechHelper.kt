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
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

private const val TAG = "AGTextToSpeechHelper"

/**
 * Helper class to manage Text-to-Speech functionality for AI chat responses.
 */
class TextToSpeechHelper(
  private val context: Context,
  private val onInitialized: (Boolean) -> Unit = {},
  private val onSpeakingStateChanged: (Boolean) -> Unit = {}
) {
  private var tts: TextToSpeech? = null
  private var isInitialized = false
  private var isSpeaking = false

  init {
    initializeTTS()
  }

  private fun initializeTTS() {
    tts = TextToSpeech(context) { status ->
      if (status == TextToSpeech.SUCCESS) {
        val result = tts?.setLanguage(Locale.US)
        isInitialized = result != TextToSpeech.LANG_MISSING_DATA && 
                       result != TextToSpeech.LANG_NOT_SUPPORTED
        
        if (isInitialized) {
          Log.d(TAG, "TextToSpeech initialized successfully")
          setupUtteranceListener()
        } else {
          Log.e(TAG, "TextToSpeech language not supported")
        }
        onInitialized(isInitialized)
      } else {
        Log.e(TAG, "TextToSpeech initialization failed")
        onInitialized(false)
      }
    }
  }

  private fun setupUtteranceListener() {
    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
      override fun onStart(utteranceId: String?) {
        isSpeaking = true
        onSpeakingStateChanged(true)
        Log.d(TAG, "Started speaking: $utteranceId")
      }

      override fun onDone(utteranceId: String?) {
        isSpeaking = false
        onSpeakingStateChanged(false)
        Log.d(TAG, "Finished speaking: $utteranceId")
      }

      override fun onError(utteranceId: String?) {
        isSpeaking = false
        onSpeakingStateChanged(false)
        Log.e(TAG, "Error speaking: $utteranceId")
      }
    })
  }

  /**
   * Speaks the given text using TTS.
   * @param text The text to speak
   * @param utteranceId Unique identifier for this utterance
   */
  fun speak(text: String, utteranceId: String = "chat_message") {
    if (!isInitialized) {
      Log.w(TAG, "TTS not initialized, cannot speak")
      return
    }

    if (text.isEmpty()) {
      Log.w(TAG, "Empty text, nothing to speak")
      return
    }

    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
  }

  /**
   * Stops any ongoing speech.
   */
  fun stop() {
    if (isSpeaking) {
      tts?.stop()
      isSpeaking = false
      onSpeakingStateChanged(false)
      Log.d(TAG, "Stopped speaking")
    }
  }

  /**
   * Checks if TTS is currently speaking.
   */
  fun isSpeaking(): Boolean = isSpeaking

  /**
   * Checks if TTS is initialized and ready to use.
   */
  fun isReady(): Boolean = isInitialized

  /**
   * Releases TTS resources. Should be called when done using TTS.
   */
  fun shutdown() {
    stop()
    tts?.shutdown()
    tts = null
    isInitialized = false
    Log.d(TAG, "TTS shutdown")
  }
}
