#include <plane.h>
#include <jni.h>

extern "C" JNIEXPORT void JNICALL
Java_com_mag_slam3dvideo_orb3_Plane_nDestroyPlane(JNIEnv *env, jobject thiz, jlong ptr){
  if(ptr == NULL)
    return ;

  delete reinterpret_cast<SLAMVideo::Plane*>(ptr);
}
extern "C" JNIEXPORT jfloatArray
Java_com_mag_slam3dvideo_orb3_Plane_nGetGLTpw(JNIEnv *env, jobject thiz, jlong ptr) {
  auto* plane = reinterpret_cast<SLAMVideo::Plane*>(ptr);
  if(plane == nullptr)
    return 0;

  const auto& Tpw = plane->Tpw;
  if(Tpw.cols != 4 || Tpw.rows != 4 || Tpw.type() != CV_32FC1){
    throw std::runtime_error("Invalid matrix type");
  }
  float glMat[16];
  // convert rowMajor opencv mat to column major opengl mat
  glMat[0] = Tpw.at<float>(0, 0);
  glMat[1] = Tpw.at<float>(1, 0);
  glMat[2] = Tpw.at<float>(2, 0);
  glMat[3] = 0.0;

  glMat[4] = Tpw.at<float>(0, 1);
  glMat[5] = Tpw.at<float>(1, 1);
  glMat[6] = Tpw.at<float>(2, 1);
  glMat[7] = 0.0;

  glMat[8] = Tpw.at<float>(0, 2);
  glMat[9] = Tpw.at<float>(1, 2);
  glMat[10] = Tpw.at<float>(2, 2);
  glMat[11] = 0.0;

  glMat[12] = Tpw.at<float>(0, 3);
  glMat[13] = Tpw.at<float>(1, 3);
  glMat[14] = Tpw.at<float>(2, 3);
  glMat[15] = 1.0;

  jfloatArray javaFloatArray = env->NewFloatArray(16);
  env->SetFloatArrayRegion(javaFloatArray,0,16,glMat);
  return javaFloatArray;
}
