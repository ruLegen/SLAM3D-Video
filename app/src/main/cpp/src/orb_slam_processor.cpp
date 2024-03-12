#include <orb_slam_processor.h>

namespace SLAMVideo {

OrbSlamProcessor::OrbSlamProcessor(const std::string &vocabFilePath,
                                   const std::string &configFilePath)
    : mOrbSlamSystem(vocabFilePath, configFilePath,ORB_SLAM3::System::MONOCULAR, false) {
  
}
} // namespace SLAMVideo
