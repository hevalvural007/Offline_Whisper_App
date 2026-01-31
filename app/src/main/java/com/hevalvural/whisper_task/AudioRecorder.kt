package com.hevalvural.whisper_task

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

class AudioRecorder {

   private val sampleRate = 16000
   private val channelConfig = AudioFormat.CHANNEL_IN_MONO
   private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

   private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

   private var recorder: AudioRecord? = null
   private var isRecording = false

   @SuppressLint("MissingPermission")
   fun startRecording(onAudioData: (ShortArray) -> Unit) {
      if (isRecording) return

      try {
         recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
         )

         recorder?.startRecording()
         isRecording = true

         Thread {
            val buffer = ShortArray(bufferSize)

            while (isRecording) {
               val readSize = recorder?.read(buffer, 0, bufferSize) ?: 0
               if (readSize > 0) {
                  onAudioData(buffer.copyOf(readSize))
               }
            }
         }.start()

      } catch (e: Exception) {
         e.printStackTrace()
      }
   }

   fun stopRecording() {
      isRecording = false
      try {
         recorder?.stop()
         recorder?.release()
      } catch (e: Exception) {
         e.printStackTrace()
      }
      recorder = null
   }
}