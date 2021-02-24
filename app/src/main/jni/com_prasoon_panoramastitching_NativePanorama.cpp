#include "com_prasoon_panoramastitching_NativePanorama.h"
#include "opencv2/opencv.hpp"
#include "opencv2/stitching.hpp"

using namespace std;
using namespace cv;

JNIEXPORT jint JNICALL Java_com_prasoon_panoramastitching_NativePanorama_processPanorama
  (JNIEnv *env, jclass clazz, jlongArray imageAddressArray, jlong outputAddress){
  jint ret=1;
  // Get the length of the long array
  jsize a_len = env->GetArrayLength(imageAddressArray);
  // Convert the jlongArray to an array of jlong
  jlong *imgAddressArr = env->GetLongArrayElements(imageAddressArray, 0);
  // Create a vector to store all the image
  vector<Mat> imgVec;

    for(int k=0;k<a_len;k++){
        // Get the image
        Mat & curimage=*(Mat*)imgAddressArr[k];
        Mat newimage;

        // Convert to a 3 channel Mat to use with Stitcher module
        cvtColor(curimage, newimage, CV_RGBA2RGB);

        // Reduce the resolution for fast computation
        float scale = 1000.0f / curimage.rows;
        resize(newimage, newimage, Size(scale * curimage.rows, scale * curimage.cols));
        // resize(newimage, newimage, Size(800,600));

        imgVec.push_back(newimage);
      }

  Mat & result  = *(Mat*) outputAddress;

  Stitcher stitcher = Stitcher::createDefault(true);
  Stitcher::Status status = stitcher.stitch(imgVec, result);
  stitcher.setRegistrationResol(-1); /// 0.6
  stitcher.setSeamEstimationResol(-1);   /// 0.1
  stitcher.setCompositingResol(-1);   //1
  stitcher.setPanoConfidenceThresh(-1);   //1
  stitcher.setWaveCorrection(true);
  stitcher.setWaveCorrectKind(detail::WAVE_CORRECT_HORIZ);

   if (status != Stitcher::OK){
                  ret=0;
   }else{
         cv::cvtColor(result, result, CV_BGR2RGBA, 4);
         }

  // Release the jlong array
  env->ReleaseLongArrayElements(imageAddressArray, imgAddressArr ,0);
  return ret;

}


JNIEXPORT jstring JNICALL Java_com_prasoon_panoramastitching_NativePanorama_getMessageFromJni
  (JNIEnv *env, jclass obj){
  return env->NewStringUTF("This is a message from JNI");
  }