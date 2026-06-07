package com.suseoaa.projectoaa.util

import android.content.Context
import android.util.Log

object LiteRtNativePreloader {
    private const val TAG = "AiLabLiteRtPreload"

    @Volatile
    private var attempted = false

    fun preload(context: Context) {
        if (attempted) return
        attempted = true

        try {
            System.loadLibrary("suseoaa_litert_preload")
            val loaded = preloadLiteRt(context.applicationInfo.nativeLibraryDir)
            Log.d(TAG, "LiteRT global preload result: $loaded")
        } catch (exception: UnsatisfiedLinkError) {
            Log.w(TAG, "LiteRT global preload unavailable: ${exception.message}", exception)
        } catch (exception: Exception) {
            Log.w(TAG, "LiteRT global preload failed: ${exception.message}", exception)
        }
    }

    @JvmStatic
    private external fun preloadLiteRt(nativeLibraryDir: String): Boolean
}
