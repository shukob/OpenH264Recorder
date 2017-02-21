package me.skonb.openh264cameraview

import android.graphics.ImageFormat
import android.hardware.Camera
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Created by skonb on 2017/02/19.
 */
class H264Recorder : Camera.PreviewCallback {

    interface Delegate {
        fun onRecordingStop(recorder: H264Recorder)
        fun onUpdateRecordedLength(recorder: H264Recorder, length: Long)
    }

    constructor(outputFile: File?) {
        this.outputFile = outputFile
    }

    var delegate: Delegate? = null
    private val videoDataQueue = ConcurrentLinkedQueue<VideoData>()
    private var videoThread: Thread? = null
    private var frameRate: Float = 0.toFloat()
    protected var recording = false
    var maxDuration = 0L
    var recordedTime = 0L
    @Volatile protected var recordingStarted = 0L
    @Volatile protected var lastRecordedTimeOffset = 0L
    @Volatile private var runVideoThread: Boolean = false
    val h264Encoder = H264Encoder()
    @Volatile private var canceled: Boolean = false
    var previewSize: Camera.Size? = null
    protected var cameraInfo: Camera.CameraInfo = Camera.CameraInfo()
    protected var outputFile: File? = null

    var sync = Object()

    var camera: Camera? = null
        set(value) {
            field = value
            camera?.setPreviewCallback(this)
        }
    var cameraId: Int = 0
        set(value) {
            field = value
            Camera.getCameraInfo(value, cameraInfo)
        }


    private inner class VideoEncoderRunnable : Runnable {

        override fun run() {
            var concurrentData: VideoData? = null
            h264Encoder.createEncoder(previewSize!!.width, previewSize!!.height, outputFile!!.absolutePath)
            var lastData = false
            while (!lastData) {
                concurrentData = videoDataQueue.poll()
                lastData = concurrentData == null && !runVideoThread
                concurrentData?.let {
                    synchronized(this) {
                        h264Encoder.encode(it.data!!, it.timeStamp, it.cameraInfo!!, it.width, it.height
                        )
                    }
                }
            }
            h264Encoder.destroyEncoder()
        }
    }


    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
        data?.let { data ->
            if ((maxDuration < 0 || recordedTime < maxDuration) && recording) {
                val currentTime = System.currentTimeMillis()
                val recordDuration = currentTime - recordingStarted
                recordedTime = recordDuration + lastRecordedTimeOffset
                videoDataQueue.offer(VideoData().set(recordedTime, data, cameraInfo, previewSize!!.width, previewSize!!.height))
            } else if (recordedTime >= maxDuration) {
                recordedTime = maxDuration
                stop()
            }
            delegate?.onUpdateRecordedLength(this, recordedTime)
            camera?.addCallbackBuffer(data)
        }
//        if (recording) {
//            val currentTime = System.currentTimeMillis()
//            val recordDuration = currentTime - recordingStarted
//            recordedTime = recordDuration + lastRecordedTimeOffset
//
//            if (((maxDuration < 0) || (recordedTime < maxDuration)) && recording) {
//                val len = h264Encoder.encode(data!!, recordedTime, cameraInfo)
//                Log.i(TAG, "encoded $len bytes")
//            } else if (recordedTime >= maxDuration) {
//                recordedTime = maxDuration;
//                stop()
//            }
//        }

    }

    fun prepare(sync: Object) {
        if (runVideoThread)
            throw IllegalStateException("You must first stop the encoder.")
        if (camera == null)
            throw IllegalStateException("No camera set.")
        this.sync = sync
        recording = false
        canceled = false
        recordingStarted = 0
        camera?.parameters?.let { cameraParameters ->
            previewSize = cameraParameters?.previewSize
            previewSize?.let { previewSize ->
                val previewBufferSize = previewSize.width * previewSize.height + (previewSize.width * previewSize.height + 1) / 2
                val fpsRange = IntArray(2)
                cameraParameters.getPreviewFpsRange(fpsRange)
                frameRate = fpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX].toFloat() / 1000f
                cameraParameters.previewFormat = ImageFormat.NV21
                camera?.parameters = cameraParameters
                camera?.addCallbackBuffer(ByteArray(previewBufferSize))
                camera?.addCallbackBuffer(ByteArray(previewBufferSize))

            }
        }
        runVideoThread = true
        videoThread = Thread(VideoEncoderRunnable(), "VideoThread")
        videoThread?.start()
    }

    fun start() {
        if (!recording) {
            recordingStarted = System.currentTimeMillis();
            lastRecordedTimeOffset = recordedTime;
            recording = true;
        }
    }

    fun pause() {
        recording = false
    }

    fun stop() {
        runVideoThread = false
        delegate?.onRecordingStop(this)
    }

    fun release() {
        try {
            camera?.setPreviewCallback(null)
        } catch (e: Exception) {

        }
        h264Encoder.destroyEncoder()
    }

    fun join() {
        try {
            videoThread?.join()
        } catch (e: InterruptedException) {

        }
    }

}

