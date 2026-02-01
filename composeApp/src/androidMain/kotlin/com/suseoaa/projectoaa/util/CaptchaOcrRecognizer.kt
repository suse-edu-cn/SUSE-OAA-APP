package com.suseoaa.projectoaa.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * OCR验证码识别器
 * 
 * 识别策略（按优先级）：
 * 1. ddddocr (ONNX模型) - 专门针对验证码训练，识别率最高
 * 2. ML Kit + 多种预处理策略 - 通用OCR，作为后备方案
 * 
 * 如需使用 ddddocr，请：
 * 1. 下载模型文件: https://github.com/sml2h3/ddddocr/raw/master/ddddocr/common_old.onnx
 * 2. 将 common_old.onnx 放到 composeApp/src/androidMain/assets/ 目录
 */
object CaptchaOcrRecognizer {
    
    private var context: Context? = null
    private var ddddocrInitialized = false
    private var ddddocrAvailable = false
    
    // 使用拉丁文识别器（验证码通常是数字和英文）
    private val latinRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    
    // 备用：中文识别器
    private val chineseRecognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }
    
    /**
     * 初始化识别器
     * 建议在 Application 或 Activity 启动时调用
     */
    suspend fun initialize(ctx: Context) {
        context = ctx.applicationContext
        
        // 检查并初始化 ddddocr
        if (DdddOcrRecognizer.hasModel(ctx)) {
            ddddocrAvailable = DdddOcrRecognizer.initialize(ctx)
            ddddocrInitialized = true
            if (ddddocrAvailable) {
                println("[OCR] ddddocr 模型已加载，将优先使用")
            }
        } else {
            println("[OCR] ddddocr 模型不存在，将使用 ML Kit")
            println("[OCR] 如需使用 ddddocr，请下载模型到 assets/common_old.onnx")
        }
    }
    
    /**
     * 识别验证码图片中的文字
     * 优先使用 ddddocr，失败后使用 ML Kit
     * 
     * @param imageBytes 验证码图片的字节数组
     * @return 识别出的文字
     */
    suspend fun recognizeCaptcha(imageBytes: ByteArray): Result<String> {
        // 1. 首先尝试 ddddocr（如果可用）
        if (ddddocrAvailable) {
            val ddddResult = DdddOcrRecognizer.recognizeCaptcha(imageBytes)
            if (ddddResult.isSuccess) {
                val text = ddddResult.getOrNull() ?: ""
                if (text.isNotBlank()) {
                    println("[OCR] ddddocr 识别成功: $text")
                    return Result.success(text)
                }
            }
            println("[OCR] ddddocr 识别失败，尝试 ML Kit")
        }
        
        // 2. 使用 ML Kit + 多种预处理策略
        return recognizeWithMLKit(imageBytes)
    }
    
    /**
     * 使用 ML Kit 识别验证码
     */
    private suspend fun recognizeWithMLKit(imageBytes: ByteArray): Result<String> {
        return try {
            // 解码图片
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                ?: return Result.failure(Exception("无法解码验证码图片"))
            
            println("[OCR] 原始图片尺寸: ${bitmap.width}x${bitmap.height}")
            
            // 尝试多种预处理策略
            val strategies = listOf(
                "adaptive" to { preprocessAdaptive(bitmap) },
                "color_extract" to { preprocessColorExtract(bitmap) },
                "enhanced" to { preprocessEnhanced(bitmap) },
                "simple" to { preprocessSimple(bitmap) }
            )
            
            var bestResult = ""
            var bestConfidence = 0
            
            for ((name, processor) in strategies) {
                try {
                    val processedBitmap = processor()
                    val inputImage = InputImage.fromBitmap(processedBitmap, 0)
                    val recognizedText = recognizeWithLatinRecognizer(inputImage)
                    val cleanedText = cleanCaptchaText(recognizedText)
                    
                    println("[OCR] 策略[$name] 原始: '$recognizedText' -> 清理后: '$cleanedText'")
                    
                    // 选择最佳结果（优先选择4-6位数字的结果）
                    val confidence = calculateConfidence(cleanedText)
                    if (confidence > bestConfidence) {
                        bestConfidence = confidence
                        bestResult = cleanedText
                    }
                    
                    // 如果找到高置信度结果，直接返回
                    if (confidence >= 100) {
                        println("[OCR] 高置信度结果: $cleanedText")
                        return Result.success(cleanedText)
                    }
                } catch (e: Exception) {
                    println("[OCR] 策略[$name] 失败: ${e.message}")
                }
            }
            
            if (bestResult.isBlank()) {
                Result.failure(Exception("未能识别出验证码"))
            } else {
                println("[OCR] 最终结果: $bestResult (置信度: $bestConfidence)")
                Result.success(bestResult)
            }
        } catch (e: Exception) {
            println("[OCR] 识别异常: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * 计算识别结果的置信度
     * 验证码通常是4-6位数字
     */
    private fun calculateConfidence(text: String): Int {
        if (text.isBlank()) return 0
        val length = text.length
        return when {
            length == 4 -> 100  // 4位数字最常见
            length == 5 -> 90
            length == 6 -> 85
            length == 3 -> 50
            length > 6 -> 30
            else -> 20
        }
    }
    
    /**
     * 使用拉丁文识别器识别
     */
    private suspend fun recognizeWithLatinRecognizer(image: InputImage): String {
        return suspendCancellableCoroutine { continuation ->
            latinRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    continuation.resume(visionText.text)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }
    }
    
    /**
     * 策略1: 自适应阈值二值化 (Otsu算法)
     * 适合背景和前景对比明显的验证码
     */
    private fun preprocessAdaptive(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // 转灰度
        val grayPixels = IntArray(width * height)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            grayPixels[i] = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        }
        
        // 使用Otsu算法计算最佳阈值
        val threshold = calculateOtsuThreshold(grayPixels)
        println("[OCR] Otsu阈值: $threshold")
        
        // 二值化
        val resultPixels = IntArray(width * height)
        for (i in grayPixels.indices) {
            val binaryValue = if (grayPixels[i] < threshold) 0 else 255
            resultPixels[i] = (0xFF shl 24) or (binaryValue shl 16) or (binaryValue shl 8) or binaryValue
        }
        
        // 应用形态学处理去噪
        val denoisedPixels = applyMorphology(resultPixels, width, height)
        
        val processedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        processedBitmap.setPixels(denoisedPixels, 0, width, 0, 0, width, height)
        return processedBitmap
    }
    
    /**
     * 策略2: 颜色提取
     * 提取特定颜色范围的文字（适合彩色干扰的验证码）
     */
    private fun preprocessColorExtract(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // 统计颜色分布，找出主要的文字颜色
        val colorCounts = mutableMapOf<Int, Int>()
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            // 量化颜色以减少种类
            val quantizedColor = ((r / 32) shl 10) or ((g / 32) shl 5) or (b / 32)
            colorCounts[quantizedColor] = (colorCounts[quantizedColor] ?: 0) + 1
        }
        
        // 找出最常见的几种深色（可能是文字）
        val darkColors = colorCounts.entries
            .filter { 
                val r = ((it.key shr 10) and 0x1F) * 32
                val g = ((it.key shr 5) and 0x1F) * 32
                val b = (it.key and 0x1F) * 32
                // 排除太亮的颜色（背景）和太少出现的颜色（噪点）
                val brightness = (r + g + b) / 3
                brightness < 180 && it.value > (width * height * 0.005)
            }
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }
        
        val resultPixels = IntArray(width * height)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val quantizedColor = ((r / 32) shl 10) or ((g / 32) shl 5) or (b / 32)
            
            // 如果是文字颜色范围内，设为黑色；否则设为白色
            val isTextColor = darkColors.any { targetColor ->
                val tr = ((targetColor shr 10) and 0x1F) * 32
                val tg = ((targetColor shr 5) and 0x1F) * 32
                val tb = (targetColor and 0x1F) * 32
                abs(r - tr) < 50 && abs(g - tg) < 50 && abs(b - tb) < 50
            }
            
            val binaryValue = if (isTextColor) 0 else 255
            resultPixels[i] = (0xFF shl 24) or (binaryValue shl 16) or (binaryValue shl 8) or binaryValue
        }
        
        // 去噪
        val denoisedPixels = removeNoise(resultPixels, width, height)
        
        val processedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        processedBitmap.setPixels(denoisedPixels, 0, width, 0, 0, width, height)
        return processedBitmap
    }
    
    /**
     * 策略3: 增强处理
     * 对比度增强 + 锐化 + 自适应二值化
     */
    private fun preprocessEnhanced(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // 转灰度
        val grayPixels = IntArray(width * height)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            grayPixels[i] = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        }
        
        // 对比度增强 (直方图拉伸)
        var minGray = 255
        var maxGray = 0
        for (gray in grayPixels) {
            minGray = min(minGray, gray)
            maxGray = max(maxGray, gray)
        }
        
        val range = maxGray - minGray
        if (range > 0) {
            for (i in grayPixels.indices) {
                grayPixels[i] = ((grayPixels[i] - minGray) * 255 / range).coerceIn(0, 255)
            }
        }
        
        // 局部自适应二值化
        val blockSize = 15
        val resultPixels = IntArray(width * height)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = y * width + x
                
                // 计算局部平均值
                var sum = 0
                var count = 0
                val halfBlock = blockSize / 2
                
                for (dy in -halfBlock..halfBlock) {
                    for (dx in -halfBlock..halfBlock) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until width && ny in 0 until height) {
                            sum += grayPixels[ny * width + nx]
                            count++
                        }
                    }
                }
                
                val localMean = sum / count
                // 使用局部阈值（略低于平均值，更容易识别深色文字）
                val threshold = (localMean * 0.85).toInt()
                val binaryValue = if (grayPixels[i] < threshold) 0 else 255
                resultPixels[i] = (0xFF shl 24) or (binaryValue shl 16) or (binaryValue shl 8) or binaryValue
            }
        }
        
        // 去除细线干扰
        val cleanedPixels = removeInterferenceLines(resultPixels, width, height)
        
        val processedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        processedBitmap.setPixels(cleanedPixels, 0, width, 0, 0, width, height)
        return processedBitmap
    }
    
    /**
     * 策略4: 简单处理（原始方法）
     * 作为后备策略
     */
    private fun preprocessSimple(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val processedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            
            val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            val binaryValue = if (gray < 128) 0 else 255
            
            pixels[i] = (0xFF shl 24) or (binaryValue shl 16) or (binaryValue shl 8) or binaryValue
        }
        
        processedBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return processedBitmap
    }
    
    /**
     * Otsu算法计算最佳二值化阈值
     */
    private fun calculateOtsuThreshold(grayPixels: IntArray): Int {
        // 计算直方图
        val histogram = IntArray(256)
        for (gray in grayPixels) {
            histogram[gray.coerceIn(0, 255)]++
        }
        
        val total = grayPixels.size
        var sum = 0.0
        for (i in 0 until 256) {
            sum += i * histogram[i]
        }
        
        var sumB = 0.0
        var wB = 0
        var wF: Int
        var maxVariance = 0.0
        var threshold = 0
        
        for (t in 0 until 256) {
            wB += histogram[t]
            if (wB == 0) continue
            
            wF = total - wB
            if (wF == 0) break
            
            sumB += t * histogram[t]
            
            val mB = sumB / wB
            val mF = (sum - sumB) / wF
            
            val variance = wB.toDouble() * wF * (mB - mF) * (mB - mF)
            
            if (variance > maxVariance) {
                maxVariance = variance
                threshold = t
            }
        }
        
        return threshold
    }
    
    /**
     * 形态学处理：开运算去噪
     */
    private fun applyMorphology(pixels: IntArray, width: Int, height: Int): IntArray {
        // 先腐蚀再膨胀（开运算）
        val eroded = erode(pixels, width, height)
        return dilate(eroded, width, height)
    }
    
    /**
     * 腐蚀操作
     */
    private fun erode(pixels: IntArray, width: Int, height: Int): IntArray {
        val result = IntArray(width * height)
        val white = (0xFF shl 24) or (0xFF shl 16) or (0xFF shl 8) or 0xFF
        val black = 0xFF shl 24
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = y * width + x
                // 3x3窗口，如果有任何白色像素，则结果为白色
                var hasWhite = false
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until width && ny in 0 until height) {
                            if ((pixels[ny * width + nx] and 0xFF) == 0xFF) {
                                hasWhite = true
                                break
                            }
                        }
                    }
                    if (hasWhite) break
                }
                result[i] = if (hasWhite) white else black
            }
        }
        return result
    }
    
    /**
     * 膨胀操作
     */
    private fun dilate(pixels: IntArray, width: Int, height: Int): IntArray {
        val result = IntArray(width * height)
        val white = (0xFF shl 24) or (0xFF shl 16) or (0xFF shl 8) or 0xFF
        val black = 0xFF shl 24
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = y * width + x
                // 3x3窗口，如果有任何黑色像素，则结果为黑色
                var hasBlack = false
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until width && ny in 0 until height) {
                            if ((pixels[ny * width + nx] and 0xFF) == 0) {
                                hasBlack = true
                                break
                            }
                        }
                    }
                    if (hasBlack) break
                }
                result[i] = if (hasBlack) black else white
            }
        }
        return result
    }
    
    /**
     * 去除孤立噪点（中值滤波）
     */
    private fun removeNoise(pixels: IntArray, width: Int, height: Int): IntArray {
        val result = pixels.copyOf()
        val white = (0xFF shl 24) or (0xFF shl 16) or (0xFF shl 8) or 0xFF
        val black = 0xFF shl 24
        
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val i = y * width + x
                
                // 统计3x3窗口内的黑色像素数量
                var blackCount = 0
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        if ((pixels[(y + dy) * width + (x + dx)] and 0xFF) == 0) {
                            blackCount++
                        }
                    }
                }
                
                // 如果黑色像素太少（孤立点），变为白色
                // 如果黑色像素太多但中心是白色（孤立白点），变为黑色
                if ((pixels[i] and 0xFF) == 0 && blackCount <= 2) {
                    result[i] = white
                } else if ((pixels[i] and 0xFF) == 0xFF && blackCount >= 7) {
                    result[i] = black
                }
            }
        }
        return result
    }
    
    /**
     * 去除干扰线
     * 干扰线通常很细（1-2像素宽），文字笔画更粗
     */
    private fun removeInterferenceLines(pixels: IntArray, width: Int, height: Int): IntArray {
        val result = pixels.copyOf()
        val white = (0xFF shl 24) or (0xFF shl 16) or (0xFF shl 8) or 0xFF
        
        // 水平扫描，去除细水平线
        for (y in 1 until height - 1) {
            for (x in 0 until width) {
                val i = y * width + x
                if ((pixels[i] and 0xFF) == 0) {
                    // 检查垂直方向的连通性
                    val hasTopNeighbor = y > 0 && (pixels[(y - 1) * width + x] and 0xFF) == 0
                    val hasBottomNeighbor = y < height - 1 && (pixels[(y + 1) * width + x] and 0xFF) == 0
                    
                    // 如果上下都没有黑色邻居，可能是水平干扰线
                    if (!hasTopNeighbor && !hasBottomNeighbor) {
                        // 检查是否是连续的水平线段
                        var lineLength = 1
                        var tx = x + 1
                        while (tx < width && (pixels[y * width + tx] and 0xFF) == 0) {
                            val tHasTop = y > 0 && (pixels[(y - 1) * width + tx] and 0xFF) == 0
                            val tHasBottom = y < height - 1 && (pixels[(y + 1) * width + tx] and 0xFF) == 0
                            if (tHasTop || tHasBottom) break
                            lineLength++
                            tx++
                        }
                        
                        // 如果线段较长且没有垂直连接，则删除
                        if (lineLength > 5) {
                            for (lx in x until x + lineLength) {
                                result[y * width + lx] = white
                            }
                        }
                    }
                }
            }
        }
        
        // 垂直扫描，去除细垂直线
        for (x in 1 until width - 1) {
            for (y in 0 until height) {
                val i = y * width + x
                if ((result[i] and 0xFF) == 0) {
                    val hasLeftNeighbor = x > 0 && (result[y * width + (x - 1)] and 0xFF) == 0
                    val hasRightNeighbor = x < width - 1 && (result[y * width + (x + 1)] and 0xFF) == 0
                    
                    if (!hasLeftNeighbor && !hasRightNeighbor) {
                        var lineLength = 1
                        var ty = y + 1
                        while (ty < height && (result[ty * width + x] and 0xFF) == 0) {
                            val tHasLeft = x > 0 && (result[ty * width + (x - 1)] and 0xFF) == 0
                            val tHasRight = x < width - 1 && (result[ty * width + (x + 1)] and 0xFF) == 0
                            if (tHasLeft || tHasRight) break
                            lineLength++
                            ty++
                        }
                        
                        if (lineLength > 5) {
                            for (ly in y until y + lineLength) {
                                result[ly * width + x] = white
                            }
                        }
                    }
                }
            }
        }
        
        return result
    }
    
    /**
     * 清理识别结果
     * 保留数字和字母（验证码可能是纯数字或字母数字混合）
     */
    private fun cleanCaptchaText(text: String): String {
        // 移除空格和换行
        val cleaned = text.replace(Regex("[\\s\\n\\r]"), "")
        
        // 保留数字和字母
        val alphanumeric = cleaned.filter { it.isLetterOrDigit() }
        
        // 处理常见的OCR误识别（相似字符）
        val corrected = StringBuilder()
        for (c in alphanumeric) {
            val fixed = when (c) {
                'O', 'o' -> '0'  // O -> 0
                'l', 'I', '|' -> '1'  // l, I -> 1
                'Z' -> '2'  // Z -> 2 (在纯数字验证码中)
                'S', '$' -> '5'  // S -> 5
                'B' -> '8'  // B -> 8
                'G' -> '6'  // G -> 6
                'q' -> '9'  // q -> 9
                'D' -> '0'  // D -> 0
                else -> c
            }
            corrected.append(fixed)
        }
        
        return corrected.toString().uppercase()
    }
    
    /**
     * 关闭识别器释放资源
     */
    fun close() {
        latinRecognizer.close()
        chineseRecognizer.close()
        DdddOcrRecognizer.close()
        context = null
        ddddocrInitialized = false
        ddddocrAvailable = false
    }
}
