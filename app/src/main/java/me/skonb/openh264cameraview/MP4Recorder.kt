package me.skonb.openh264cameraview

import android.hardware.Camera
import android.os.AsyncTask
import android.os.Environment
import com.googlecode.mp4parser.FileDataSourceImpl
import com.googlecode.mp4parser.authoring.Movie
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder
import com.googlecode.mp4parser.authoring.tracks.AACTrackImpl
import com.googlecode.mp4parser.authoring.tracks.h264.H264TrackImpl
import java.io.File
import java.io.FileOutputStream


/**
 * Created by skonb on 2017/02/18.
 */
open class MP4Recorder : Camera.PreviewCallback, H264Recorder.Delegate {

    companion object {
        val TAG = "MP4Recorder"
    }

    protected var h264Recorder: H264Recorder? = null
    protected var aacRecorder: AACRecorder? = null
    protected val outputDir = File(Environment.getExternalStorageDirectory(), TAG)
    protected var h264OutputFile: File? = null
    protected var aacOutputFile: File? = null
    var outputFile: File? = null
    protected var recording = false
    var maxDuration = 0L
        set(value) {
            field = value
            h264Recorder?.maxDuration = value
        }
    val sync = Object()

    fun ensureOutputFiles() {
        h264OutputFile = File(outputDir, "video.h264")
        outputDir.mkdirs()
        if (h264OutputFile?.exists() == true) {
            h264OutputFile?.delete()
        }
        h264OutputFile?.createNewFile()
        aacOutputFile = File(outputDir, "sound.aac")
        if (aacOutputFile?.exists() == true) {
            aacOutputFile?.delete()
        }
        aacOutputFile?.createNewFile()
        outputFile = File(outputDir, "output.mp4")
        if (outputFile?.exists() == true) {
            outputFile?.delete()
        }
        outputFile?.createNewFile()
    }

    constructor() {
        ensureOutputFiles()
        h264Recorder = H264Recorder(h264OutputFile)
        h264Recorder?.delegate = this
        h264Recorder?.maxDuration = maxDuration
        aacRecorder = AACRecorder(aacOutputFile)
    }


    fun mux(h264Path: String, aacPath: String, outputPath: String) {
        val h264Track = H264TrackImpl(FileDataSourceImpl(h264Path))
        val aacTrack = AACTrackImpl(FileDataSourceImpl(aacPath))
        val movie = Movie()
        movie.addTrack(h264Track)
        movie.addTrack(aacTrack)
        val mp4File = DefaultMp4Builder().build(movie)
        val fc = FileOutputStream(File(outputPath)).channel
        mp4File.writeContainer(fc)
        fc.close()
    }

    override fun onPreviewFrame(p0: ByteArray?, p1: Camera?) {
        h264Recorder?.onPreviewFrame(p0, p1)
    }

    fun prepare() {
        h264Recorder?.prepare(sync)
        aacRecorder?.prepare(sync)
    }

    fun start() {
        if (!recording) {
            h264Recorder?.start()
            aacRecorder?.start()
            recording = true
        } else {
            resume()
        }
    }

    fun resume() {
        recording = true
    }

    fun pause() {
        recording = false
        h264Recorder?.pause()
        aacRecorder?.pause()
    }

    fun stop(callback: ((success: Boolean) -> Unit)?) {
        pause()
        object : AsyncTask<Unit, Unit, Unit>() {
            override fun doInBackground(vararg p0: Unit?) {
                h264Recorder?.stop()
                //aacRecorder automatically stops
                h264Recorder?.join()
                aacRecorder?.join()
                mux(h264OutputFile!!.absolutePath, aacOutputFile!!.absolutePath, outputFile!!.absolutePath)
            }

            override fun onPostExecute(result: Unit?) {
                callback?.invoke(true)
            }
        }.execute()
    }


    fun release() {
        h264Recorder?.release()
        aacRecorder?.release()
    }

    fun setCamera(camera: Camera) {
        h264Recorder?.camera = camera
    }

    override fun onRecordingStop(recorder: H264Recorder) {
        aacRecorder?.stop()
    }
}
