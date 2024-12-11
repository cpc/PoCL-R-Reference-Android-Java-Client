#include <jni.h>
#include <string>
#include <android/log.h>

#ifndef ENABLE_REMOTE_DISCOVERY_ANDROID
#define ENABLE_REMOTE_DISCOVERY_ANDROID
#endif
#include "pocl_remote.h"

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

    env->ReleaseStringUTFChars(key, c_key);
    env->ReleaseStringUTFChars(value, c_value);
}
extern "C"
JNIEXPORT void JNICALL
Java_org_portablecl_pocl_1rreferenceandroidjavaclient_NativeUtils_remoteAddServer(JNIEnv *env,
                                                                                  jclass clazz,
                                                                                  jstring id,
                                                                                  jstring domain,
                                                                                  jstring ip_port,
                                                                                  jstring type,
                                                                                  jint device_count) {

    char *c_id = (char *) env->GetStringUTFChars(id, 0);
    char *c_domain = (char *) env->GetStringUTFChars(domain, 0);
    char *c_ip_port = (char *) env->GetStringUTFChars(ip_port, 0);
    char *c_type = (char *) env->GetStringUTFChars(type, 0);

    pocl_remote_discovery_add_server(c_id, c_domain, c_ip_port, c_type, (cl_uint)device_count);

    env->ReleaseStringUTFChars(id, c_id);
    env->ReleaseStringUTFChars(domain, c_domain);
    env->ReleaseStringUTFChars(ip_port, c_ip_port);
    env->ReleaseStringUTFChars(type, c_type);
}