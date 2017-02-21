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
#include <libyuv/convert.h>
#include <libyuv/scale.h>
#include <libyuv.h>
#include "YUV.h"

extern void logEncode(void *context, int level, const char *message);

class Encoder {

public:
    typedef enum {
        NV21
    } SourceFormat;

    Encoder(int width, int height, std::string outputPath) : _originalInputWidth(width),
                                                             _originalInputHeight(height),
                                                             _outputPath(outputPath),
                                                             _encoder(NULL) {
        int rv = 0;
        rv = WelsCreateSVCEncoder(&_encoder);
        clear();
        SEncParamBase param;
        memset(&param, 0, sizeof(param));
        param.iUsageType = CAMERA_VIDEO_REAL_TIME;
        param.fMaxFrameRate = 30;
        param.iPicWidth = _originalInputWidth;
        param.iPicHeight = _originalInputHeight;
        param.iTargetBitrate = 5000000;
        param.iRCMode = RC_BITRATE_MODE;
//        SEncParamExt param;
//        _encoder->GetDefaultParams(&param);
//
//        param.iUsageType = CAMERA_VIDEO_NON_REAL_TIME;
//        param.fMaxFrameRate = 30;
//        param.iPicWidth = _originalInputWidth;
//        param.iPicHeight = _originalInputHeight;
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
//            param.sSpatialLayers[i].iVideoWidth = _originalInputWidth >> (param.iSpatialLayerNum - 1 - i);
//            param.sSpatialLayers[i].iVideoHeight = _originalInputHeight >> (param.iSpatialLayerNum - 1 - i);
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

        setSize(_originalInputWidth, _originalInputHeight);
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
        _param.iPicWidth = _originalInputWidth;
        _param.iPicHeight = _originalInputHeight;
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


    int encodeOnce(BufferedData &d, SourceFormat format, long long timeStamp, int width, int height, int rotation) {
        _sourcePicture.uiTimeStamp = timeStamp;
        applySourceData(d, format, width, height, rotation);
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

    void applySourceData(BufferedData &source, SourceFormat format, int width, int height, int rotation) {
        if (width == _originalInputWidth && height == _originalInputHeight) {

            if (rotation == 0) {
                libyuv::NV21ToI420(source.data(), width, source.data() + width * height, ((width + 1) / 2) * 2,
                                   _sourcePicture.pData[0], _sourcePicture.iStride[0],
                                   _sourcePicture.pData[1], _sourcePicture.iStride[1],
                                   _sourcePicture.pData[2], _sourcePicture.iStride[2], width, height);
//                NV21toI420((const char *) source.data(), (char *) _buffer.data(), width, height);
            } else {
                _tempBuffer.SetLength(width * height * 3 / 2);
                auto sourceYUV = YUV::from(source.data(), width, height);
                auto tempYUV = YUV::from(_tempBuffer.data(), width, height);
                libyuv::NV21ToI420(sourceYUV.y, sourceYUV.yStride, sourceYUV.u, sourceYUV.uStride,
                                   tempYUV.y, tempYUV.yStride,
                                   tempYUV.u, tempYUV.uStride,
                                   tempYUV.v, tempYUV.vStride, width, height);
                libyuv::I420Rotate(tempYUV.y, tempYUV.yStride, tempYUV.u, tempYUV.uStride, tempYUV.v, tempYUV.vStride,
                                   _sourcePicture.pData[0], _sourcePicture.iStride[0],
                                   _sourcePicture.pData[1], _sourcePicture.iStride[1],
                                   _sourcePicture.pData[2], _sourcePicture.iStride[2], width, height, libyuvRotationForRotationDegrees(rotation));
            }
        } else {
            float sourceAR = (float) width / height;
            float destinationAR = (float) _originalInputWidth / _originalInputHeight;
            float scale = 1;
            if (sourceAR > destinationAR) {
                scale = (float) _originalInputHeight / height;
            } else {
                scale = (float) _originalInputWidth / width;
            }
            int outputWidth = _originalInputWidth / scale;
            int outputHeight = _originalInputHeight / scale;
            int cropX = (width - outputWidth) / 2;
            int cropY = (height - outputHeight) / 2;
            auto sourceYUV = YUV::from(source.data(), width, height);
            _tempBuffer.SetLength(outputWidth * outputHeight * 3 / 2);
            auto outputYUV = YUV::from(_tempBuffer.data(), outputWidth, outputHeight);
            libyuv::ConvertToI420(source.data(), sourceYUV.length(),
                                  outputYUV.y, outputYUV.yStride,
                                  outputYUV.u, outputYUV.uStride,
                                  outputYUV.v, outputYUV.vStride,
                                  cropX, cropY, width, height,
                                  outputWidth, outputHeight, libyuvRotationForRotationDegrees(rotation), libyuvFormatForSourceFormat(format)
            );
            libyuv::I420Scale(outputYUV.y, outputYUV.yStride,
                              outputYUV.u, outputYUV.uStride,
                              outputYUV.v, outputYUV.vStride, outputWidth, outputHeight,
                              _sourcePicture.pData[0], _sourcePicture.iStride[0],
                              _sourcePicture.pData[1], _sourcePicture.iStride[1],
                              _sourcePicture.pData[2], _sourcePicture.iStride[2],
                              _originalInputWidth, _originalInputHeight, libyuv::kFilterLinear);
        }
    }

    //TODO add definitions
    static int libyuvFormatForSourceFormat(SourceFormat format) {
        switch (format) {
            case NV21:
                return libyuv::FOURCC_NV21;
            default:
                return libyuv::FOURCC_NV21;
        }
    }

    static libyuv::RotationMode libyuvRotationForRotationDegrees(int rotationDegree) {
        switch (rotationDegree) {
            case 0:
                return libyuv::kRotate0;
            case 90:
                return libyuv::kRotate90;
            case 180:
                return libyuv::kRotate180;
            case 270:
                return libyuv::kRotate270;
            default:
                return libyuv::kRotate0;
        }
    }

protected:

    int _originalInputWidth;
    int _originalInputHeight;
    std::string _outputPath;
    ISVCEncoder *_encoder;
    BufferedData _buffer;
    SEncParamExt _param;
    SSourcePicture _sourcePicture;
    SFrameBSInfo _info;
    std::ofstream _outputStream;
    BufferedData _tempBuffer;
    int _previouslyEncodedOutputLength;
};


#endif //OPENH264CAMERAVIEW_ENCODER_H
