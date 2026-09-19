package com.suseoaa.projectoaa.util

import java.text.Normalizer

actual fun String.normalizeFont(): String {
    // 使用 NFKC 标准化，将全角字符或数学符号转化为对应的标准兼容字符
    return Normalizer.normalize(this, Normalizer.Form.NFKC)
}
