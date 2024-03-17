#ifndef SLAM3D_VIDEO_ORB_SLAM_PROCESSOR_H
#define SLAM3D_VIDEO_ORB_SLAM_PROCESSOR_H

#include "bitmap_guard.h"
#include <System.h>
namespace SLAMVideo {
class OrbSlamProcessor {

public:
  OrbSlamProcessor(const std::string& vocabFilePath,const std::string& configFilePath);

  cv::Mat* processFrame(SLAMVideo::BitmapGuard &bitmapGuard);

  int getTrackingState(){return mOrbSlamSystem.GetTrackingState();};

  vector<ORB_SLAM3::MapPoint *> getMapPoints();

  std::vector<cv::KeyPoint> getCurrentKeyPoints();

private:
  ORB_SLAM3::System mOrbSlamSystem;
};

} // namespace SLAMVideo
#endif // SLAM3D_VIDEO_ORB_SLAM_PROCESSOR_H
