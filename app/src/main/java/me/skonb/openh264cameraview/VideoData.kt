package me.skonb.openh264cameraview

/**
 * Created by skonb on 2017/02/19.
 */

import android.annotation.SuppressLint
import android.hardware.Camera.CameraInfo
import android.os.Build

class VideoData {

    var timeStamp: Long = 0
        private set

    var data: ByteArray? = null
        private set

    var cameraInfo: CameraInfo? = null
        private set

    var width: Int = 0
        private set
    var height: Int = 0
        private set

    operator fun set(timeStamp: Long, data: ByteArray, info: CameraInfo, width: Int, height: Int): VideoData {
        this.timeStamp = timeStamp
        this.data = data.copyOf()
        this.cameraInfo = cloneCameraInfo(info)
        this.width = width
        this.height = height
        return this
    }

    @SuppressLint("NewApi")
    private fun cloneCameraInfo(info: CameraInfo?): CameraInfo? {
        if (info == null)
            return null
        val clone = CameraInfo()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            clone.canDisableShutterSound = info.canDisableShutterSound
        clone.facing = info.facing
        clone.orientation = info.orientation
        return clone
    }

    @Throws(CloneNotSupportedException::class)
    fun clone(): Any {
        val clone = VideoData()
        clone.data = data!!.clone()
        clone.timeStamp = timeStamp
        clone.cameraInfo = cloneCameraInfo(cameraInfo)
        return clone
    }
}
