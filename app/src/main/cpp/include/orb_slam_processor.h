#ifndef SLAM3D_VIDEO_ORB_SLAM_PROCESSOR_H
#define SLAM3D_VIDEO_ORB_SLAM_PROCESSOR_H

#include "bitmap_guard.h"
#include <System.h>
#include <plane.h>

namespace SLAMVideo {
class OrbSlamProcessor {

public:
  /**
   * Constructs an OrbSlamProcessor object with the given vocabulary and configuration file paths.
   *
   * @param vocabFilePath Path to the vocabulary file.
   * @param configFilePath Path to the configuration file.
   */
  OrbSlamProcessor(const std::string& vocabFilePath,const std::string& configFilePath);

  /**
   * Processes a frame captured by a BitmapGuard and returns the resulting transformation matrix.
   *
   * @param bitmapGuard Reference to the BitmapGuard containing the frame.
   * @return The resulting transformation matrix as a cv::Mat pointer.
   */
  cv::Mat* processFrame(SLAMVideo::BitmapGuard &bitmapGuard);

  int getTrackingState(){return mOrbSlamSystem.GetTrackingState();};


  /**
   * Retrieves the map points detected by the ORB-SLAM system.
   *
   * @return A vector of pointers to ORB_SLAM3::MapPoint objects representing the detected map points.
   */
  vector<ORB_SLAM3::MapPoint *> getMapPoints();

  std::vector<cv::KeyPoint> getCurrentKeyPoints();

  /**
   * Retrieves all map points currently present in the map.
   *
   * @return A vector of pointers to MapPoint objects representing all map points in the map.
   */
  std::vector<MapPoint*> getAllMapPoints();

  /**
   * Retrieves reference map points.
   *
   * @return A vector of pointers to MapPoint objects representing reference map points.
   */
  std::vector<MapPoint*> getReferenceMapPoints();

  Plane* detectPlane(int iterations);

  bool getMapChanged();

  /**
   * Retrieves the positions of all keyframes in the map.
   *
   * @return A vector of pointers to cv::Mat objects representing the positions of all keyframes.
   */
  std::vector<cv::Mat*> getAllKeyFramePositions();

  std::recursive_mutex*getAtlasMutex();
private:
  ORB_SLAM3::System mOrbSlamSystem;

};

} // namespace SLAMVideo
#endif // SLAM3D_VIDEO_ORB_SLAM_PROCESSOR_H
