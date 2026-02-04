#include <jni.h>
#include <string>
#include <android/log.h>

// Link to our verified C++ logic
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

    // Construct the workspace path where the Preprocessor saves the .inp
    // Result: /data/user/0/com.example.androidapp/files/pretrained/resnet50_mock_input.inp
    std::string inputPath = std::string(filesDir) + "/pretrained/" +
                            std::string(model) + "_mock_input.inp";

    // Call the verified C++ core directly
    int rc = client_run(protocol, model, ip, jPort, inputPath.c_str());

    // Clean up JNI strings
    env->ReleaseStringUTFChars(jProtocol, protocol);
    env->ReleaseStringUTFChars(jModel, model);
    env->ReleaseStringUTFChars(jIp, ip);
    env->ReleaseStringUTFChars(jFilesDir, filesDir);

    std::string result = "Native Client Exit Code: " + std::to_string(rc);
    if (rc != 0) result += "\n(Check Server Connection or Input Path)";

    return env->NewStringUTF(result.c_str());
}