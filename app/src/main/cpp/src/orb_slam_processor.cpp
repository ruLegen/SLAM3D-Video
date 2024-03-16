#include <orb_slam_processor.h>
#include <log.h>
using TrackingState = ORB_SLAM3::Tracking::eTrackingState;

namespace SLAMVideo {

static std::vector<ORB_SLAM3::IMU::Point> imuData{};
OrbSlamProcessor::OrbSlamProcessor(const std::string &vocabFilePath,
                                   const std::string &configFilePath)
    : mOrbSlamSystem(vocabFilePath, configFilePath,
                     ORB_SLAM3::System::MONOCULAR, false) {}
int OrbSlamProcessor::processFrame(BitmapGuard &bitmapGuard) {
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
  mOrbSlamSystem.TrackMonocular(mat, frameTime, imuData, "");
  return mOrbSlamSystem.GetTrackingState();
}
vector<ORB_SLAM3::MapPoint *> OrbSlamProcessor::getMapPoints() {
  return mOrbSlamSystem.GetTrackedMapPoints();
}
vector<cv::KeyPoint> OrbSlamProcessor::getCurrentKeyPoints() {
  return mOrbSlamSystem.GetTrackedKeyPointsUn();
}
} // namespace SLAMVideo
