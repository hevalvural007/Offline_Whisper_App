package com.hevalvural.whisper_task

class WhisperLib {

    companion object {
        init {
            System.loadLibrary("whisper_task")
        }
    }


    external fun initContext(modelPath: String): Long
    external fun freeContext(contextPtr: Long)
    external fun fullTranscribe(contextPtr: Long, audioData: FloatArray): String
}