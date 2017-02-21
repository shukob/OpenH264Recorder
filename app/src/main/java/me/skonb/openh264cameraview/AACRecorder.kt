package me.skonb.openh264cameraview

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder.AudioSource
import android.util.Log
import com.todoroo.aacenc.AACEncoder
import java.io.File
import java.nio.ByteBuffer


/**
 * Created by skonb on 2017/02/18.
 */
class AACRecorder {

    companion object {
        val TAG = "AACEncoder"
    }


    var aacEncoder: AACEncoder? = null
    var audioRecord: AudioRecord? = null
    var samplingRate = 44100
    var outputFile: File? = null
    var recording = false
    var sync: Object? = null
    var runAudioThread = false
    var audioThread: Thread? = null
    var bufferSize: Int = 0

    private val SAMPLING_RATES = intArrayOf(44100, 22050, 11025, 8000)


    constructor(outputFile: File?) {
        this.outputFile = outputFile
    }

    private inner class AudioRecordRunnable : Runnable {


        @Volatile var isReady: Boolean = false
            private set

        init {
            isReady = false
        }

        fun findAudioRecord() {
            for (rate in SAMPLING_RATES) {
                for (audioFormat in shortArrayOf(AudioFormat.ENCODING_PCM_16BIT.toShort(), AudioFormat.ENCODING_PCM_8BIT.toShort())) {
                    for (channelConfig in shortArrayOf(AudioFormat.CHANNEL_IN_MONO.toShort(), AudioFormat.CHANNEL_IN_STEREO.toShort())) {
                        try {
                            Log.d(TAG, "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: "
                                    + channelConfig)
                            val bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig.toInt(), audioFormat.toInt())

                            if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                                // check if we can instantiate and have a success
                                val recorder = AudioRecord(AudioSource.VOICE_RECOGNITION, rate, channelConfig.toInt(), audioFormat.toInt(), bufferSize)

                                if (recorder.state == AudioRecord.STATE_INITIALIZED) {
                                    this@AACRecorder.samplingRate = rate
                                    this@AACRecorder.audioRecord = recorder
                                    this@AACRecorder.bufferSize = bufferSize * 8
                                    return
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, rate.toString() + "Exception, keep trying.", e)
                        }

                    }
                }
            }
        }

        override fun run() {
            isReady = false
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)
            findAudioRecord()
            audioRecord?.let { audioRecord ->
                val buffer = ByteArray(bufferSize)
                aacEncoder = AACEncoder()
                aacEncoder?.init(64000, when (audioRecord.channelConfiguration) {
                    AudioFormat.CHANNEL_IN_MONO -> 1
                    AudioFormat.CHANNEL_IN_STEREO -> 2
                    else -> 1
                }, samplingRate,
                        when (audioRecord.audioFormat) {
                            AudioFormat.ENCODING_PCM_16BIT -> 16
                            AudioFormat.ENCODING_PCM_8BIT -> 8
                            else -> 8
                        }
                        , outputFile?.absolutePath)
                audioRecord.startRecording()
                synchronized(sync!!) {
                    isReady = true
                    sync?.notifyAll()
                }
                while (runAudioThread) {
                    val readDataSize = audioRecord.read(buffer, 0, bufferSize)
                    if (readDataSize > 0) {
                        if (runAudioThread && recording) {
                            aacEncoder?.encode(buffer, readDataSize)
                            Log.i(TAG, "encoded ${readDataSize}")
                        }
                    }
                }
                audioRecord.stop()
                audioRecord.release()
                this@AACRecorder.audioRecord = null
                aacEncoder?.uninit()
                aacEncoder = null
            }
        }
    }

    fun prepare(sync: Object) {
        this.sync = sync
        runAudioThread = true
        val audioRecordRunnable = AudioRecordRunnable()
        audioThread = Thread(audioRecordRunnable, "AudioThread")
        audioThread?.start()
        synchronized(sync) {
            try {
                while (!audioRecordRunnable.isReady)
                    sync.wait()
            } catch (e: InterruptedException) {
            }
        }
    }


    fun start() {
        recording = true
    }

    fun pause() {
        recording = false
    }

    fun stop() {
        runAudioThread = false
    }

    fun release() {
        audioRecord?.release()
        aacEncoder?.uninit()
        audioRecord = null
        aacEncoder = null
    }

    fun join() {
        try {
            audioThread?.join()
        } catch (e: InterruptedException) {

        }
    }
}
