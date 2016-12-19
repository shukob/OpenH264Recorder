#include <jni.h>
#include <codec_api.h>
extern "C"
jstring
Java_me_skonb_openh264cameraview_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    auto hello = "Hello from C++";
    return env->NewStringUTF(hello);
}
