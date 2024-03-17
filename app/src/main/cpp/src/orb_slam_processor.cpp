#include <log.h>
#include <Eigen/Dense>
#include <opencv2/core/eigen.hpp>
#include <orb_slam_processor.h>
using TrackingState = ORB_SLAM3::Tracking::eTrackingState;

namespace SLAMVideo {

static std::vector<ORB_SLAM3::IMU::Point> imuData{};
OrbSlamProcessor::OrbSlamProcessor(const std::string &vocabFilePath,
                                   const std::string &configFilePath)
    : mOrbSlamSystem(vocabFilePath, configFilePath,
                     ORB_SLAM3::System::MONOCULAR, false) {}
cv::Mat* OrbSlamProcessor::processFrame(BitmapGuard &bitmapGuard) {
  uint8_t *bitmapPtr = bitmapGuard.get();
  int32_t bitmapFormat = bitmapGuard.format();

  auto size = cv::Size(bitmapGuard.width(), bitmapGuard.height());
  bool isValidFormat = bitmapFormat == ANDROID_BITMAP_FORMAT_RGB_565 ||
                       bitmapFormat == ANDROID_BITMAP_FORMAT_RGBA_8888;
  if (!isValidFormat)
    throw std::runtime_error(std::string("bitmap has unsupported format ") +
                             std::to_string(bitmapFormat));

  auto matFormat = CV_8UC4;
  if (bitmapFormat == ANDROID_BITMAP_FORMAT_RGB_565) {
    matFormat = CV_8UC2;
  }
  auto mat = cv::Mat(size, matFormat, bitmapPtr);

  double frameTime = 0;
  Sophus::SE3f tcwSophus = mOrbSlamSystem.TrackMonocular(mat, frameTime, imuData, "");
  Eigen::Matrix4f Tcw_Matrix = tcwSophus.matrix();
  cv::Mat* cvT = new cv::Mat(4,4,CV_32FC1);
  cv::eigen2cv(Tcw_Matrix, *cvT);
  return cvT;
}
vector<ORB_SLAM3::MapPoint *> OrbSlamProcessor::getMapPoints() {
  return mOrbSlamSystem.GetTrackedMapPoints();
}
vector<cv::KeyPoint> OrbSlamProcessor::getCurrentKeyPoints() {
  auto tracker = mOrbSlamSystem.GetTracking();
  if(tracker->mLastProcessedState == TrackingState::NOT_INITIALIZED)
    return {};

  const std::vector<cv::KeyPoint>& allKeys = tracker->mCurrentFrame.mvKeys;
  const int N = allKeys.size();
  std::vector<cv::KeyPoint> inlinersKeys;
  inlinersKeys.reserve(N);
  for(int i=0;i<N;i++)
  {
    ORB_SLAM3::MapPoint * pMP = tracker->mCurrentFrame.mvpMapPoints[i];
    if(pMP)
    {
      if(!tracker->mCurrentFrame.mvbOutlier[i])
      {
          inlinersKeys.push_back(allKeys[i]);
      }
    }
  }

  return inlinersKeys;
}
} // namespace SLAMVideo
