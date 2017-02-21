//
// Created by skonb on 2017/02/15.
//

#include "exports.h"
#include <android/log.h>


static Encoder *encoder = NULL;
static BufferedData *buf;

void logEncode(void *context, int level, const char *message) {
    __android_log_print(ANDROID_LOG_VERBOSE, "JNI", message, 1);
}


int fetchCameraOrientation(JNIEnv *env, jobject cameraInfo) {
    jclass cls = env->GetObjectClass(cameraInfo);
    jfieldID fid = env->GetFieldID(cls, "orientation", "I");
    return env->GetIntField(cameraInfo, fid);
}

JNIEXPORT void JNICALL Java_me_skonb_openh264cameraview_H264Encoder_createEncoder(JNIEnv *env, jobject jThis, jint width, jint height, jstring outputPath) {
    if (encoder != NULL) {
        Java_me_skonb_openh264cameraview_H264Encoder_destroyEncoder(env, jThis);
    }
    jboolean isCopy = 1;
    encoder = new CameraEncoder(width, height, env->GetStringUTFChars(outputPath, &isCopy));
    buf = new BufferedData();
}

JNIEXPORT void JNICALL Java_me_skonb_openh264cameraview_H264Encoder_destroyEncoder(JNIEnv *env, jobject jThis) {
    delete encoder;
    encoder = NULL;
    delete buf;
    buf = NULL;
}

JNIEXPORT jint JNICALL Java_me_skonb_openh264cameraview_H264Encoder_encode(JNIEnv *env, jobject jThis, jbyteArray data, jlong timeStamp, jobject cameraInfo, jint width, jint height) {

    int len = env->GetArrayLength(data);
    if (buf->Length() != len) {
        buf->SetLength(len);
    }
    int orientation = fetchCameraOrientation(env, cameraInfo);
    env->GetByteArrayRegion(data, 0, len, reinterpret_cast<jbyte *>(buf->data()));
    return encoder->encodeOnce(*buf, Encoder::NV21, timeStamp, width, height, (orientation + 270) % 360);
}


