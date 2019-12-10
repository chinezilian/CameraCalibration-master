LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=on
OPENCV_LIB_TYPE:=SHARED

include /Users/poly/workspace/OpenCV-2.4.8-android-sdk/sdk/native/jni/OpenCV.mk
