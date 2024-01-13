package com.christophroyer.avybuddy

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.net.toUri
import kotlinx.coroutines.CancellationException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.concurrent.thread

class SoundMeasurement(private val context: Context) {
    private var player: MediaPlayer
    private var debugPlayer: MediaPlayer? = null
    private var recorder: AudioRecord? = null
    var measurementRunning by mutableStateOf(false)


    init {
        player = MediaPlayer.create(context, R.raw.sweep)

        player.setOnCompletionListener {
            stopMeasurement()
        }

    }

    fun startMeasurement() {
        if (startRecorder()) {
            startPlayer()
        }
    }

    fun stopMeasurement() {
        stopPlayer()
        stopRecorder()
    }

    fun startPlayer() {

        try {
            player.start()
            measurementRunning = true
        } catch (e: CancellationException) {
            stopMeasurement()
            throw e
        }
    }

    fun stopPlayer() {
        player.stop()
        player.prepare()
        measurementRunning = false
    }

    fun startRecorder(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            requestPermissions(context as Activity, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            return false
        }

        if (recorder == null) {
            recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                512
            )
        }

        recorder?.startRecording()
        thread(true) {writeAudioDataToFile()}

        return true
    }

    fun writeAudioDataToFile() {
        val outputFile = File(context.cacheDir, "recordedFile.wav")
        val outputStream: FileOutputStream?
        val data = ByteArray(256)
        try {
            outputStream = FileOutputStream(outputFile)
        } catch (e: IOException) {
            e.printStackTrace()
            return
        }

        while(recorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            val read = recorder!!.read(data, 0, data.size)
            try {
                outputStream.write(data, 0, read)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        try {
            outputStream.flush()
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    fun stopRecorder() {
        recorder?.stop()
        recorder?.release()
    }

    fun playDebug() {
        try {
            File(context.cacheDir, "recordedFile.wav").also {
                MediaPlayer.create(context, it.toUri())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        debugPlayer!!.setOnCompletionListener {
            debugPlayer!!.stop()
            debugPlayer!!.release()
        }

        debugPlayer!!.start()
    }
}