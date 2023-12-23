#include <jni.h>
#include <string>
#include <android/log.h>

extern "C"
JNIEXPORT void JNICALL
Java_org_portablecl_pocl_1rreferenceandroidjavaclient_NativeUtils_setNativeEnv(JNIEnv *env,
                                                                               jclass clazz,
                                                                               jstring key,
                                                                               jstring value) {
    char *c_key = (char *) env->GetStringUTFChars(key, 0);
    char *c_value = (char *) env->GetStringUTFChars(value, 0);
    __android_log_print(ANDROID_LOG_INFO, "Native utils", "setting env variable: %s : %s", c_key,
                        c_value);

    setenv(c_key, c_value, 1);
}