//
// Created by skonb on 2017/02/21.
//

#ifndef OPENH264CAMERAVIEW_YUV_H
#define OPENH264CAMERAVIEW_YUV_H


struct YUV {
    uint8_t *y;
    uint8_t *u;
    uint8_t *v;
    int yStride;
    int uStride;
    int vStride;
    int width;
    int height;

    static YUV from(uint8_t *data, int width, int height) {
        YUV yuv;
        yuv.yStride = width;
        yuv.uStride = yuv.vStride = width >> 1;
        yuv.y = data;
        yuv.u = yuv.y + yuv.yStride;
        yuv.v = yuv.u + yuv.uStride;
        yuv.width = width;
        yuv.height = height;
        return yuv;

    }

    int length() {
        return width * height * 3 / 2;
    }

};


#endif //OPENH264CAMERAVIEW_YUV_H
