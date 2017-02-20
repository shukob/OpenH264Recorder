//
// Created by skonb on 2016/12/19.
//

#ifndef OPENH264CAMERAVIEW_CAMERAENCODER_H
#define OPENH264CAMERAVIEW_CAMERAENCODER_H

#import "Encoder.h"
#include <cstdlib>

class CameraEncoder : public Encoder {
public:
    CameraEncoder(int width, int height, std::string outputPath) : Encoder(width, height, outputPath) {
    }

protected:


};


#endif //OPENH264CAMERAVIEW_CAMERAENCODER_H
