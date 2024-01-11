package com.christophroyer.avybuddy

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer

class SoundMeasurement : Service(), MediaPlayer.OnPreparedListener {
    private var mMediaPlayer: MediaPlayer? = null
    fun startMeasurement() {

                mMediaPlayer = MediaPlayer.create(this, R.raw.sweep);
                mMediaPlayer?.apply {
                    setOnPreparedListener(this@SoundMeasurement)
                    prepareAsync()
                }
        }
    }

    override fun onPrepared(mp: MediaPlayer) {
        mp.start();
    }
}