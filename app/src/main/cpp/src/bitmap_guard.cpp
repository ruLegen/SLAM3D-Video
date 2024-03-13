#include <bitmap_guard.h>
#include <cassert>
#include <log.h>
namespace SLAMVideo{

BitmapGuard::BitmapGuard(JNIEnv* env, jobject jBitmap)
        : env{env}, bitmap{jBitmap}, bytes{nullptr} {
    valid = false;
    if (AndroidBitmap_getInfo(env, bitmap, &info) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("AndroidBitmap_getInfo failed");
        return;
    }
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888 &&
        info.format != ANDROID_BITMAP_FORMAT_A_8) {
        LOGE("AndroidBitmap in the wrong format");
        return;
    }
    bytesPerPixel = info.stride / info.width;
    if (bytesPerPixel != 1 && bytesPerPixel != 4) {
        LOGE("Expected a vector size of 1 or 4. Got %d. Extra padding per line not currently "
              "supported",
              bytesPerPixel);
        return;
    }
    if (AndroidBitmap_lockPixels(env, bitmap, &bytes) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("AndroidBitmap_lockPixels failed");
        return;
    }
    valid = true;
}

BitmapGuard::~BitmapGuard() {
    if (valid) {
        AndroidBitmap_unlockPixels(env, bitmap);
    }
}
uint8_t*BitmapGuard::get() const {
    assert(valid);
    return reinterpret_cast<uint8_t*>(bytes);
}
int BitmapGuard::width() const { return info.width; }
int BitmapGuard::height() const { return info.height; }
int BitmapGuard::vectorSize() const { return bytesPerPixel; }
int BitmapGuard::format() const {return info.format;}
}
