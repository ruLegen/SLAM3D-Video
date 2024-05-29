#include <log.h>
#include <Eigen/Dense>
#include <opencv2/core/eigen.hpp>
#include <orb_slam_processor.h>

using TrackingState = ORB_SLAM3::Tracking::eTrackingState;
using MapPoint = ORB_SLAM3::MapPoint;

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
Plane* OrbSlamProcessor::detectPlane(int iterations) {
  // Retrieve 3D points
  cv::Mat Tcw;
  cv::eigen2cv(mOrbSlamSystem.GetTracking()->mCurrentFrame.mTcw.matrix(),Tcw);

  const auto& vMPs = mOrbSlamSystem.GetTrackedMapPoints();
  std::vector<cv::Mat> vPoints;
  vPoints.reserve(vMPs.size());
  std::vector<MapPoint*> vPointMP;
  vPointMP.reserve(vMPs.size());

  for (size_t i = 0; i < vMPs.size(); i++)
  {
    MapPoint* pMP = vMPs[i];
    if (pMP)
    {
      if (pMP->Observations() >= 5)
      {
          auto eigenPos=pMP->GetWorldPos();
          cv::Mat pos;
          cv::eigen2cv(eigenPos,pos);
          vPoints.push_back(pos);
          vPointMP.push_back(pMP);
      }
    }
  }

  const int N = vPoints.size();
  if (N < 20)
    return NULL;


  // Indices for minimum set selection
  vector<size_t> vAllIndices; vAllIndices.reserve(N);
  vector<size_t> vAvailableIndices;

  for (int i = 0; i < N; i++)
  {
    vAllIndices.push_back(i);
  }

  float bestDist = 1e10;
  vector<float> bestvDist;

  //RANSAC
  for (int n = 0; n < iterations; n++)
  {
    vAvailableIndices = vAllIndices;

    cv::Mat A(3, 4, CV_32F);
    A.col(3) = cv::Mat::ones(3, 1, CV_32F);

    // Get min set of points
    for (short i = 0; i < 3; ++i)
    {
      int randi = DUtils::Random::RandomInt(0, vAvailableIndices.size() - 1);

      int idx = vAvailableIndices[randi];

      A.row(i).colRange(0, 3) = vPoints[idx].t();

      // unordered fast delete
      vAvailableIndices[randi] = vAvailableIndices.back();
      vAvailableIndices.pop_back();
    }

    cv::Mat u, w, vt;
    cv::SVDecomp(A, w, u, vt, cv::SVD::MODIFY_A | cv::SVD::FULL_UV);

    const float a = vt.at<float>(3, 0);
    const float b = vt.at<float>(3, 1);
    const float c = vt.at<float>(3, 2);
    const float d = vt.at<float>(3, 3);

    vector<float> vDistances(N, 0);

    const float f = 1.0f / sqrt(a * a + b * b + c * c + d * d);

    for (int i = 0; i < N; i++)
    {
      vDistances[i] = fabs(vPoints[i].at<float>(0) * a + vPoints[i].at<float>(1) * b + vPoints[i].at<float>(2) * c + d) * f;
    }

    vector<float> vSorted = vDistances;
    sort(vSorted.begin(), vSorted.end());

    int nth = max((int)(0.2 * N), 20);
    const float medianDist = vSorted[nth];

    if (medianDist < bestDist)
    {
      bestDist = medianDist;
      bestvDist = vDistances;
    }
  }

  // Compute threshold inlier/outlier
  const float th = 1.4 * bestDist;
  vector<bool> vbInliers(N, false);
  int nInliers = 0;
  for (int i = 0; i < N; i++)
  {
    if (bestvDist[i] < th)
    {
      nInliers++;
      vbInliers[i] = true;
    }
  }

  vector<MapPoint*> vInlierMPs(nInliers, NULL);
  int nin = 0;
  for (int i = 0; i < N; i++)
  {
    if (vbInliers[i])
    {
      vInlierMPs[nin] = vPointMP[i];
      nin++;
    }
  }
  return new Plane(vInlierMPs, Tcw);
}
std::vector<MapPoint *> OrbSlamProcessor::GetAllMapPoints() {
  return mOrbSlamSystem.GetAtlas()->GetAllMapPoints();
}
std::recursive_mutex* OrbSlamProcessor::GetAtlasMutex() {
    return mOrbSlamSystem.GetAtlas()->GetAtlasMutex();
}

std::vector<MapPoint *> OrbSlamProcessor::GetReferenceMapPoints() {
  return mOrbSlamSystem.GetAtlas()->GetReferenceMapPoints();
}
bool OrbSlamProcessor::getMapChanged() {
  return mOrbSlamSystem.MapChanged();
}
vector<cv::Mat*> OrbSlamProcessor::GetAllKeyFramePositions() {
  auto tracker = mOrbSlamSystem.GetTracking();
  auto poses = tracker->mlRelativeFramePoses;

  std::vector<cv::Mat*> resultVector;
  Eigen::Matrix4f initialPose;
  list<ORB_SLAM3::KeyFrame*>::iterator lRit = tracker->mlpReferences.begin();
  for(auto lit=tracker->mlRelativeFramePoses.begin(),lend=tracker->mlRelativeFramePoses.end();lit!=lend;lit++, lRit++){
    ORB_SLAM3::KeyFrame* pKF = *lRit;

    while(pKF->isBad())
    {
      pKF = pKF->GetParent();
    }
     auto tcwSophus = pKF->GetPose();
     auto relative = tcwSophus.matrix() * (*lit).matrix();
     Eigen::Matrix4f Tcw_Matrix = relative.matrix();
     cv::Mat* cvT = new cv::Mat(4,4,CV_32FC1);
     cv::eigen2cv(Tcw_Matrix, *cvT);
     resultVector.push_back(cvT);
  }

  return resultVector;
//  auto mpAtlas = mOrbSlamSystem.GetAtlas();
//
//  std::vector<ORB_SLAM3::KeyFrame*> vpKFs = mpAtlas->GetAllKeyFrames();
//  sort(vpKFs.begin(),vpKFs.end(),ORB_SLAM3::KeyFrame::lId);
//  std::vector<cv::Mat*> resultVector;
//  resultVector.reserve(vpKFs.size());
//  // Transform all keyframes so that the first keyframe is at the origin.
//  // After a loop closure the first keyframe might not be at the origin.
//  for(size_t i=0; i<vpKFs.size(); i++)
//  {
//    ORB_SLAM3::KeyFrame* pKF = vpKFs[i];
//
//    // pKF->SetPose(pKF->GetPose()*Two);
//
//    if(pKF->isBad())
//      continue;
//    auto tcwSophus = pKF->GetPose();
//    Eigen::Matrix4f Tcw_Matrix = tcwSophus.matrix();
//    cv::Mat* cvT = new cv::Mat(4,4,CV_32FC1);
//    cv::eigen2cv(Tcw_Matrix, *cvT);
//  resultVector.push_back(cvT);
//  }
//  return resultVector;
}

} // namespace SLAMVideo
