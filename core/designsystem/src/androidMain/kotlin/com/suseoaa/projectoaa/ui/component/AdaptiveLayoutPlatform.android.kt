package com.suseoaa.projectoaa.ui.component

import android.content.res.Resources

actual fun isTabletFormFactorDevice(): Boolean {
    return Resources.getSystem().configuration.smallestScreenWidthDp >= 600
}

