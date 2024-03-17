#include <jni.h>
#include <System.h>
#include <orb_slam_processor.h>
#include <bitmap_guard.h>
#include <algorithm>
#include <opencv2/core/eigen.hpp>

extern "C"
JNIEXPORT jlong JNICALL
Java_com_mag_slam3dvideo_orb3_OrbSlamProcessor_nInitOrb(JNIEnv *env, jobject thiz, jstring vocab_file_name,
                                           jstring config_file_name) {

    jboolean isCopy;
    const char *vocabFileNameBytes = (env)->GetStringUTFChars(vocab_file_name, &isCopy);
    const char *configFileNameBytes = (env)->GetStringUTFChars(config_file_name, &isCopy);
    std::string vocabFile = vocabFileNameBytes;
    std::string configFile = configFileNameBytes;

    auto *SLAM = new SLAMVideo::OrbSlamProcessor(vocabFile,configFile);

    env->ReleaseStringUTFChars(vocab_file_name,vocabFileNameBytes);
    env->ReleaseStringUTFChars(config_file_name,configFileNameBytes);

    return reinterpret_cast<jlong>(SLAM);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_mag_slam3dvideo_orb3_OrbSlamProcessor_nProcessBitmap(JNIEnv *env, jobject thiz, jlong ptr,jobject bitmap) {

    auto *processor = reinterpret_cast<SLAMVideo::OrbSlamProcessor*>(ptr);
    if (processor == nullptr)
      return 0;
    auto inputGuard = SLAMVideo::BitmapGuard(env, bitmap);
    cv::Mat* Tcw = processor->processFrame(inputGuard);
    return reinterpret_cast<long>(Tcw);
}

extern "C" JNIEXPORT jlongArray
Java_com_mag_slam3dvideo_orb3_OrbSlamProcessor_nGetMapPointsPositions(JNIEnv *env, jobject thiz, jlong ptr) {
    auto *processor = reinterpret_cast<SLAMVideo::OrbSlamProcessor*>(ptr);
    if (processor == nullptr)
      return 0;

    std::vector<ORB_SLAM3::MapPoint*> mapPoints = processor->getMapPoints();
    auto matPointers = std::vector<long>();
    for(auto matPtr:mapPoints){
      if(matPtr == NULL)
        continue ;
      auto* cvMat = new cv::Mat();
      cv::eigen2cv(matPtr->GetWorldPos(),*cvMat);
      matPointers.push_back(reinterpret_cast<long>(cvMat));
    }
    jlongArray longJavaArray = env->NewLongArray(matPointers.size());
    env->SetLongArrayRegion(longJavaArray,0,matPointers.size(),matPointers.data());
    return  longJavaArray;
}

extern "C" JNIEXPORT jfloatArray
Java_com_mag_slam3dvideo_orb3_OrbSlamProcessor_nGetCurrentFrameKeyPoints(JNIEnv *env, jobject thiz, jlong ptr) {
    static int numberOfKeyPointMembers =2; //keep in sync with OrbSlamProcessor.kt

    auto *processor = reinterpret_cast<SLAMVideo::OrbSlamProcessor*>(ptr);
    if (processor == nullptr)
      return 0;
    std::vector<cv::KeyPoint> keyPoints = processor->getCurrentKeyPoints();
    if(keyPoints.size() == 0)
      return 0;

    std::vector<float> unpackedKeyPoints;
    unpackedKeyPoints.reserve(keyPoints.size()*numberOfKeyPointMembers);
    for(const auto& kp:keyPoints){
      unpackedKeyPoints.push_back(kp.pt.x);
      unpackedKeyPoints.push_back(kp.pt.y);
    }
    jfloatArray floatArray = env->NewFloatArray(unpackedKeyPoints.size());
    env->SetFloatArrayRegion(floatArray,0,unpackedKeyPoints.size(),unpackedKeyPoints.data());
    return floatArray;
}

extern "C" JNIEXPORT jint
Java_com_mag_slam3dvideo_orb3_OrbSlamProcessor_nGetTrackingState(JNIEnv *env, jobject thiz, jlong ptr) {
    auto *processor = reinterpret_cast<SLAMVideo::OrbSlamProcessor*>(ptr);
    if (processor == nullptr)
      return 0;
    return processor->getTrackingState();
}

extern "C" JNIEXPORT jlong
Java_com_mag_slam3dvideo_orb3_OrbSlamProcessor_nDetectPlane(JNIEnv *env, jobject thiz, jlong ptr) {
    auto *processor = reinterpret_cast<SLAMVideo::OrbSlamProcessor*>(ptr);
    if (processor == nullptr)
      return 0;
    SLAMVideo::Plane* plane =  processor->detectPlane(50);
    return reinterpret_cast<long>(plane);
}