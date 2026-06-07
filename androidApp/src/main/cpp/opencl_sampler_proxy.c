#include <android/log.h>
#include <dlfcn.h>
#include <jni.h>
#include <pthread.h>
#include <string.h>

static const char* k_log_tag = "AiLabSamplerProxy";
static pthread_once_t g_load_once = PTHREAD_ONCE_INIT;
static void* g_real_sampler_handle = NULL;

typedef int (*CreateFunc)(
    void* env,
    int batch_size,
    int sequence_size,
    int vocab_size,
    const void* activation_data_type,
    const void* sampler_params,
    void** sampler_out,
    char** error_msg
);
typedef void (*DestroyFunc)(void* sampler);
typedef int (*SampleFunc)(
    void* sampler,
    void* logits_tensor,
    void* ids_tensor,
    const void* scores_tensor,
    char** error_msg
);
typedef int (*UpdateConfigFunc)(
    void* sampler,
    const void* sampler_params,
    int batch_size,
    void* rand_gen_shared_ptr,
    char** error_msg
);

static CreateFunc g_create_func = NULL;
static DestroyFunc g_destroy_func = NULL;
static SampleFunc g_sample_func = NULL;
static UpdateConfigFunc g_update_config_func = NULL;

static void set_error_message(char** error_msg, const char* message) {
    if (error_msg == NULL) {
        return;
    }

    *error_msg = strdup(message);
}

static void* lookup_symbol(const char* symbol_name) {
    void* symbol = dlsym(g_real_sampler_handle, symbol_name);
    if (symbol == NULL) {
        const char* error = dlerror();
        __android_log_print(
            ANDROID_LOG_WARN,
            k_log_tag,
            "Failed to lookup %s: %s",
            symbol_name,
            error != NULL ? error : "unknown error"
        );
    }
    return symbol;
}

static void load_real_sampler_once(void) {
    dlerror();
    void* litert_handle = dlopen("libLiteRt.so", RTLD_NOW | RTLD_GLOBAL);
    if (litert_handle == NULL) {
        const char* error = dlerror();
        __android_log_print(
            ANDROID_LOG_WARN,
            k_log_tag,
            "Failed to preload libLiteRt.so before real sampler: %s",
            error != NULL ? error : "unknown error"
        );
    } else {
        __android_log_print(
            ANDROID_LOG_INFO,
            k_log_tag,
            "Preloaded libLiteRt.so before real sampler."
        );
    }

    dlerror();
    g_real_sampler_handle = dlopen("libLiteRtTopKOpenClSamplerReal.so", RTLD_NOW | RTLD_LOCAL);
    if (g_real_sampler_handle == NULL) {
        const char* error = dlerror();
        __android_log_print(
            ANDROID_LOG_WARN,
            k_log_tag,
            "Failed to load real OpenCL sampler: %s",
            error != NULL ? error : "unknown error"
        );
        return;
    }

    g_create_func = (CreateFunc)lookup_symbol("LiteRtTopKOpenClSampler_Create");
    g_destroy_func = (DestroyFunc)lookup_symbol("LiteRtTopKOpenClSampler_Destroy");
    g_sample_func = (SampleFunc)lookup_symbol("LiteRtTopKOpenClSampler_SampleToIdAndScoreBuffer");
    g_update_config_func = (UpdateConfigFunc)lookup_symbol("LiteRtTopKOpenClSampler_UpdateConfig");

    if (g_create_func != NULL &&
        g_destroy_func != NULL &&
        g_sample_func != NULL &&
        g_update_config_func != NULL) {
        __android_log_print(
            ANDROID_LOG_INFO,
            k_log_tag,
            "Real OpenCL sampler loaded and linked."
        );
    }
}

static int ensure_real_sampler_loaded(char** error_msg) {
    pthread_once(&g_load_once, load_real_sampler_once);
    if (g_real_sampler_handle == NULL ||
        g_create_func == NULL ||
        g_destroy_func == NULL ||
        g_sample_func == NULL ||
        g_update_config_func == NULL) {
        set_error_message(error_msg, "OpenCL sampler proxy failed to load real sampler.");
        return 1;
    }

    return 0;
}

int LiteRtTopKOpenClSampler_Create(
    void* env,
    int batch_size,
    int sequence_size,
    int vocab_size,
    const void* activation_data_type,
    const void* sampler_params,
    void** sampler_out,
    char** error_msg
) {
    if (ensure_real_sampler_loaded(error_msg) != 0) {
        return 1;
    }

    return g_create_func(
        env,
        batch_size,
        sequence_size,
        vocab_size,
        activation_data_type,
        sampler_params,
        sampler_out,
        error_msg
    );
}

void LiteRtTopKOpenClSampler_Destroy(void* sampler) {
    if (ensure_real_sampler_loaded(NULL) != 0) {
        return;
    }

    g_destroy_func(sampler);
}

int LiteRtTopKOpenClSampler_SampleToIdAndScoreBuffer(
    void* sampler,
    void* logits_tensor,
    void* ids_tensor,
    const void* scores_tensor,
    char** error_msg
) {
    if (ensure_real_sampler_loaded(error_msg) != 0) {
        return 1;
    }

    return g_sample_func(sampler, logits_tensor, ids_tensor, scores_tensor, error_msg);
}

int LiteRtTopKOpenClSampler_UpdateConfig(
    void* sampler,
    const void* sampler_params,
    int batch_size,
    void* rand_gen_shared_ptr,
    char** error_msg
) {
    if (ensure_real_sampler_loaded(error_msg) != 0) {
        return 1;
    }

    return g_update_config_func(
        sampler,
        sampler_params,
        batch_size,
        rand_gen_shared_ptr,
        error_msg
    );
}
