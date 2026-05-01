package com.suseoaa.projectoaa.util

import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.darwin.NSObjectProtocol

actual class AppLifecycleObserver {

    private var foregroundObserver: NSObjectProtocol? = null
    private var backgroundObserver: NSObjectProtocol? = null

    actual fun startObserving(onForeground: () -> Unit, onBackground: () -> Unit) {
        stopObserving()
        val center = NSNotificationCenter.defaultCenter

        foregroundObserver = center.addObserverForName(
            name = UIApplicationDidBecomeActiveNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) {
            onForeground()
        }

        backgroundObserver = center.addObserverForName(
            name = UIApplicationDidEnterBackgroundNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) {
            onBackground()
        }
    }

    actual fun stopObserving() {
        val center = NSNotificationCenter.defaultCenter
        foregroundObserver?.let { center.removeObserver(it) }
        backgroundObserver?.let { center.removeObserver(it) }
        foregroundObserver = null
        backgroundObserver = null
    }
}
