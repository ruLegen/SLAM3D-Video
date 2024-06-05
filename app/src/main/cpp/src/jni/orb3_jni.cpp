#include <jni.h>
#include <System.h>
#include <orb_slam_processor.h>
#include <bitmap_guard.h>
#include <algorithm>
#include <opencv2/core/eigen.hpp>
/**
 * Unpacks the coordinates of a MapPoint along with its referenced status into an output container.
 *
 * @param pt Pointer to the MapPoint object.
 * @param isReferenced Flag indicating whether the MapPoint is referenced.
 * @param outputContainer Vector to store the unpacked coordinates and referenced status.
 */
inline void unpackMapPoint(ORB_SLAM3::MapPoint* pt,bool isReferenced, vector<float>& outputContainer){
  auto worldPos = pt->GetWorldPos();
  outputContainer.push_back(worldPos(0));
  outputContainer.push_back(worldPos(1));
  outputContainer.push_back(worldPos(2));
  outputContainer.push_back(isReferenced);
}
/**
 * Initializes the ORB-SLAM system with the given vocabulary and configuration file paths.
 *
 * @param vocab_file_name File name of the vocabulary.
 * @param config_file_name File name of the configuration.
 * @return Pointer to the initialized ORB-SLAM processor.
 */
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

/**
 * Processes a bitmap frame using the ORB-SLAM system.
 *
 * @param ptr Pointer to the ORB-SLAM processor.
 * @param bitmap Bitmap object representing the frame.
 * @return Pointer to the transformation matrix.
 */
extern "C" JNIEXPORT jlong JNICALL
Java_com_mag_slam3dvideo_orb3_OrbSlamProcessor_nProcessBitmap(JNIEnv *env, jobject thiz, jlong ptr,jobject bitmap) {

    auto *processor = reinterpret_cast<SLAMVideo::OrbSlamProcessor*>(ptr);
    if (processor == nullptr)
      return 0;
    auto inputGuard = SLAMVideo::BitmapGuard(env, bitmap);
    cv::Mat* Tcw = processor->processFrame(inputGuard);
    return reinterpret_cast<long>(Tcw);
}
/**
 * Retrieves the positions of map points detected by the ORB-SLAM system.
 *
 * @param ptr Pointer to the ORB-SLAM processor.
 * @return Array of pointers to the map point positions.
 */
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
/**
 * Retrieves the key points of the current frame.
 *
 * @param ptr Pointer to the ORB-SLAM processor.
 * @return Array of unpacked key points represented as floats.
 */
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
/**
 * Retrieves the map points of the current map.
 *
 * @param ptr Pointer to the ORB-SLAM processor.
 * @return Array of unpacked map points represented as floats.
 */

extern "C" JNIEXPORT jfloatArray
Java_com_mag_slam3dvideo_orb3_OrbSlamProcessor_nGetCurrentMapPoints(JNIEnv *env, jobject thiz, jlong ptr) {
    static int numberOfMapPointMembers =4; //keep in sync with OrbSlamProcessor.kt
    auto *processor = reinterpret_cast<SLAMVideo::OrbSlamProcessor*>(ptr);
    if (processor == nullptr)
      return 0;
    try {

        auto mutex = processor->getAtlasMutex();
        unique_lock<std::recursive_mutex> lock(*mutex);

        const vector<ORB_SLAM3::MapPoint*> allMapPoints = processor->getAllMapPoints();
        const vector<ORB_SLAM3::MapPoint*> referenceMapPoints =
            processor->getReferenceMapPoints();
        set<ORB_SLAM3::MapPoint*> setRefMapPoints(referenceMapPoints.begin(),referenceMapPoints.end());

        if(allMapPoints.empty())
          return 0;

        std::vector<float> unpackedMapPoints;
        int reservePointCount = std::max(allMapPoints.size(),setRefMapPoints.size());
        unpackedMapPoints.reserve(reservePointCount *numberOfMapPointMembers);

        for(size_t i=0, iend=allMapPoints.size(); i<iend;i++)
        {
          if(allMapPoints[i]->isBad() || setRefMapPoints.count(allMapPoints[i]))
            continue;
          auto pt = allMapPoints[i];
          unpackMapPoint(pt,false,unpackedMapPoints);
        }
        for(set<ORB_SLAM3::MapPoint*>::iterator sit= setRefMapPoints.begin(), send= setRefMapPoints.end(); sit!=send; sit++)
        {
          if((*sit)->isBad())
            continue;
          unpackMapPoint(*sit,true,unpackedMapPoints);
        }
        jfloatArray floatArray = env->NewFloatArray(unpackedMapPoints.size());
        env->SetFloatArrayRegion(floatArray,0, unpackedMapPoints.size(),
                                 unpackedMapPoints.data());
        return floatArray;

    }catch (std::exception ex){
      return 0;
    }
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

extern "C" JNIEXPORT jboolean Java_com_mag_slam3dvideo_orb3_OrbSlamProcessor_nMapChanged(JNIEnv *env, jobject thiz, jlong ptr) {
    auto *processor = reinterpret_cast<SLAMVideo::OrbSlamProcessor*>(ptr);
    if (processor == nullptr)
      return false;

    bool changed = processor->getMapChanged();
    return changed;
}
extern "C" JNIEXPORT jlongArray Java_com_mag_slam3dvideo_orb3_OrbSlamProcessor_nGetKeyFramePositions(JNIEnv *env, jobject thiz, jlong ptr){
    auto *processor = reinterpret_cast<SLAMVideo::OrbSlamProcessor*>(ptr);
    if (processor == nullptr)
      return 0;
    std::vector<cv::Mat*> res = processor->getAllKeyFramePositions();
    std::vector<long> matPtrs;
    matPtrs.reserve(res.size());
    for(auto matPtr:res){
      if(matPtr == NULL)
        continue;
      matPtrs.push_back(reinterpret_cast<long>(matPtr));
    }
    jlongArray longArray = env->NewLongArray(matPtrs.size());
    env->SetLongArrayRegion(longArray,0, matPtrs.size(),
                             matPtrs.data());
    return longArray;
}