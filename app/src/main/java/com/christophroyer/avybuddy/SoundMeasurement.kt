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
import com.himanshoe.charty.line.model.LineData
import kotlinx.coroutines.CancellationException
import org.jtransforms.fft.FloatFFT_1D
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread

class SoundMeasurement(private val context: Context) {
    private val sampleRate = 44100
    private val bufferSize = 512
    private val bitsPerSample = 16
    private val channels = 1
    private val rawCalibrationFile = File(context.cacheDir, "calibration.pcm")
    private val calibrationFile = File(context.cacheDir, "calibration.wav")
    private val rawMeasurementFile = File(context.cacheDir, "measurement.pcm")
    private val measurementFile = File(context.cacheDir, "measurement.wav")

    private var player: MediaPlayer
    private var debugPlayer: MediaPlayer? = null
    private var recorder: AudioRecord? = null

    var measurementRunning by mutableStateOf(false)
    var calibrating = false

    private val chunkSize = 1000
    var results by mutableStateOf(listOf(LineData(1F, 1), LineData(2F,2)))


    init {
        player = MediaPlayer.create(context, R.raw.sweep)

        player.setOnCompletionListener {
            stopMeasurement()
        }

    }

    fun startMeasurement(calibration: Boolean) {
        // set flag whether to write to calibration or result file
        calibrating = calibration
        if (measurementRunning) return
        if (startRecorder()) {
            startPlayer()
        }
    }

    fun stopMeasurement() {
        stopPlayer()
        stopRecorder()
        createWavFile()
        calculateResults()
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
            requestPermissions(context as Activity, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            return false
        }

        assert(bitsPerSample == 16) // else we need to change the encoding
        assert(channels == 1) // else we need to change channels

        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        recorder?.startRecording()
        thread(true) {writeAudioDataToFile()}

        return true
    }

    fun writeAudioDataToFile() {
        val outputFile = if (calibrating) rawCalibrationFile else rawMeasurementFile
        val outputStream: FileOutputStream?
        val data = ByteArray(bufferSize / 2)
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

    private fun createWavHeader(
        fileOutputStream: FileOutputStream,
        totalAudioLen: Long,
    ) {
        val totalDataLen = totalAudioLen + 36
        val byteRate = bitsPerSample * channels * sampleRate / 8
        try {
            val header = ByteArray(44)
            header[0] = 'R'.code.toByte() // RIFF/WAVE header
            header[1] = 'I'.code.toByte()
            header[2] = 'F'.code.toByte()
            header[3] = 'F'.code.toByte()
            header[4] = (totalDataLen and 0xffL).toByte()
            header[5] = (totalDataLen shr 8 and 0xffL).toByte()
            header[6] = (totalDataLen shr 16 and 0xffL).toByte()
            header[7] = (totalDataLen shr 24 and 0xffL).toByte()
            header[8] = 'W'.code.toByte()
            header[9] = 'A'.code.toByte()
            header[10] = 'V'.code.toByte()
            header[11] = 'E'.code.toByte()
            header[12] = 'f'.code.toByte() // 'fmt ' chunk
            header[13] = 'm'.code.toByte()
            header[14] = 't'.code.toByte()
            header[15] = ' '.code.toByte()
            header[16] = 16 // 4 bytes: size of 'fmt ' chunk
            header[17] = 0
            header[18] = 0
            header[19] = 0
            header[20] = 1 // format = 1
            header[21] = 0
            header[22] = 1.toByte() // mono measurement
            header[23] = 0
            header[24] = (sampleRate and 0xff).toByte()
            header[25] = ((sampleRate shr 8) and 0xff).toByte()
            header[26] = ((sampleRate shr 16) and 0xff).toByte()
            header[27] = ((sampleRate shr 24) and 0xff).toByte()
            header[28] = (byteRate and 0xff).toByte()
            header[29] = (byteRate shr 8 and 0xff).toByte()
            header[30] = (byteRate shr 16 and 0xff).toByte()
            header[31] = (byteRate shr 24 and 0xff).toByte()
            header[32] = (2 * 16 / 8).toByte() // block align
            header[33] = 0
            header[34] = bitsPerSample.toByte() // bits per sample
            header[35] = 0
            header[36] = 'd'.code.toByte()
            header[37] = 'a'.code.toByte()
            header[38] = 't'.code.toByte()
            header[39] = 'a'.code.toByte()
            header[40] = (totalAudioLen and 0xffL).toByte()
            header[41] = (totalAudioLen shr 8 and 0xffL).toByte()
            header[42] = (totalAudioLen shr 16 and 0xffL).toByte()
            header[43] = (totalAudioLen shr 24 and 0xffL).toByte()
            fileOutputStream.write(header)
            fileOutputStream.flush()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun createWavFile() {
        val raw = FileInputStream(if (calibrating) rawCalibrationFile else rawMeasurementFile)
        val wav = FileOutputStream(if (calibrating) calibrationFile else measurementFile)
        val data = ByteArray(bufferSize)

        createWavHeader(wav, raw.channel.size())
        while (raw.read(data) != -1) {
            wav.write(data)
        }
        wav.flush()

        raw.close()
        wav.close()
    }

    fun stopRecorder() {
        recorder?.stop()
        recorder?.release()
        recorder = null
    }

    fun playCalibration() {
        playFile(calibrationFile)
    }

    fun playMeasurement() {
        playFile(measurementFile)
    }

    fun playFile(file: File) {
        try {
            debugPlayer = MediaPlayer.create(context, file.toUri())
            debugPlayer!!.setOnCompletionListener {
                debugPlayer!!.stop()
                debugPlayer!!.release()
                debugPlayer = null
            }

            debugPlayer!!.start()
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    fun calculateResults() {
        try {
            val calibrationStream = FileInputStream(rawCalibrationFile)
            val measurementStream = FileInputStream(rawMeasurementFile)

            val values = ArrayList<LineData>()
            val bytes = ByteArray(chunkSize * 2)
            val fft = FloatFFT_1D(chunkSize.toLong())
            var i = 0

            while (calibrationStream.available() > 0) {
                calibrationStream.read(bytes)
                val byteBuf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                val floats = FloatArray(chunkSize, {ii -> byteBuf.getShort(ii * 2).toFloat()})
                fft.realForward(floats)

                // frequency of max response
//                values.add(LineData(floats.indices.maxBy{floats[it]}.toFloat() * chunkSize, i++))
                // amplitude of max frequency
                values.add(LineData(floats.max(), i++))
            }

            results = values.toList()

            calibrationStream.close()
            measurementStream.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
    }
}