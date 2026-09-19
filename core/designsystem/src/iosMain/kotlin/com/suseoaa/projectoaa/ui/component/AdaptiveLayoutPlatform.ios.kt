package com.suseoaa.projectoaa.ui.component

import platform.UIKit.UIDevice
import platform.UIKit.UIUserInterfaceIdiomPad

actual fun isTabletFormFactorDevice(): Boolean {
    return UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad
}

