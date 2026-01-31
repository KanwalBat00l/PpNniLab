#include <jni.h>       // ✅ Required for JNIEXPORT, JNICALL, and JNIEnv
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

// -------------------
// Type alias for client main function
using client_main_fn = int(*)(int, char**);

// -------------------
// Dynamically load client library
static void* open_client_library() {
    void* handle = dlopen("libmock_client.so", RTLD_NOW | RTLD_LOCAL);
    if (!handle) {
        LOGE("dlopen failed: %s", dlerror());
    }
    return handle;
}

// -------------------
// Resolve client entrypoint
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

// -------------------
// JNI function
extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_androidapp_NativeBridge_runMockClient(
        JNIEnv* env,
        jobject /* this */,
        jstring jProtocol,
        jstring jModel,
        jstring jIp,
        jint jPort) {

    if (!jProtocol || !jModel || !jIp) {
        return env->NewStringUTF("Error: null argument(s)");
    }

    // --- Set CWD to filesDir (where we copied pretrained/) ---
    chdir("/data/user/0/com.example.androidapp");


    const char* protocol = env->GetStringUTFChars(jProtocol, nullptr);
    const char* model    = env->GetStringUTFChars(jModel, nullptr);
    const char* ip       = env->GetStringUTFChars(jIp, nullptr);

    char portBuf[16];
    snprintf(portBuf, sizeof(portBuf), "%d", jPort);

    char* argv[5];
    argv[0] = strdup("mock_client");
    argv[1] = strdup(protocol);
    argv[2] = strdup(model);
    argv[3] = strdup(ip);
    argv[4] = strdup(portBuf);
    int argc = 5;

    env->ReleaseStringUTFChars(jProtocol, protocol);
    env->ReleaseStringUTFChars(jModel, model);
    env->ReleaseStringUTFChars(jIp, ip);

    void* handle = open_client_library();
    if (!handle) {
        for (int i = 0; i < argc; ++i) free(argv[i]);
        return env->NewStringUTF("Error: failed to load client library");
    }

    client_main_fn fn = resolve_client_main(handle);
    if (!fn) {
        dlclose(handle);
        for (int i = 0; i < argc; ++i) free(argv[i]);
        return env->NewStringUTF("Error: client entrypoint not found");
    }

    // Redirect stdout/stderr
    int stdout_pipe[2], stderr_pipe[2];
    pipe(stdout_pipe);
    pipe(stderr_pipe);

    int old_stdout = dup(fileno(stdout));
    int old_stderr = dup(fileno(stderr));
    dup2(stdout_pipe[1], fileno(stdout));
    dup2(stderr_pipe[1], fileno(stderr));
    close(stdout_pipe[1]);
    close(stderr_pipe[1]);

    // Call client
    int rc = fn(argc, argv);

    // Restore stdout/stderr
    fflush(stdout);
    fflush(stderr);
    dup2(old_stdout, fileno(stdout));
    dup2(old_stderr, fileno(stderr));
    close(old_stdout);
    close(old_stderr);

    // Capture output
    std::ostringstream output;
    char buffer[256];
    ssize_t n;
    while ((n = read(stdout_pipe[0], buffer, sizeof(buffer))) > 0) output.write(buffer, n);
    while ((n = read(stderr_pipe[0], buffer, sizeof(buffer))) > 0) output.write(buffer, n);
    close(stdout_pipe[0]);
    close(stderr_pipe[0]);

    for (int i = 0; i < argc; ++i) free(argv[i]);

    LOGI("Native client returned %d", rc);
    std::string result = "[Return code: " + std::to_string(rc) + "]\n" + output.str();
    return env->NewStringUTF(result.c_str());
}
