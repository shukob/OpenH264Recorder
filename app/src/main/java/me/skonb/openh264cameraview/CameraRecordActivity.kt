package me.skonb.openh264cameraview

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import kotlinx.android.synthetic.main.activity_camera_record.*
import java.io.File
import java.util.*

class CameraRecordActivity : AppCompatActivity() {

    companion object {
        val TAG = "CameraRecord"
    }

    var camera: Camera? = null
    var mp4Recorder = MP4Recorder()
    var flashMode = Camera.Parameters.FLASH_MODE_OFF
        set(value) {
            if (field != value) {
                camera?.let { camera ->
                    camera.lock()
                    val params = camera.parameters
                    params.supportedFlashModes?.let {
                        if (it.contains(flashMode)) {
                            params.flashMode = flashMode
                            camera.parameters = params
                            field = value
                        }
                    }
                    camera.unlock()
                    reloadCamera()
                }
            }
        }

    var previewStarted = false
    var displayOrientation: Int? = null
    var cameraID = getBackCameraID()
    var surfaceTexture: SurfaceTexture? = null
    var surfaceWidth: Int = 0
    var surfaceHeight: Int = 0
    var previewWidth: Int = 0
    var previewHeight: Int = 0
    var videoWidth: Int = 0
    var videoHeight: Int = 0
    var progressDialogHelper = ProgressDialogHelper()

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_stop -> {
                progressDialogHelper.showProgressDialog(this@CameraRecordActivity)
                progressDialogHelper.updateProgressDialogMessage(this@CameraRecordActivity, "動画を生成中です")
                mp4Recorder.stop {
                    progressDialogHelper.hideProgressDialog(this@CameraRecordActivity)
                    ErrorDialogHelper().showMessageDialogWithMessage(this@CameraRecordActivity, "動画を生成しました",
                            "生成完了", "再生", object : Runnable {
                        override fun run() {
                            val intent = Intent(Intent.ACTION_VIEW, FileProvider.getUriForFile(this@CameraRecordActivity, "${applicationContext.packageName}.provider", mp4Recorder.outputFile))
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            finish()
                            startActivity(intent)
                        }
                    })

                }
            }
            R.id.action_camera_orientation -> {
//                progressDialogHelper.showProgressDialog(this@CameraRecordActivity)
//                progressDialogHelper.updateProgressDialogMessage(this@CameraRecordActivity, "カメラの向きを変えています")
//                mp4Recorder.stop {
//                    videoFileList.add(mp4Recorder.outputFile!!)
//                    mp4Recorder.ensureOutputFiles()
//                    stopPreview()
//                    releaseCamera()
//                    if (cameraID == getBackCameraID()) {
//                        cameraID = getFrontCameraID()
//                    } else {
//                        cameraID = getBackCameraID()
//                    }
//                    openCamera(cameraID)
//                }
                stopPreview()
                releaseCamera()
                if (cameraID == getBackCameraID()) {
                    cameraID = getFrontCameraID()
                } else {
                    cameraID = getBackCameraID()
                }
                openCamera(cameraID)
            }
            else -> {

            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_record)
        mp4Recorder.maxDuration = 10 * 1000L;
        textureView?.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    mp4Recorder.start()
                }
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                    mp4Recorder.pause()
                }
            }
            true
        }
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                openCamera(cameraID)
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    ErrorDialogHelper().showMessageDialogWithMessage(this, "カメラにアクセスします", "カメラの使用許可", "はい", object : Runnable {
                        override fun run() {
                            if (Build.VERSION.SDK_INT >= 23) {
                                requestPermissions(arrayOf(Manifest.permission.CAMERA,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                        Manifest.permission.READ_EXTERNAL_STORAGE,
                                        Manifest.permission.RECORD_AUDIO),
                                        100)
                            }
                        }
                    })
                } else {
                    requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO),
                            100)
                }
            }
        } else {
            openCamera(cameraID)
        }
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture?): Boolean {
                stopPreview()
                this@CameraRecordActivity.surfaceTexture = null
                return true
            }

            override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture?, width: Int, height: Int) {
                this@CameraRecordActivity.surfaceTexture = surfaceTexture
                surfaceWidth = width
                surfaceHeight = height
                camera?.let {
                    startPreview()
                }
            }

            override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture?, width: Int, height: Int) {
                surfaceWidth = width
                surfaceHeight = height
                determineDisplayOrientation()
                configurePreviewTransform(textureView!!.width, textureView!!.height)
            }

            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture?) {
                this@CameraRecordActivity.surfaceTexture = surfaceTexture
            }
        }
    }


    fun openCamera(id: Int) {
        determineVideoSize(id)?.let { size ->
            videoWidth = size.width
            videoHeight = size.height
            Log.i(TAG, "video size: ($videoWidth, $videoHeight)")
        }
        camera = getCamera(id)
        camera?.let {
            var parameters = it.parameters

            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(metrics)
            getOptimalSize(parameters.supportedPreviewSizes, 1280, 720)?.let { previewSize ->
                previewWidth = previewSize.width
                previewHeight = previewSize.height
                when (windowManager.defaultDisplay.rotation) {
                    Surface.ROTATION_0 -> {
                        camera?.setDisplayOrientation(90)
                    }
                    Surface.ROTATION_270 -> {
                        camera?.setDisplayOrientation(180)
                    }
                }

                parameters.setPreviewSize(previewWidth, previewHeight)
                Log.i(TAG, "preview size: ($previewWidth, $previewHeight)")
            }

            if (parameters.supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
            }

            parameters.supportedFlashModes?.let {
                if (it.contains(flashMode)) {
                    parameters.flashMode = flashMode
                }
            }

//            configureEnabledSpeeds(parameters)
            it.parameters = parameters
            determineDisplayOrientation()
            surfaceTexture?.let {
                startPreview()
            }
        }
    }

    private fun determineDisplayOrientation() {
        cameraID?.let { cameraID ->
            val cameraInfo = Camera.CameraInfo()
            Camera.getCameraInfo(cameraID, cameraInfo)

            // Clockwise rotation needed to align the window display to the natural position
            val rotation = windowManager.defaultDisplay.rotation
            var degrees = 0
            when (rotation) {
                Surface.ROTATION_0 -> {
                    degrees = 0
                }
                Surface.ROTATION_90 -> {
                    degrees = 90
                }
                Surface.ROTATION_180 -> {
                    degrees = 180
                }
                Surface.ROTATION_270 -> {
                    degrees = 270
                }
            }

            var displayOrientation: Int

            // CameraInfo.Orientation is the angle relative to the natural position of the device
            // in clockwise rotation (angle that is rotated clockwise from the natural position)
            if (cameraInfo.facing === Camera.CameraInfo.CAMERA_FACING_FRONT) {
                // Orientation is angle of rotation when facing the camera for
                // the camera image to match the natural orientation of the device
                displayOrientation = (cameraInfo.orientation + degrees) % 360
                displayOrientation = (360 - displayOrientation) % 360
            } else {
                displayOrientation = (cameraInfo.orientation - degrees + 360) % 360
            }

//            camera?.setDisplayOrientation(displayOrientation)
            this.displayOrientation = displayOrientation
        }
    }


    private fun getCamera(cameraID: Int): Camera? {
        try {
            return Camera.open(cameraID)
        } catch (e: Exception) {
            Log.d(TAG, "Can't open camera with id " + cameraID)
            e.printStackTrace()
        }
        return null
    }


    private fun getOptimalSize(sizes: List<Camera.Size>?, w: Int, h: Int, maxW: Int = Int.MAX_VALUE, maxH: Int = Int.MAX_VALUE): Camera.Size? {
        val ASPECT_TOLERANCE = 0.15
        val targetRatio = w.toDouble() / h


        if (sizes == null) return null


        Log.i(TAG, "supplied sizes: ${sizes.map { size -> "(${size.width}, ${size.height})" }.joinToString(", ")}")
        var optimalSize: Camera.Size? = null

        var minDiff = java.lang.Double.MAX_VALUE

        val targetHeight = h

        // Find size
        for (size in sizes) {
            val ratio = size.width.toDouble() / size.height.toDouble()
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue
            if (Math.abs(size.height - targetHeight) < minDiff && size.height <= maxH && size.width <= maxW) {
                optimalSize = size
                minDiff = Math.abs(size.height - targetHeight).toDouble()
            }
        }

        if (optimalSize == null) {
            minDiff = java.lang.Double.MAX_VALUE
            for (size in sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff && size.height <= maxH && size.width <= maxW) {
                    optimalSize = size
                    minDiff = Math.abs(size.height - targetHeight).toDouble()
                }
            }
        }

        return optimalSize
    }

    private fun getFrontCameraID(): Int {
        val pm = packageManager
        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
            return Camera.CameraInfo.CAMERA_FACING_FRONT
        }

        return getBackCameraID()
    }

    private fun getBackCameraID(): Int {
        return Camera.CameraInfo.CAMERA_FACING_BACK
    }

    var cameraInfo: Camera.CameraInfo? = null
    var encodedData: ByteArray = byteArrayOf()
    fun startPreview() {
        if (!previewStarted) {
            camera?.let { camera ->
                surfaceTexture?.let { surfaceTexture ->
                    camera.setPreviewTexture(surfaceTexture)
                    mp4Recorder.setCamera(camera, cameraID)
                    mp4Recorder.prepare()
                    camera.startPreview()
                    previewStarted = true
                    configurePreviewTransform(surfaceWidth, surfaceHeight)
                }
            }
        }
    }


    fun reloadCamera() {
//        stopRecording()
//        releaseMediaRecorder()
        releaseCamera()
        openCamera(cameraID!!)
    }

    fun determineVideoSize(id: Int): Camera.Size? {
        var videoSizes: MutableList<Camera.Size> = mutableListOf()
        getCamera(id)?.let { camera ->
            val params = camera.parameters
            params.supportedVideoSizes?.let { sizes ->
                videoSizes.addAll(sizes)
            } ?: videoSizes.addAll(params.supportedPreviewSizes)
            camera.release()
        }
        return getOptimalSize(videoSizes, 720, 1280, 1280, 720)
    }

    fun stopPreview() {
        if (previewStarted) {
            camera?.let { camera ->
                camera.stopPreview()
                camera.setPreviewCallback(null)
            }
        }
        previewStarted = false
    }


    fun releaseCamera() {
        stopPreview()
        camera?.let {
            it.release()
        }
        camera = null
    }

    private fun configurePreviewTransform(viewWidth: Int, viewHeight: Int) {
        val rotation = windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val height = previewWidth
        val width = previewHeight
        val horizontalScale = viewWidth.toFloat() / width
        val verticalScale = viewHeight.toFloat() / height
        if (horizontalScale > verticalScale) {
            matrix.postScale(1f, horizontalScale / verticalScale, 0f, 0f)
            val scaledHeight = viewHeight * horizontalScale / verticalScale
            matrix.postTranslate(0f, -(scaledHeight - viewHeight) / 2f)
        } else {
            matrix.postScale(verticalScale / horizontalScale, 1f, 0f, 0f)
            val scaledWidth = viewWidth * verticalScale / horizontalScale
            matrix.postTranslate(-(scaledWidth - viewWidth) / 2f, 0f)
        }
//
        Log.i(TAG, "displayRotation: $rotation")
        if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, viewWidth / 2f, viewHeight / 2f)
        } else if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            matrix.postRotate((90 * (rotation - 2)).toFloat(), viewWidth / 2f, viewHeight / 2f)
        }
        textureView?.setTransform(matrix)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            var granted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false
                    break
                }
            }
            if (granted) {
                openCamera(getBackCameraID())
            }
        }
    }


    override fun onResume() {
        super.onResume()
        cameraID?.let {
            openCamera(it)
        }
        textureView?.keepScreenOn = true
    }

    override fun onPause() {
        releaseCamera()
        textureView?.keepScreenOn = false
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mp4Recorder.release()
    }
}



