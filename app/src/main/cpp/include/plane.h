#ifndef SLAM3D_VIDEO_PLANE_H
#define SLAM3D_VIDEO_PLANE_H

#include <System.h>

using MapPoint = ORB_SLAM3::MapPoint;

namespace SLAMVideo {
class Plane {
public:
  Plane(const std::vector<MapPoint *> &vMPs, const cv::Mat& Tcw);
  Plane(const float &nx, const float &ny, const float &nz, const float &ox,
        const float &oy, const float &oz);

  void Recompute();

  // normal
  cv::Mat n;
  // origin
  cv::Mat o;
  // arbitrary orientation along normal
  float rang;
  // transformation from world to the plane
  cv::Mat Tpw;
  // pangolin::OpenGlMatrix glTpw;
  // MapPoints that define the plane
  std::vector<MapPoint *> mvMPs;
  // camera pose when the plane was first observed (to compute normal direction)
  cv::Mat mTcw, XC;
};
}

#endif // SLAM3D_VIDEO_PLANE_H
