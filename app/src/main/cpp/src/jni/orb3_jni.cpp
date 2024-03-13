#include <jni.h>
#include <System.h>
#include <orb_slam_processor.h>
#include <bitmap_guard.h>

extern "C"
JNIEXPORT jlong JNICALL
Java_com_mag_slam3dvideo_orb3_OrbSlamProcessor_initOrb(JNIEnv *env, jobject thiz, jstring vocab_file_name,
                                           jstring config_file_name) {

    jboolean isCopy;
    const char *vocabFileNameBytes = (env)->GetStringUTFChars(vocab_file_name, &isCopy);
    const char *configFileNameBytes = (env)->GetStringUTFChars(config_file_name, &isCopy);
    std::string vocabFile = vocabFileNameBytes;
    std::string configFile = configFileNameBytes;

    auto *SLAM = new SLAMVideo::OrbSlamProcessor(vocabFile,configFile);

    env->ReleaseStringUTFChars(vocab_file_name,vocabFileNameBytes);
    env->ReleaseStringUTFChars(config_file_name,configFileNameBytes);

    return reinterpret_cast<jlong>(SLAM);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_mag_slam3dvideo_orb3_OrbSlamProcessor_processBitmap(JNIEnv *env, jobject thiz, jlong ptr,jobject bitmap) {

    auto *processor = reinterpret_cast<SLAMVideo::OrbSlamProcessor*>(ptr);
    if (processor == nullptr)
      return -1;
    auto inputGuard = SLAMVideo::BitmapGuard(env, bitmap);

    int trackingState = processor->processFrame(inputGuard);
    return trackingState;
}













//extern "C"
//JNIEXPORT void JNICALL
//Java_com_mag_slam3dvideo_orb3_OrbSlamProcessor_resaveVocabularyAsBinaryNative(JNIEnv *env, jobject thiz,
//                                                                  jstring text_vocab_input_path,
//                                                                  jstring binary_vocab_output_path) {
//    jboolean isCopy;
//    const char *vocabInputFileNameBytes = (env)->GetStringUTFChars(text_vocab_input_path, &isCopy);
//    const char *vocabOutputFileNameBytes = (env)->GetStringUTFChars(binary_vocab_output_path, &isCopy);
//
//    std::string vocabInFile = vocabInputFileNameBytes;
//    std::string vocabOutFile = vocabOutputFileNameBytes;
//
//    auto vocab = ORB_SLAM3::ORBVocabulary();
//    vocab.loadFromTextFile(vocabInFile);
//    vocab.saveToBinaryFile(vocabOutFile);
//
//
//
//    env->ReleaseStringUTFChars(text_vocab_input_path,vocabInputFileNameBytes);
//    env->ReleaseStringUTFChars(binary_vocab_output_path,vocabOutputFileNameBytes);
//
//
//}
