//
// Created by skonb on 2016/12/19.
//

#ifndef OPENH264CAMERAVIEW_ENCODER_H
#define OPENH264CAMERAVIEW_ENCODER_H

#include <codec_api.h>
#include <cstdlib>
#include <codec_app_def.h>
#include "BufferedData.h"
#include <iostream>
#include <fstream>

extern void logEncode(void *context, int level, const char *message);

class Encoder {

public:
    Encoder(int width, int height, std::string outputPath) : _inputWidth(width),
                                                             _inputHeight(height),
                                                             _outputPath(outputPath),
                                                             _encoder(NULL) {
        int rv = 0;
        rv = WelsCreateSVCEncoder(&_encoder);
        clear();
        SEncParamBase param;
        memset(&param, 0, sizeof(param));
        param.iUsageType = CAMERA_VIDEO_REAL_TIME;
        param.fMaxFrameRate = 30;
        param.iPicWidth = _inputWidth;
        param.iPicHeight = _inputHeight;
        param.iTargetBitrate = 5000000;
        param.iRCMode = RC_BITRATE_MODE;
//        SEncParamExt param;
//        _encoder->GetDefaultParams(&param);
//
//        param.iUsageType = CAMERA_VIDEO_NON_REAL_TIME;
//        param.fMaxFrameRate = 30;
//        param.iPicWidth = _inputWidth;
//        param.iPicHeight = _inputHeight;
//        param.iTargetBitrate = 5000000;
//        param.bEnableDenoise = 0;
////        param.iSpatialLayerNum = pEncParamExt->iSpatialLayerNum;
////        param.bIsLosslessLink = pEncParamExt->bIsLosslessLink;
////        param.bEnableLongTermReference = pEncParamExt->bEnableLongTermReference;
////        param.iEntropyCodingModeFlag = pEncParamExt->iEntropyCodingModeFlag ? 1 : 0;
//        auto eSliceMode = SM_SINGLE_SLICE;
//        if (eSliceMode != SM_SINGLE_SLICE && eSliceMode != SM_SIZELIMITED_SLICE) //SM_SIZELIMITED_SLICE don't support multi-thread now
//            param.iMultipleThreadIdc = 2;
//
//        for (int i = 0; i < param.iSpatialLayerNum; i++) {
//            param.sSpatialLayers[i].iVideoWidth = _inputWidth >> (param.iSpatialLayerNum - 1 - i);
//            param.sSpatialLayers[i].iVideoHeight = _inputHeight >> (param.iSpatialLayerNum - 1 - i);
//            param.sSpatialLayers[i].fFrameRate = 30;
//            param.sSpatialLayers[i].iSpatialBitrate = param.iTargetBitrate;
//
//            param.sSpatialLayers[i].sSliceArgument.uiSliceMode = eSliceMode;
//            if (eSliceMode == SM_SIZELIMITED_SLICE) {
//                param.sSpatialLayers[i].sSliceArgument.uiSliceSizeConstraint = 600;
//                param.uiMaxNalSize = 1500;
//                param.iMultipleThreadIdc = 4;
//                param.bUseLoadBalancing = false;
//            }
//            if (eSliceMode == SM_FIXEDSLCNUM_SLICE) {
//                param.sSpatialLayers[i].sSliceArgument.uiSliceNum = 4;
//                param.iMultipleThreadIdc = 4;
//                param.bUseLoadBalancing = false;
//            }
//        }
//        param.iTargetBitrate *= param.iSpatialLayerNum;

        setSize(_inputWidth, _inputHeight);
        int level = WELS_LOG_DETAIL;
        rv = _encoder->SetOption(ENCODER_OPTION_TRACE_LEVEL, &level);
        void (*func)(void *, int, const char *) = &logEncode;
        rv = _encoder->SetOption(ENCODER_OPTION_TRACE_CALLBACK, &func);
        int videoFormat = videoFormatI420;
        rv = _encoder->SetOption(ENCODER_OPTION_DATAFORMAT, &videoFormat);
        rv = _encoder->Initialize(&param);
        rv = 0;

    }

    virtual ~Encoder() {
        if (_encoder) {
            _encoder->Uninitialize();
        }
        WelsDestroySVCEncoder(_encoder);
        _encoder = NULL;
        _buffer.Clear();
    }

    BufferedData *buffer() {
        return &_buffer;
    }

    void clear() {
        memset(&_param, 0, sizeof(_param));
        memset(&_sourcePicture, 0, sizeof(_sourcePicture));
        memset(&_info, 0, sizeof(_info));
        memset(&_sourcePicture, 0, sizeof(_sourcePicture));
    }

    void initParam() {
        _param.iUsageType = CAMERA_VIDEO_NON_REAL_TIME;
        _param.fMaxFrameRate = 30;
        _param.iPicWidth = _inputWidth;
        _param.iPicHeight = _inputHeight;
        _param.iTargetBitrate = 5000000;
    }

    int initWithParam(SEncParamExt *pEncParamExt) {

        SliceModeEnum eSliceMode = pEncParamExt->sSpatialLayers[0].sSliceArgument.uiSliceMode;
        bool bBaseParamFlag = (SM_SINGLE_SLICE == eSliceMode && !pEncParamExt->bEnableDenoise
                               && pEncParamExt->iSpatialLayerNum == 1 && !pEncParamExt->bIsLosslessLink
                               && !pEncParamExt->bEnableLongTermReference && !pEncParamExt->iEntropyCodingModeFlag) ? true : false;
        if (bBaseParamFlag) {
            SEncParamBase param;
            memset(&param, 0, sizeof(SEncParamBase));

            param.iUsageType = pEncParamExt->iUsageType;
            param.fMaxFrameRate = pEncParamExt->fMaxFrameRate;
            param.iPicWidth = pEncParamExt->iPicWidth;
            param.iPicHeight = pEncParamExt->iPicHeight;
            param.iTargetBitrate = 5000000;

            return _encoder->Initialize(&param);
        } else {
            SEncParamExt param;
            _encoder->GetDefaultParams(&param);

            param.iUsageType = pEncParamExt->iUsageType;
            param.fMaxFrameRate = pEncParamExt->fMaxFrameRate;
            param.iPicWidth = pEncParamExt->iPicWidth;
            param.iPicHeight = pEncParamExt->iPicHeight;
            param.iTargetBitrate = 5000000;
            param.bEnableDenoise = pEncParamExt->bEnableDenoise;
            param.iSpatialLayerNum = pEncParamExt->iSpatialLayerNum;
            param.bIsLosslessLink = pEncParamExt->bIsLosslessLink;
            param.bEnableLongTermReference = pEncParamExt->bEnableLongTermReference;
            param.iEntropyCodingModeFlag = pEncParamExt->iEntropyCodingModeFlag ? 1 : 0;
            if (eSliceMode != SM_SINGLE_SLICE && eSliceMode != SM_SIZELIMITED_SLICE) //SM_SIZELIMITED_SLICE don't support multi-thread now
                param.iMultipleThreadIdc = 2;

            for (int i = 0; i < param.iSpatialLayerNum; i++) {
                param.sSpatialLayers[i].iVideoWidth = pEncParamExt->iPicWidth >> (param.iSpatialLayerNum - 1 - i);
                param.sSpatialLayers[i].iVideoHeight = pEncParamExt->iPicHeight >> (param.iSpatialLayerNum - 1 - i);
                param.sSpatialLayers[i].fFrameRate = pEncParamExt->fMaxFrameRate;
                param.sSpatialLayers[i].iSpatialBitrate = param.iTargetBitrate;

                param.sSpatialLayers[i].sSliceArgument.uiSliceMode = eSliceMode;
                if (eSliceMode == SM_SIZELIMITED_SLICE) {
                    param.sSpatialLayers[i].sSliceArgument.uiSliceSizeConstraint = 600;
                    param.uiMaxNalSize = 1500;
                    param.iMultipleThreadIdc = 4;
                    param.bUseLoadBalancing = false;
                }
                if (eSliceMode == SM_FIXEDSLCNUM_SLICE) {
                    param.sSpatialLayers[i].sSliceArgument.uiSliceNum = 4;
                    param.iMultipleThreadIdc = 4;
                    param.bUseLoadBalancing = false;
                }
            }
            param.iTargetBitrate *= param.iSpatialLayerNum;


        }
        return _encoder->InitializeExt(&_param);
    }

    void setCurrentTimeStamp(long long timeStamp){
        _sourcePicture.uiTimeStamp = timeStamp;
    }

    int encodeOnce() {
        int ret = _encoder->EncodeFrame(&_sourcePicture, &_info);
        if (ret) {
            _previouslyEncodedOutputLength = 0;
        } else {
            if (_info.eFrameType != videoFrameTypeSkip) {
                int len = 0;
                for (int i = 0; i < _info.iLayerNum; ++i) {
                    const SLayerBSInfo &layerInfo = _info.sLayerInfo[i];
                    for (int j = 0; j < layerInfo.iNalCount; ++j) {
                        len += layerInfo.pNalLengthInByte[j];
                    }
                }
                _previouslyEncodedOutputLength = len;
            } else {
                _previouslyEncodedOutputLength = 0;
            }
        }
        writeToOutput();
        return _previouslyEncodedOutputLength;
    }

    void writeToOutput() {
        if (_outputPath.length()) {
            if (!_outputStream.is_open()) {
                _outputStream.open(_outputPath, std::ios_base::binary | std::ios_base::out);
            }
            if (_outputStream.is_open()) {
                int i;
                for (i = 0; i < _info.iLayerNum; ++i) {
                    SLayerBSInfo layerInfo = _info.sLayerInfo[i];
                    int layerSize = 0;
                    int j;
                    for (j = 0; j < layerInfo.iNalCount; ++j)
                        layerSize += layerInfo.pNalLengthInByte[j];
                    _outputStream.write((const char *) layerInfo.pBsBuf, layerSize);
                }
            }
        }
    }


    void setSize(int width, int height) {
        _buffer.Clear();
        _buffer.SetLength(width * height * 3 / 2);
        _sourcePicture.iPicWidth = width;
        _sourcePicture.iPicHeight = height;
        _sourcePicture.iStride[0] = width;
        _sourcePicture.iStride[1] = _sourcePicture.iStride[2] = width >> 1;
        _sourcePicture.pData[0] = _buffer.data();
        _sourcePicture.pData[1] = _sourcePicture.pData[0] + width * height;
        _sourcePicture.pData[2] = _sourcePicture.pData[1] + (width * height >> 2);
        _sourcePicture.iColorFormat = videoFormatI420;
    }


    int getPreviouslyEncodedOutputLength() {
        return _previouslyEncodedOutputLength;
    }

    unsigned char *getPreviouslyEncodedOutputData() {
        return _info.sLayerInfo[0].pBsBuf;
    }

    int getInputWidth() {
        return _inputWidth;
    }

    int getInputHeight() {
        return _inputHeight;
    }

protected:
    int _inputWidth;
    int _inputHeight;
    std::string _outputPath;
    ISVCEncoder *_encoder;
    BufferedData _buffer;
    SEncParamExt _param;
    SSourcePicture _sourcePicture;
    SFrameBSInfo _info;
    std::ofstream _outputStream;
    int _previouslyEncodedOutputLength;
};


#endif //OPENH264CAMERAVIEW_ENCODER_H
