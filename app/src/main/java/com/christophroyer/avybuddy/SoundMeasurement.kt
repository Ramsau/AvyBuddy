package com.christophroyer.avybuddy

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException

class SoundMeasurement {
    private var mMediaPlayer: MediaPlayer? = null
    var measurementRunning by mutableStateOf(false)
    fun runMeasurement(context: Context) {

        try {
            if (mMediaPlayer == null) {
                mMediaPlayer = MediaPlayer.create(context, R.raw.sweep)
            }
            mMediaPlayer!!.setOnCompletionListener({
                measurementRunning = false
            })
            mMediaPlayer!!.start()
            measurementRunning = true
        } catch (e: CancellationException) {
            mMediaPlayer!!.stop()
            throw e
        }
    }

    fun stopMeasurement() {
        mMediaPlayer!!.stop()
        mMediaPlayer!!.prepare()
        measurementRunning = false
    }
}