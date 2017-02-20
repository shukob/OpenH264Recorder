package com.todoroo.aacenc;

public class AACEncoder {

    /**
     * Native JNI - initialize AAC h264Encoder
     *
     */
    public native void init(int bitrate, int channels,
            int sampleRate, int bitsPerSample, String outputFile);

    /**
     * Native JNI - encode one or more frames
     *
     */
    public native void encode(byte[] inputArray);

    /**
     * Native JNI - uninitialize AAC h264Encoder and flush file
     *
     */
    public native void uninit();

    static {
        System.loadLibrary("aac_enc");
    }

}
