package com.suseoaa.projectoaa.util

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer
import kotlin.math.max

/**
 * ddddocr 的 Android 移植版本
 * 
 * ddddocr 是一个专门用于验证码识别的开源库，基于 CRNN (CNN + RNN) 架构
 * 这个类将 ddddocr 的 ONNX 模型移植到 Android 平台
 * 
 * 使用方法：
 * 1. 从 ddddocr 仓库下载 common_old.onnx 模型文件和 charsets.json 文件
 * 2. 将模型文件放到 assets 目录
 * 3. 调用 recognizeCaptcha() 方法
 * 
 * 模型下载地址: https://github.com/sml2h3/ddddocr/tree/master/ddddocr
 * 需要的文件: common_old.onnx (约 10MB)
 */
object DdddOcrRecognizer {
    
    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var isInitialized = false
    
    // ddddocr CHARSET_OLD - 从 Python 源码提取的完整字符集
    // 第一个字符是空白符(blank)用于CTC解码
    private var charset: List<String> = emptyList()
    
    // 模型输入的目标高度
    private const val TARGET_HEIGHT = 64
    
    /**
     * 初始化 ONNX Runtime 和加载模型
     * @param context Android Context
     * @param modelFileName 模型文件名 (在 assets 目录下)
     */
    suspend fun initialize(context: Context, modelFileName: String = "common_old.onnx"): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (isInitialized) return@withContext true
                
                // 加载字符集
                loadCharset(context)
                
                // 创建 ONNX Runtime 环境
                ortEnv = OrtEnvironment.getEnvironment()
                
                // 从 assets 加载模型
                val modelBytes = context.assets.open(modelFileName).use { it.readBytes() }
                
                // 创建推理会话
                val sessionOptions = OrtSession.SessionOptions()
                sessionOptions.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                ortSession = ortEnv?.createSession(modelBytes, sessionOptions)
                
                isInitialized = true
                println("[DdddOcr] 模型加载成功: $modelFileName")
                true
            } catch (e: Exception) {
                println("[DdddOcr] 模型加载失败: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }
    
    /**
     * 检查是否有可用的模型文件
     */
    fun hasModel(context: Context, modelFileName: String = "common_old.onnx"): Boolean {
        return try {
            context.assets.open(modelFileName).close()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 识别验证码
     * @param imageBytes 验证码图片的字节数组
     * @return 识别结果
     */
    suspend fun recognizeCaptcha(imageBytes: ByteArray): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (!isInitialized || ortSession == null) {
                    return@withContext Result.failure(Exception("模型未初始化，请先调用 initialize()"))
                }
                
                // 1. 解码图片
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    ?: return@withContext Result.failure(Exception("无法解码图片"))
                
                println("[DdddOcr] 原始图片尺寸: ${bitmap.width}x${bitmap.height}")
                
                // 2. 预处理图片 (按照 ddddocr 的方式)
                val inputTensor = preprocessImage(bitmap)
                
                // 3. 运行推理
                val inputName = ortSession!!.inputNames.first()
                val output = ortSession!!.run(mapOf(inputName to inputTensor))
                
                // 4. 解码输出 (CTC 解码)
                val outputTensor = output.get(0) as OnnxTensor
                val result = ctcDecode(outputTensor)
                
                // 清理资源
                inputTensor.close()
                output.close()
                
                println("[DdddOcr] 识别结果: $result")
                
                if (result.isBlank()) {
                    Result.failure(Exception("未能识别出验证码"))
                } else {
                    Result.success(result)
                }
            } catch (e: Exception) {
                println("[DdddOcr] 识别异常: ${e.message}")
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    /**
     * 预处理图片
     * 按照 ddddocr 的方式:
     * 1. 转灰度
     * 2. 调整高度到 64，宽度按比例缩放
     * 3. 归一化到 [-1, 1]: (x/255 - 0.5) / 0.5
     */
    private fun preprocessImage(bitmap: Bitmap): OnnxTensor {
        // 计算目标宽度 (保持宽高比)
        val scale = TARGET_HEIGHT.toFloat() / bitmap.height
        val targetWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        
        // 缩放图片
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, TARGET_HEIGHT, true)
        
        println("[DdddOcr] 缩放后尺寸: ${scaledBitmap.width}x${scaledBitmap.height}")
        
        // 转换为灰度并归一化
        val width = scaledBitmap.width
        val height = scaledBitmap.height
        
        // ddddocr 模型输入形状: [1, 1, 64, width] (batch, channel, height, width)
        val floatBuffer = FloatBuffer.allocate(1 * 1 * height * width)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = scaledBitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                
                // 转灰度: 使用标准灰度转换公式
                val grayValue = 0.299f * r + 0.587f * g + 0.114f * b
                
                // 归一化到 [-1, 1]: (x/255 - 0.5) / 0.5 = x/127.5 - 1
                val normalized = (grayValue / 255.0f - 0.5f) / 0.5f
                floatBuffer.put(normalized)
            }
        }
        
        floatBuffer.rewind()
        
        // 创建 ONNX Tensor
        val shape = longArrayOf(1, 1, height.toLong(), width.toLong())
        return OnnxTensor.createTensor(ortEnv, floatBuffer, shape)
    }
    
    /**
     * CTC 解码
     * 将模型输出转换为字符串
     */
    private fun ctcDecode(tensor: OnnxTensor): String {
        // 输出形状: [sequence_length, batch_size, num_classes]
        val data = tensor.floatBuffer
        val shape = tensor.info.shape  // e.g., [T, 1, 6964] 或 [1, T, 6964]
        
        println("[DdddOcr] 输出形状: ${shape.contentToString()}, charset大小: ${charset.size}")
        
        // 确定各维度
        val seqLength: Int
        val numClasses: Int
        
        if (shape.size == 3) {
            // 通常是 [T, 1, num_classes] 或 [1, T, num_classes]
            if (shape[1] == 1L) {
                seqLength = shape[0].toInt()
                numClasses = shape[2].toInt()
            } else {
                seqLength = shape[1].toInt()
                numClasses = shape[2].toInt()
            }
        } else if (shape.size == 2) {
            seqLength = shape[0].toInt()
            numClasses = shape[1].toInt()
        } else {
            return ""
        }
        
        println("[DdddOcr] seqLength=$seqLength, numClasses=$numClasses")
        
        // 贪婪解码: 每个时间步取最大概率的类别
        val result = StringBuilder()
        var prevIndex = -1
        
        for (t in 0 until seqLength) {
            // 找到当前时间步概率最大的类别
            var maxIndex = 0
            var maxValue = Float.NEGATIVE_INFINITY
            
            for (c in 0 until numClasses) {
                val value = data.get(t * numClasses + c)
                if (value > maxValue) {
                    maxValue = value
                    maxIndex = c
                }
            }
            
            // CTC 解码规则:
            // - 0 是空白符 (blank)，忽略
            // - 重复字符只保留一个
            // 注意: charset[0] 是空白符，所以直接用 maxIndex 作为索引
            if (maxIndex != 0 && maxIndex != prevIndex) {
                if (maxIndex < charset.size) {
                    val char = charset[maxIndex]
                    if (char.isNotEmpty()) {
                        result.append(char)
                    }
                }
            }
            
            prevIndex = maxIndex
        }
        
        return result.toString()
    }
    
    /**
     * 从 assets 加载字符集
     */
    private fun loadCharset(context: Context) {
        try {
            // 尝试从 assets/charsets_old.json 加载
            val jsonStr = context.assets.open("charsets_old.json").bufferedReader().use { it.readText() }
            // 简单解析 JSON 数组
            charset = parseJsonArray(jsonStr)
            println("[DdddOcr] 从文件加载字符集成功, 大小: ${charset.size}")
        } catch (e: Exception) {
            println("[DdddOcr] 无法加载字符集文件, 使用内置字符集: ${e.message}")
            // 使用内置的简化字符集（仅包含常见验证码字符）
            charset = listOf("") + "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".map { it.toString() }
        }
    }
    
    /**
     * 简单解析 JSON 字符串数组
     */
    private fun parseJsonArray(json: String): List<String> {
        val result = mutableListOf<String>()
        val content = json.trim().removeSurrounding("[", "]")
        
        var i = 0
        while (i < content.length) {
            // 跳过空白和逗号
            while (i < content.length && (content[i].isWhitespace() || content[i] == ',')) {
                i++
            }
            if (i >= content.length) break
            
            // 解析字符串
            if (content[i] == '"' || content[i] == '\'') {
                val quote = content[i]
                i++
                val start = i
                while (i < content.length && content[i] != quote) {
                    if (content[i] == '\\' && i + 1 < content.length) {
                        i += 2 // 跳过转义字符
                    } else {
                        i++
                    }
                }
                val str = content.substring(start, i)
                    .replace("\\n", "\n")
                    .replace("\\t", "\t")
                    .replace("\\\"", "\"")
                    .replace("\\'", "'")
                    .replace("\\\\", "\\")
                result.add(str)
                i++ // 跳过结束引号
            } else {
                i++
            }
        }
        
        return result
    }
    
    /**
     * 释放资源
     */
    fun close() {
        try {
            ortSession?.close()
            ortEnv?.close()
            ortSession = null
            ortEnv = null
            isInitialized = false
            println("[DdddOcr] 资源已释放")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
