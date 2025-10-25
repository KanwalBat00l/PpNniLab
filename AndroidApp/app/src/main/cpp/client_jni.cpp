#include <jni.h>
#include <dlfcn.h>
#include <cstdlib>
#include <cstring>
#include <cstdio>
#include <sstream>
#include <iostream>
#include <streambuf>
#include <unistd.h>
#include <fcntl.h>
#include <android/log.h>

#define LOG_TAG "ClientJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

using client_main_fn = int(*)(int, char**);

static void* open_client_library() {
    void* handle = dlopen("libclient_android.so", RTLD_NOW | RTLD_LOCAL);
    if (!handle) {
        LOGE("dlopen failed: %s", dlerror());
    }
    return handle;
}

static client_main_fn resolve_client_main(void* handle) {
    if (!handle) return nullptr;
    const char* candidates[] = {"client_main", "main", nullptr};
    for (const char** p = candidates; *p != nullptr; ++p) {
        void* sym = dlsym(handle, *p);
        if (sym) {
            LOGI("Found symbol: %s", *p);
            return reinterpret_cast<client_main_fn>(sym);
        }
    }
    LOGE("No client entrypoint found");
    return nullptr;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_androidapp_NativeBridge_runClient(
        JNIEnv* env,
        jobject /* this */,
        jstring jIp,
        jint jPort,
        jstring jImagePath) {

    if (!jIp || !jImagePath) {
        return env->NewStringUTF("Error: null ip or imagePath");
    }

    const char* ip = env->GetStringUTFChars(jIp, nullptr);
    const char* img = env->GetStringUTFChars(jImagePath, nullptr);

    char portBuf[16];
    snprintf(portBuf, sizeof(portBuf), "%d", (int)jPort);
    char* argv[4];
    argv[0] = strdup("client_android");
    argv[1] = strdup(ip);
    argv[2] = strdup(portBuf);
    argv[3] = strdup(img);
    int argc = 4;

    env->ReleaseStringUTFChars(jIp, ip);
    env->ReleaseStringUTFChars(jImagePath, img);

    void* handle = open_client_library();
    if (!handle) {
        for (int i=0;i<argc;i++) free(argv[i]);
        return env->NewStringUTF("Error: failed to load client library");
    }

    client_main_fn fn = resolve_client_main(handle);
    if (!fn) {
        dlclose(handle);
        for (int i=0;i<argc;i++) free(argv[i]);
        return env->NewStringUTF("Error: client entrypoint not found");
    }

    // --- Redirect stdout and stderr at OS level ---
    int stdout_pipe[2], stderr_pipe[2];
    pipe(stdout_pipe);
    pipe(stderr_pipe);

    int old_stdout = dup(fileno(stdout));
    int old_stderr = dup(fileno(stderr));
    dup2(stdout_pipe[1], fileno(stdout));
    dup2(stderr_pipe[1], fileno(stderr));
    close(stdout_pipe[1]);
    close(stderr_pipe[1]);

    // --- Run client ---
    int rc = fn(argc, argv);

    // --- Restore stdout/stderr ---
    fflush(stdout);
    fflush(stderr);
    dup2(old_stdout, fileno(stdout));
    dup2(old_stderr, fileno(stderr));
    close(old_stdout);
    close(old_stderr);

    // --- Read captured output ---
    std::ostringstream output;
    char buffer[256];
    ssize_t n;
    while ((n = read(stdout_pipe[0], buffer, sizeof(buffer))) > 0) {
        output.write(buffer, n);
    }
    while ((n = read(stderr_pipe[0], buffer, sizeof(buffer))) > 0) {
        output.write(buffer, n);
    }
    close(stdout_pipe[0]);
    close(stderr_pipe[0]);

    for (int i=0;i<argc;i++) free(argv[i]);

    LOGI("Native client returned %d", rc);
    std::string result = "[Return code: " + std::to_string(rc) + "]\n" + output.str();
    return env->NewStringUTF(result.c_str());
}
