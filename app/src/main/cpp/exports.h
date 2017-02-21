//
// Created by skonb on 2017/02/15.
//

#ifndef OPENH264CAMERAVIEW_EXPORTS_H
#define OPENH264CAMERAVIEW_EXPORTS_H

#include <jni.h>
#include "Encoder.h"
#include "CameraEncoder.h"

#ifdef __cplusplus
extern "C" {
#endif
JNIEXPORT void JNICALL Java_me_skonb_openh264cameraview_H264Encoder_createEncoder(JNIEnv *env, jobject jThis, jint width, jint height, jstring outputPath);

JNIEXPORT void JNICALL Java_me_skonb_openh264cameraview_H264Encoder_destroyEncoder(JNIEnv *env, jobject jThis);

//@returns length of encoded data in bytes
JNIEXPORT jint JNICALL Java_me_skonb_openh264cameraview_H264Encoder_encode(JNIEnv *env, jobject jThis, jbyteArray data, jlong timeStamp, jobject cameraInfo, jint width
, jint height);

JNIEXPORT void JNICALL Java_me_skonb_openh264cameraview_H265Encoder_fetchEncodedData(JNIEnv *env, jobject jThis, jbyteArray output);

#ifdef __cplusplus
}
#endif

#endif //OPENH264CAMERAVIEW_EXPORTS_H
