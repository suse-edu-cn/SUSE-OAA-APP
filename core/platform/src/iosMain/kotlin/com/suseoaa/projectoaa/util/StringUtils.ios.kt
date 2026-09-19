package com.suseoaa.projectoaa.util

import platform.Foundation.NSString
import platform.Foundation.precomposedStringWithCompatibilityMapping

actual fun String.normalizeFont(): String {
    return (this as NSString).precomposedStringWithCompatibilityMapping()
}
