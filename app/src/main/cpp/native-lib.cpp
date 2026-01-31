#include <jni.h>
#include <string>
#include <vector>
#include "whisper.h"
#include <android/log.h>

#define TAG "JNI_Whisper"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

struct whisper_context *ctx = nullptr;

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_hevalvural_whisper_1task_WhisperLib_initContext(JNIEnv *env, jobject thiz, jstring model_path_str) {
    const char *model_path = env->GetStringUTFChars(model_path_str, nullptr);


    ctx = whisper_init_from_file(model_path);

    env->ReleaseStringUTFChars(model_path_str, model_path);

    if (ctx == nullptr) {
        LOGI("Error: Model cannot be uploaded! (whisper_init_from_file returned null)");
        return 0;
    }
    LOGI("Success: Model uploaded.");
    return (jlong) ctx;
}

JNIEXPORT void JNICALL
Java_com_hevalvural_whisper_1task_WhisperLib_freeContext(JNIEnv *env, jobject thiz, jlong context_ptr) {
    if (ctx != nullptr) {
        whisper_free(ctx);
        ctx = nullptr;
    }
}

JNIEXPORT jstring JNICALL
Java_com_hevalvural_whisper_1task_WhisperLib_fullTranscribe(JNIEnv *env, jobject thiz, jlong context_ptr, jfloatArray audio_data) {

    if (ctx == nullptr) return env->NewStringUTF("Model is missing!");

    jfloat *audio_raw = env->GetFloatArrayElements(audio_data, nullptr);
    jsize len = env->GetArrayLength(audio_data);

    std::vector<float> pcmf32(audio_raw, audio_raw + len);
    env->ReleaseFloatArrayElements(audio_data, audio_raw, 0);

    whisper_full_params wparams = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);

    wparams.print_progress = false;
    wparams.language = "en";
    wparams.n_threads = 4;

    if (whisper_full(ctx, wparams, pcmf32.data(), pcmf32.size()) != 0) {
        return env->NewStringUTF("Error while converting (whisper_full failed).");
    }

    std::string result = "";
    int n_segments = whisper_full_n_segments(ctx);
    for (int i = 0; i < n_segments; ++i) {
        const char *text = whisper_full_get_segment_text(ctx, i);
        result += text;
    }

    return env->NewStringUTF(result.c_str());
}

}