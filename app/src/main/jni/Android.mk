LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

#opencv
OPENCVROOT:= D:\AndroidStudioProjects\opencv-2.4.13.2-android-sdk\OpenCV-android-sdk
OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=on
OPENCV_LIB_TYPE:=SHARED
include ${OPENCVROOT}/sdk/native/jni/OpenCV.mk

LOCAL_SRC_FILES := com_prasoon_panoramastitching_NativePanorama.cpp

LOCAL_LDLIBS += -llog
LOCAL_MODULE := MyLibs

#LOCAL_STATIC_LIBRARIES += libopencv_stitching.a

#LOCAL_LDLIBS += -L$(OPENCVROOT)/libs/armeabi-v7a/
#LOCAL_LDLIBS += -lopencv_stitching

include $(BUILD_SHARED_LIBRARY)