#include <android/asset_manager_jni.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>

#include <android/log.h>

#include <jni.h>

#include <string>
#include <vector>

#include <platform.h>
#include <benchmark.h>

#include "ndkcamera.h"
#include "scrfd.h"

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <iostream>
#include <android/bitmap.h>
#include <opencv2/opencv.hpp>
#include <opencv2/highgui/highgui.hpp>

using namespace cv;

#if __ARM_NEON
#include <arm_neon.h>
#endif // __ARM_NEON

static SCRFD *g_scrfd = 0;
static ncnn::Mutex lock;
static std::vector<FaceObject> faceObjects;

class MyNdkCamera : public NdkCameraWindow {
public:
    virtual void on_image_render(cv::Mat &rgb) const;
};

void MyNdkCamera::on_image_render(cv::Mat &rgb) const {
    {
        ncnn::MutexLockGuard g(lock);
        if (g_scrfd) {
            g_scrfd->detect(rgb, faceObjects);
            g_scrfd->findPose(faceObjects);
            g_scrfd->draw(rgb, faceObjects);
        }
    }
}

static MyNdkCamera *g_camera = 0;

extern "C" {
JNIEXPORT jint
JNI_OnLoad(JavaVM *vm, void *reserved) {
    g_camera = new MyNdkCamera;

    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
    {
        ncnn::MutexLockGuard g(lock);
        delete g_scrfd;
        g_scrfd = 0;
    }

    delete g_camera;
    g_camera = 0;
}


extern "C" jboolean
Java_com_tondz_phathiengianlanthicu_CelebSDK_loadModel(JNIEnv *env, jobject thiz,
                                                       jobject assetManager) {
    AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);
    ncnn::MutexLockGuard g(lock);
    const char *modeltype = "n";

    if (!g_scrfd)
        g_scrfd = new SCRFD;
    g_scrfd->load(mgr, modeltype, false);

    return JNI_TRUE;
}
extern "C" jboolean
Java_com_tondz_phathiengianlanthicu_CelebSDK_openCamera(JNIEnv *env, jobject thiz, jint facing) {
    if (facing < 0 || facing > 1)
        return JNI_FALSE;
    g_camera->open((int) facing);

    return JNI_TRUE;
}

extern "C" jboolean
Java_com_tondz_phathiengianlanthicu_CelebSDK_closeCamera(JNIEnv *env, jobject thiz) {
    g_camera->close();

    return JNI_TRUE;
}

extern "C" jboolean
Java_com_tondz_phathiengianlanthicu_CelebSDK_setOutputWindow(JNIEnv *env, jobject thiz,
                                                             jobject surface) {
    ANativeWindow *win = ANativeWindow_fromSurface(env, surface);
    g_camera->set_window(win);
    return JNI_TRUE;
}

}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_tondz_phathiengianlanthicu_CelebSDK_getPose(JNIEnv *env, jobject thiz) {
    if (!faceObjects.empty()) {
        std::ostringstream oss;
        oss << faceObjects[0].yaw << "," << faceObjects[0].pitch << "," << faceObjects[0].roll
            << ",";
        std::string embeddingStr = oss.str();
        return env->NewStringUTF(embeddingStr.c_str());
    }
    return env->NewStringUTF("");

}