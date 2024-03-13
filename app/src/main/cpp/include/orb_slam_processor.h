#ifndef SLAM3D_VIDEO_ORB_SLAM_PROCESSOR_H
#define SLAM3D_VIDEO_ORB_SLAM_PROCESSOR_H

#include "bitmap_guard.h"
#include <System.h>
namespace SLAMVideo {
class OrbSlamProcessor {

public:
  OrbSlamProcessor(const std::string& vocabFilePath,const std::string& configFilePath);

  int processFrame(SLAMVideo::BitmapGuard &bitmapGuard);

private:
  ORB_SLAM3::System mOrbSlamSystem;
};

} // namespace SLAMVideo
#endif // SLAM3D_VIDEO_ORB_SLAM_PROCESSOR_H
