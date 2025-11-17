package com.google.ai.edge.gallery.ui.llmchat

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.Intents.intending
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.ai.edge.gallery.GalleryApplication
import com.google.ai.edge.gallery.MainActivity
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.ui.llmchat.LlmChatModelHelper
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import androidx.core.content.FileProvider
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput

@RunWith(AndroidJUnit4::class)
@LargeTest
class LlmChatAudioFlowTest {
  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Before
  fun setup() {
    Intents.init()
    // Set a deterministic fake inference for tests.
    LlmChatModelHelper.testInferenceHook = { model, input, resultListener, _, _, _, _ ->
      // Immediately return a single final result.
      resultListener("Fake AI reply for audio", true)
    }
  }

  @After
  fun tearDown() {
    LlmChatModelHelper.testInferenceHook = null
    Intents.release()
  }

  @Test
  fun audioPickSend_receivesAgentResponse() {
    // Prepare an audio WAV file in the test cache.
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val cacheDir = context.cacheDir
    val audioFile = File(cacheDir, "test_audio.wav")
    if (!audioFile.exists()) {
      // Create a small silent WAV file (PCM 16-bit mono 8kHz) programmatically.
      val wavData = generateSilentWav(sampleRate = 8000, durationSec = 1.0)
      audioFile.writeBytes(wavData)
    }

    val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", audioFile)
    val resultData = Intent().setData(uri)
    intending(hasAction(Intent.ACTION_GET_CONTENT)).respondWith(
      Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)
    )

    // Tap on the "Audio Scribe" task in the home screen.
    composeTestRule.onNodeWithText("Audio Scribe").performClick()

    // Open Add content menu
    composeTestRule.onNodeWithContentDescription(context.getString(R.string.cd_add_content_icon)).performClick()

    // Perform pick wav file - this will be intercepted by Espresso Intents stub above
    composeTestRule.onNodeWithText("Pick wav file").performClick()

    // Now we expect to see the audio preview duration text in the UI e.g. "1.0s".
    // Wait until audio preview shows up.
    composeTestRule.onNodeWithText(containsString("s"), useUnmergedTree = true).assertIsDisplayed()

    // Enter a short text description so send button appears (send button is only shown when there's text)
    val promptDescription = "Audio test"
    composeTestRule.onNodeWithContentDescription(context.getString(R.string.cd_prompt_input_text_field)).performTextInput(promptDescription)

    // Click Send
    composeTestRule.onNodeWithContentDescription(context.getString(R.string.cd_send_prompt_icon)).performClick()

    // Verify the fake AI response shows up.
    composeTestRule.onNodeWithText("Fake AI reply for audio").assertIsDisplayed()
  }

  private fun generateSilentWav(sampleRate: Int, durationSec: Double): ByteArray {
    // WAV header for PCM 16-bit mono
    val channels = 1
    val bitsPerSample = 16
    val byteRate = sampleRate * channels * bitsPerSample / 8
    val totalSamples = (sampleRate * durationSec).toInt()
    val dataSize = totalSamples * channels * bitsPerSample / 8
    val header = java.io.ByteArrayOutputStream()
    fun writeIntLE(value: Int) {
      header.write(value and 0xff)
      header.write((value shr 8) and 0xff)
      header.write((value shr 16) and 0xff)
      header.write((value shr 24) and 0xff)
    }
    fun writeShortLE(value: Int) {
      header.write(value and 0xff)
      header.write((value shr 8) and 0xff)
    }

    header.write("RIFF".toByteArray())
    writeIntLE(36 + dataSize)
    header.write("WAVE".toByteArray())
    header.write("fmt ".toByteArray())
    writeIntLE(16)
    writeShortLE(1)
    writeShortLE(channels)
    writeIntLE(sampleRate)
    writeIntLE(byteRate)
    writeShortLE((channels * bitsPerSample / 8))
    writeShortLE(bitsPerSample)
    header.write("data".toByteArray())
    writeIntLE(dataSize)

    // Data - silence
    val silence = ByteArray(dataSize)
    header.write(silence)
    return header.toByteArray()
  }
}
