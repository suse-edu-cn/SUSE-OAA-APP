package com.suseoaa.projectoaa.util

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

actual class AppLifecycleObserver(private val context: Context) {

    private var observer: DefaultLifecycleObserver? = null

    actual fun startObserving(onForeground: () -> Unit, onBackground: () -> Unit) {
        stopObserving()
        observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                onForeground()
            }

            override fun onStop(owner: LifecycleOwner) {
                onBackground()
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer!!)
    }

    actual fun stopObserving() {
        observer?.let {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(it)
        }
        observer = null
    }
}
