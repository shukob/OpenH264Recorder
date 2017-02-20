//
// Created by skonb on 2017/02/15.
//

#include "exports.h"
#include <android/log.h>

Encoder *encoder = NULL;
BufferedData *buf;

static bool NV21toI420(const char *src, char *dst, int width, int height);

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

JNIEXPORT jint JNICALL Java_me_skonb_openh264cameraview_H264Encoder_encode(JNIEnv *env, jobject jThis, jbyteArray data, jlong timeStamp, jobject cameraInfo) {
    int len = env->GetArrayLength(data);
    if (buf->Length() != len) {
        buf->SetLength(len);
    }
    if (encoder->buffer()->Length() != len) {
        //TODO reallocate buffer
    }

    env->GetByteArrayRegion(data, 0, len, reinterpret_cast<jbyte *>(buf->data()));
    NV21toI420((const char *) buf->data(), (char *) encoder->buffer()->data(), encoder->getInputWidth(), encoder->getInputHeight());
    encoder->setCurrentTimeStamp(timeStamp);
    return encoder->encodeOnce();
}


JNIEXPORT void JNICALL Java_me_skonb_openh264cameraview_H265Encoder_fetchEncodedData(JNIEnv *env, jobject jThis, jbyteArray output) {
    env->SetByteArrayRegion(output, 0, encoder->getPreviouslyEncodedOutputLength(), reinterpret_cast<jbyte *>(encoder->getPreviouslyEncodedOutputData()));
}


void logEncode(void *context, int level, const char *message) {
    __android_log_print(ANDROID_LOG_VERBOSE, "JNI", message, 1);
}

static bool NV21toI420(const char *src, char *dst, int width, int height) {
    if (!src || !dst) {
        return false;
    }

    unsigned int YSize = width * height;
    unsigned int UVSize = (YSize >> 1);

    // NV21: Y..Y + VUV...U
    const char *pSrcY = src;
    const char *pSrcUV = src + YSize;

    // I420: Y..Y + U.U + V.V
    char *pDstY = dst;
    char *pDstU = dst + YSize;
    char *pDstV = dst + YSize + (UVSize >> 1);

    // copy Y
    memcpy(pDstY, pSrcY, YSize);

    // copy U and V
    for (int k = 0; k < (UVSize >> 1); k++) {
        pDstV[k] = pSrcUV[k * 2];     // copy V
        pDstU[k] = pSrcUV[k * 2 + 1];   // copy U
    }

    return true;
}


