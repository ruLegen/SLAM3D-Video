#include <plane.h>
#include <opencv2/core/eigen.hpp>
namespace SLAMVideo {

const float eps = 1e-4;
cv::Mat ExpSO3(const float& x, const float& y, const float& z)
{
  cv::Mat I = cv::Mat::eye(3, 3, CV_32F);
  const float d2 = x * x + y * y + z * z;
  const float d = sqrt(d2);
  cv::Mat W = (cv::Mat_<float>(3, 3) <<
               0, -z, y,
               z, 0, -x,
               -y, x, 0);
  if (d < eps)
    return (I + W + 0.5f * W * W);
  else
    return (I + W * sin(d) / d + W * W * (1.0f - cos(d)) / d2);
}

cv::Mat ExpSO3(const cv::Mat& v)
{
  return ExpSO3(v.at<float>(0), v.at<float>(1), v.at<float>(2));
}


Plane::Plane(const std::vector<MapPoint *> &vMPs, const cv::Mat &Tcw)
    : mvMPs(vMPs), mTcw(Tcw.clone()) {

  rang = -3.14f / 2 + ((float)rand() / RAND_MAX) * 3.14f; //[-90 + random(0,180)]
  Recompute();
}

void Plane::Recompute() {
  const int N = mvMPs.size();

  // Recompute plane with all points
  // REserve matrix with N rows and 4 columns
  cv::Mat A = cv::Mat(N, 4, CV_32F);

  // last column is 1 | x,y,z,1
  A.col(3) = cv::Mat::ones(N, 1, CV_32F);

  // origin (0,0,0)
  o = cv::Mat::zeros(3, 1, CV_32F);

  int nPoints = 0;
  for (int i = 0; i < N; i++) {
    MapPoint *pMP = mvMPs[i];
    if (!pMP->isBad()) {
      cv::Mat Xw;
      eigen2cv(pMP->GetWorldPos(),Xw);
      o += Xw;
      // put point in nPoints'th row of A matrix
      A.row(nPoints).colRange(0, 3) = Xw.t();
      nPoints++;
    }
  }
  // resize matrix that way so rows are equal to "good" points count
  A.resize(nPoints);
  o = o * (1.0f / nPoints); /// mnean center of plane

  // now A is nPoints x 4 (m x n)
  cv::Mat u, w, vt;
  // w (m x m) - rotation
  // u (m x n) - scale  or (singular values)
  // vt (n * n) - rotation
  // A good definition of a plane uses the normal vector. Any point Z in the
  // plane has the property that dot(any point - o,vt(:,3)) == 0	// means
  // that point on plane
  // https://www.mathworks.com/matlabcentral/answers/496215-svd-and-basis-of-a-plane
  // have no idea why vt shows a normal but let's take it as granted
  cv::SVDecomp(A, w, u, vt, cv::SVD::MODIFY_A | cv::SVD::FULL_UV);

  float a = vt.at<float>(3, 0);
  float b = vt.at<float>(3, 1);
  float c = vt.at<float>(3, 2);

  const float f =
      1.0f / sqrt(a * a + b * b + c * c); // inverse length? of rotation vector?

  // Compute XC just the first time
  if (XC.empty()) {
    /**
    * -mTcw[3x3] * mTcw[3x1]
    *
            const cv::Mat Rcw = mTcw.rowRange(0,3).colRange(0,3);
            const cv::Mat tcw = mTcw.rowRange(0,3).col(3);
            const cv::Mat twc = -Rcw.t()*tcw;
            //https://russianblogs.com/article/37022699394/

            OR
             from \slam\KeyFrame.cpp
             cv::Mat Rcw = Tcw.rowRange(0,3).colRange(0,3);
             cv::Mat tcw = Tcw.rowRange(0,3).col(3);
             cv::Mat Rwc = Rcw.t();
             Oc = -Rwc*tcw;
             Oc -s camera pose in world coordinate system
    */

    cv::Mat Oc =
        -mTcw.colRange(0, 3).rowRange(0, 3).t() * mTcw.rowRange(0, 3).col(3);
    XC = Oc - o;
  }

  if ((XC.at<float>(0) * a + XC.at<float>(1) * b + XC.at<float>(2) * c) > 0) {
    a = -a;
    b = -b;
    c = -c;
  }

  const float nx = a * f; // normal X
  const float ny = b * f; // normal Y
  const float nz = c * f; // noraml Z

  n = (cv::Mat_<float>(3, 1) << nx, ny, nz);

  /**
  * [0]
    [1]
    [0]
  */
  cv::Mat up = (cv::Mat_<float>(3, 1) << 0.0f, 1.0f, 0.0f);

  /**
   *
   *   /|\ up
   *    |
   *    |  / v
   *    | /
   *    |/
   *    |--------------> n
   */
  cv::Mat v = up.cross(n);
  const float sa = cv::norm(v); // magnitude   ?

  /**
   *
   *   /|\ up
   *    |
   *    |  / n
   *    | /  |
   *    |/   |
   *    |-------------->
   *         *ca
   */
  const float ca = up.dot(n);

  /**
   * rad = Math.Atan2(Cross(up,n), Dot(up,n));
   */
  const float ang = atan2(sa, ca);

  /**
   * [1,0,0,0]
   * [0,1,0,0]
   * [0,0,1,0]
   * [0,0,0,1]
   */
  // World to plane Rototranslation
  Tpw = cv::Mat::eye(4, 4, CV_32F);

  /**
   * v = up.cross(plane_normal);
   * [x1,x2,x3]
   * [x4,x5,x6]  = Rotation(v.x,v.y,v.z) * Rotation(0,up*arbitrary_angle,0)
   * [x7,x8,x9]
   */
  Tpw.rowRange(0, 3).colRange(0, 3) = ExpSO3(v * ang / sa) * ExpSO3(up * rang);

  /**
   * [tx]	  [o.x]
   * [ty]  = [o.y]
   * [tz]    [o.z]
   */
  o.copyTo(Tpw.col(3).rowRange(0, 3));

  /**
   *
   *       [x1,x2,x3,tx]
   * Tpw = [x4,x5,x6,ty]
   *       [x7,x8,x9,tz]
   *       [ 0, 0, 0, 1]
   */

// glTpw = ColumnMajor(Tpw)
//  glTpw.m[0] = Tpw.at<float>(0, 0);
//  glTpw.m[1] = Tpw.at<float>(1, 0);
//  glTpw.m[2] = Tpw.at<float>(2, 0);
//  glTpw.m[3] = 0.0;
//
//  glTpw.m[4] = Tpw.at<float>(0, 1);
//  glTpw.m[5] = Tpw.at<float>(1, 1);
//  glTpw.m[6] = Tpw.at<float>(2, 1);
//  glTpw.m[7] = 0.0;
//
//  glTpw.m[8] = Tpw.at<float>(0, 2);
//  glTpw.m[9] = Tpw.at<float>(1, 2);
//  glTpw.m[10] = Tpw.at<float>(2, 2);
//  glTpw.m[11] = 0.0;
//
//  glTpw.m[12] = Tpw.at<float>(0, 3);
//  glTpw.m[13] = Tpw.at<float>(1, 3);
//  glTpw.m[14] = Tpw.at<float>(2, 3);
//  glTpw.m[15] = 1.0;
}

Plane::Plane(const float &nx, const float &ny, const float &nz, const float &ox,
             const float &oy, const float &oz) {
  n = (cv::Mat_<float>(3, 1) << nx, ny, nz);
  o = (cv::Mat_<float>(3, 1) << ox, oy, oz);

  cv::Mat up = (cv::Mat_<float>(3, 1) << 0.0f, 1.0f, 0.0f);

  cv::Mat v = up.cross(n);
  const float s = cv::norm(v);
  const float c = up.dot(n);
  const float a = atan2(s, c);
  Tpw = cv::Mat::eye(4, 4, CV_32F);
  const float rang = -3.14f / 2 + ((float)rand() / RAND_MAX) * 3.14f;
  cout << rang;
  Tpw.rowRange(0, 3).colRange(0, 3) = ExpSO3(v * a / s) * ExpSO3(up * rang);
  o.copyTo(Tpw.col(3).rowRange(0, 3));

//  glTpw.m[0] = Tpw.at<float>(0, 0);
//  glTpw.m[1] = Tpw.at<float>(1, 0);
//  glTpw.m[2] = Tpw.at<float>(2, 0);
//  glTpw.m[3] = 0.0;
//
//  glTpw.m[4] = Tpw.at<float>(0, 1);
//  glTpw.m[5] = Tpw.at<float>(1, 1);
//  glTpw.m[6] = Tpw.at<float>(2, 1);
//  glTpw.m[7] = 0.0;
//
//  glTpw.m[8] = Tpw.at<float>(0, 2);
//  glTpw.m[9] = Tpw.at<float>(1, 2);
//  glTpw.m[10] = Tpw.at<float>(2, 2);
//  glTpw.m[11] = 0.0;
//
//  glTpw.m[12] = Tpw.at<float>(0, 3);
//  glTpw.m[13] = Tpw.at<float>(1, 3);
//  glTpw.m[14] = Tpw.at<float>(2, 3);
//  glTpw.m[15] = 1.0;
}

} // namespace SLAMVideo