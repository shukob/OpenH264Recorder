package me.skonb.openh264cameraview

import android.hardware.Camera
import android.os.AsyncTask
import android.os.Environment
import com.coremedia.iso.boxes.MovieHeaderBox
import com.googlecode.mp4parser.FileDataSourceImpl
import com.googlecode.mp4parser.authoring.Movie
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder
import com.googlecode.mp4parser.authoring.tracks.AACTrackImpl
import com.googlecode.mp4parser.authoring.tracks.h264.H264TrackImpl
import com.googlecode.mp4parser.util.Matrix
import java.io.File
import java.io.FileOutputStream
import java.util.*


/**
 * Created by skonb on 2017/02/18.
 */
open class MP4Recorder : Camera.PreviewCallback, H264Recorder.Delegate {

    interface Delegate {
        fun onUpdateRecordedLength(mP4Recorder: MP4Recorder, length: Long)
    }

    companion object {
        val TAG = "MP4Recorder"
    }


    var delegate: Delegate? = null
    protected var h264Recorder: H264Recorder? = null
    protected var aacRecorder: AACRecorder? = null
    protected val outputDir = File(Environment.getExternalStorageDirectory(), TAG)
    protected var h264OutputFile: File? = null
    protected var aacOutputFile: File? = null
    var isPrepared = false
    var outputFile: File? = null
    protected var recording = false
    var maxDuration = 0L
        set(value) {
            field = value
            h264Recorder?.maxDuration = value
        }
    val sync = Object()

    fun ensureOutputFiles() {
        h264OutputFile = File(outputDir, "video_${System.currentTimeMillis()}.h264")
        outputDir.mkdirs()
        if (h264OutputFile?.exists() == true) {
            h264OutputFile?.delete()
        }
        h264OutputFile?.createNewFile()
        aacOutputFile = File(outputDir, "sound_${System.currentTimeMillis()}.aac")
        if (aacOutputFile?.exists() == true) {
            aacOutputFile?.delete()
        }
        aacOutputFile?.createNewFile()
        outputFile = File(outputDir, "output_${System.currentTimeMillis()}.mp4")
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
        h264Track.trackMetaData.matrix = Matrix.ROTATE_90
        val aacTrack = AACTrackImpl(FileDataSourceImpl(aacPath))
        val movie = Movie()
        movie.addTrack(h264Track)
        movie.addTrack(aacTrack)
        val mp4File = DefaultMp4Builder().build(movie)
        val mvhd = MovieHeaderBox()
        mvhd.creationTime = Date()
        mvhd.modificationTime = Date()
        mvhd.timescale = 30 * 1000
        mvhd.duration = h264Recorder!!.recordedTime * 30
        mvhd.matrix = Matrix.ROTATE_0
        mp4File.boxes.add(mvhd)
        val fc = FileOutputStream(File(outputPath)).channel
        mp4File.writeContainer(fc)
        fc.close()
        h264OutputFile?.delete()
        h264OutputFile = null
        aacOutputFile?.delete()
        aacOutputFile = null
    }

    override fun onPreviewFrame(p0: ByteArray?, p1: Camera?) {
        h264Recorder?.onPreviewFrame(p0, p1)
    }

    fun prepare() {
        if (!isPrepared) {
            h264Recorder?.prepare(sync)
            aacRecorder?.prepare(sync)
            isPrepared = true
        }
    }

    fun unprepare() {
        //TODO implement
        isPrepared = false
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

    fun setCamera(camera: Camera, cameraId: Int) {
        h264Recorder?.camera = camera
        h264Recorder?.cameraId = cameraId
    }

    override fun onRecordingStop(recorder: H264Recorder) {
        aacRecorder?.stop()
    }

    override fun onUpdateRecordedLength(recorder: H264Recorder, length: Long) {
        delegate?.onUpdateRecordedLength(this, length)
    }

    fun concat(files: List<File>): File? {
        val out = File(outputDir, "video_${System.currentTimeMillis()}.mp4")
        //TODO
        //1: extract raw video/audio stream
        //2: concat each stream
        //3: mux video/audio
        return out
    }
}
