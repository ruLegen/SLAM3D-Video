#include <math.h>
#include "ImuTypes.h"
#include "Thirdparty/opencv/include/opencv2/core/hal/interface.h"
#include "Thirdparty/opencv/include/opencv2/imgproc.hpp"
#include <jni.h>

void test(){
    int i = 0;
    auto p = ORB_SLAM3::IMU::Point(1,2,3,4,5,6,7);
    auto ss =p.t;
    auto mat = cv::Mat::eye(4,4,CV_32F);
    auto rotationMat = cv::getRotationMatrix2D(cv::Point(0,0),170,3);
    auto res = mat * rotationMat;

}

extern "C"
JNIEXPORT void JNICALL
Java_com_mag_slam3dvideo_MainActivity_nativeTest(JNIEnv *env, jobject thiz) {
    test();
}