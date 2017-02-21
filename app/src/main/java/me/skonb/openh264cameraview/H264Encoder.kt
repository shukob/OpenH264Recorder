package me.skonb.openh264cameraview

import android.hardware.Camera


/**
 * Created by skonb on 2017/02/16.
 */
class H264Encoder {
    init {
        System.loadLibrary("native-lib")
    }

    external fun createEncoder(width: Int, height: Int, outputPath: String)
    external fun destroyEncoder()
    external fun encode(data: ByteArray, timeStamp: Long, cameraInfo: Camera.CameraInfo, width: Int, height: Int): Int

}
