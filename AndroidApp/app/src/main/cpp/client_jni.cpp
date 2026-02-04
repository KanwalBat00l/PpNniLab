#include <jni.h>
#include <string>
#include <unistd.h>
#include <android/log.h>
#include <sstream>
#include <vector>

extern "C" int client_run(const char* protocol, const char* model_name,
                          const char* ip, int port, const char* input_path);

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_androidapp_NativeBridge_runMockClient(
        JNIEnv* env, jobject, jstring jProtocol, jstring jModel,
        jstring jIp, jint jPort, jstring jFilesDir) {

    const char* protocol = env->GetStringUTFChars(jProtocol, nullptr);
    const char* model    = env->GetStringUTFChars(jModel, nullptr);
    const char* ip       = env->GetStringUTFChars(jIp, nullptr);
    const char* filesDir = env->GetStringUTFChars(jFilesDir, nullptr);

    std::string inputPath = std::string(filesDir) + "/pretrained/" + std::string(model) + "_mock_input.inp";

    // --- REDIRECT STDOUT TO CAPTURE LOGS ---
    int pipefds[2];
    pipe(pipefds);
    int old_stdout = dup(STDOUT_FILENO);
    dup2(pipefds[1], STDOUT_FILENO);

    int rc = client_run(protocol, model, ip, jPort, inputPath.c_str());

    fflush(stdout);
    close(pipefds[1]);
    dup2(old_stdout, STDOUT_FILENO);

    std::stringstream ss;
    char buf[512];
    ssize_t n;
    while ((n = read(pipefds[0], buf, sizeof(buf)-1)) > 0) {
        buf[n] = 0;
        ss << buf;
    }
    close(pipefds[0]);

    env->ReleaseStringUTFChars(jProtocol, protocol);
    env->ReleaseStringUTFChars(jModel, model);
    env->ReleaseStringUTFChars(jIp, ip);
    env->ReleaseStringUTFChars(jFilesDir, filesDir);

    std::string finalOutput = ss.str() + "\n[System Status: " + std::to_string(rc) + "]";
    return env->NewStringUTF(finalOutput.c_str());
}