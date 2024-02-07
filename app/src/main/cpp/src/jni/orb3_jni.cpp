#include <jni.h>
#include <System.h>

extern "C"
JNIEXPORT jlong JNICALL
Java_com_mag_slam3dvideo_orb3_ORB3_initOrb(JNIEnv *env, jobject thiz, jstring vocab_file_name,
                                           jstring config_file_name) {

    jboolean isCopy;
    const char *vocabFileNameBytes = (env)->GetStringUTFChars(vocab_file_name, &isCopy);
    const char *configFileNameBytes = (env)->GetStringUTFChars(config_file_name, &isCopy);
    std::string vocabFile = vocabFileNameBytes;
    std::string configFile = configFileNameBytes;
    env->ReleaseStringUTFChars(vocab_file_name,vocabFileNameBytes);
    env->ReleaseStringUTFChars(config_file_name,configFileNameBytes);


    auto *SLAM = new ORB_SLAM3::System(vocabFile,configFile, ORB_SLAM3::System::MONOCULAR, false);
    return reinterpret_cast<jlong>(SLAM);
}