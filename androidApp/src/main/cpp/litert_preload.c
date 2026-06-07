#include <android/log.h>
#include <dlfcn.h>
#include <jni.h>
#include <stdio.h>

static const char* k_log_tag = "AiLabLiteRtPreload";
static void* g_litert_handle = NULL;

static int dlopen_litert_global(const char* library_path) {
    if (g_litert_handle != NULL) {
        return 1;
    }

    dlerror();
    g_litert_handle = dlopen(library_path, RTLD_NOW | RTLD_GLOBAL);
    if (g_litert_handle != NULL) {
        __android_log_print(
            ANDROID_LOG_INFO,
            k_log_tag,
            "Preloaded %s with RTLD_GLOBAL.",
            library_path
        );
        return 1;
    }

    const char* error = dlerror();
    __android_log_print(
        ANDROID_LOG_WARN,
        k_log_tag,
        "Failed to preload %s: %s",
        library_path,
        error != NULL ? error : "unknown error"
    );
    return 0;
}

JNIEXPORT jboolean JNICALL
Java_com_suseoaa_projectoaa_util_LiteRtNativePreloader_preloadLiteRt(
    JNIEnv* env,
    jclass clazz,
    jstring native_library_dir
) {
    (void)clazz;

    if (native_library_dir == NULL) {
        __android_log_print(ANDROID_LOG_WARN, k_log_tag, "nativeLibraryDir is null.");
        return JNI_FALSE;
    }

    const char* native_library_dir_chars = (*env)->GetStringUTFChars(env, native_library_dir, NULL);
    if (native_library_dir_chars == NULL) {
        __android_log_print(ANDROID_LOG_WARN, k_log_tag, "Unable to read nativeLibraryDir.");
        return JNI_FALSE;
    }

    char litert_path[1024];
    int written = snprintf(
        litert_path,
        sizeof(litert_path),
        "%s/libLiteRt.so",
        native_library_dir_chars
    );
    (*env)->ReleaseStringUTFChars(env, native_library_dir, native_library_dir_chars);

    if (written > 0 && written < (int)sizeof(litert_path) && dlopen_litert_global(litert_path)) {
        return JNI_TRUE;
    }

    __android_log_print(ANDROID_LOG_INFO, k_log_tag, "Retrying LiteRT preload by soname.");
    return dlopen_litert_global("libLiteRt.so") ? JNI_TRUE : JNI_FALSE;
}
