package com.suseoaa.projectoaa.shared.domain.engine

import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual object CampusAiEngine {
    private var context: android.content.Context? = null

    actual fun initContext(context: Any) {
        if (context is android.content.Context) {
            this.context = context.applicationContext
        }
    }

    actual fun isModelAvailable(): Boolean {
        val ctx = context ?: return false
        val modelDir = File(ctx.filesDir, "ai_models")
        return modelDir.listFiles()?.any { it.isFile && it.length() > 100L * 1024 * 1024 } == true
    }

    private var llmInference: LlmInference? = null

    actual suspend fun loadModel(): Boolean = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext false
        if (llmInference != null) return@withContext true
        
        try {
            val modelDir = File(ctx.filesDir, "ai_models")
            android.util.Log.d("AiLab", "CampusAiEngine.loadModel() started. Checking modelDir: ${modelDir.absolutePath}")
            
            // 只要是大于 100MB 的文件就认为是模型
            val validFiles = modelDir.listFiles()?.filter { it.isFile && it.length() > 100L * 1024 * 1024 } ?: emptyList()
            
            // 优先加载 cpu 版本的模型，以规避某些设备的 OpenCL 崩溃问题
            var modelFile = validFiles.find { it.name.contains("cpu") } ?: validFiles.firstOrNull()
            
            if (modelFile == null || !modelFile.exists()) {
                val allFiles = modelDir.listFiles()?.joinToString { it.name + "(" + it.length() + ")" } ?: "null"
                android.util.Log.e("AiLab", "No valid model file found! Directory files: $allFiles")
                return@withContext false
            }
            
            android.util.Log.d("AiLab", "Found model file: ${modelFile.name}, size: ${modelFile.length()}")

            // 如果之前错误地将 .litertlm 加上了 .task 后缀，则纠正回 .bin
            if (modelFile.name.endsWith(".task") && modelFile.name.contains(".litertlm")) {
                val newFile = File(modelDir, modelFile.name.removeSuffix(".task") + ".bin")
                modelFile.renameTo(newFile)
                modelFile = newFile
                android.util.Log.d("AiLab", "Corrected previous .task extension to .bin: ${modelFile.name}")
            }

            // 强制重命名：MediaPipe 底层严格校验后缀必须是 .bin 或 .task
            // 对于单纯的权重文件（如 HuggingFace 下载的 .litertlm），必须是 .bin 才能按 FlatBuffer 解析，.task 会被当做 zip 解析导致崩溃！
            if (!modelFile.name.endsWith(".task") && !modelFile.name.endsWith(".bin")) {
                val newFile = File(modelDir, modelFile.name + ".bin")
                modelFile.renameTo(newFile)
                modelFile = newFile
                android.util.Log.d("AiLab", "Appended .bin to raw model file: ${modelFile.name}")
            }

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(2048)
                .setTopK(40)
                .setTemperature(0.8f)
                .build()

            android.util.Log.d("AiLab", "Creating LlmInference with modelPath: ${modelFile.absolutePath}...")
            llmInference = LlmInference.createFromOptions(ctx, options)
            android.util.Log.d("AiLab", "LlmInference created successfully!")
            true
        } catch (e: Exception) {
            android.util.Log.e("AiLab", "Exception in loadModel: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }

    actual suspend fun unloadModel() {
        llmInference?.close()
        llmInference = null
    }

    actual suspend fun summarizeAcademicMessage(content: String): String = withContext(Dispatchers.IO) {
        val engine = llmInference ?: return@withContext "模型未加载"
        val prompt = """<start_of_turn>user
你是一个教务通知信息提取助手。请根据提供的单条调课通知，提取出关键信息，并严格按照以下两行格式输出：
【原】[原时间/地点/教师]
【新】[新时间/地点/教师]

要求：
1. 必须且只能输出这两行，绝对不能输出其他任何多余的文字、原话或例子！
2. 语言极其精简，去掉所有废话。
3. 若为换教室/时间/老师，按【新】[变动后内容]输出。
4. 若为停课/取消，则【新】一行直接输出“停课”或“取消”。
5. 若遇到其他未知变动，在【原】中写明涉及课程，在【新】中写明最终状态。

示例1（换老师）：
输入：2026-05-19 17:44:22 换教师提醒:原定陈继鑫老师在第14-17周星期二第9-10节 LA4-507上的信息技术应用创新概论课程现改为曾胜上课, 请各位同学相互告知!
输出：
【原】陈继鑫老师 (第14-17周星期二9-10节 LA4-507)
【新】曾胜老师

示例2（停课）：
输入：停课提醒:原定赵飞翔老师在第11周星期二第5-6节于LA6-455上的Web应用开发课程停上，请各位同学相互告知！
输出：
【原】赵飞翔老师 (第11周星期二5-6节 LA6-455)
【新】停课

示例3（调时间/换教室）：
输入：调课提醒:原定于第11周星期二第5-6节在LA6-455上的Web应用开发课程，现调至第11周星期四第1-2节LA6-455，请相互转告。
输出：
【原】Web应用开发课程 (第11周星期二5-6节 LA6-455)
【新】第11周星期四1-2节 LA6-455

现在请处理以下通知：
$content<end_of_turn>
<start_of_turn>model
"""
        try {
            engine.generateResponse(prompt)
        } catch (e: Exception) {
            "推理错误: ${e.message}"
        }
    }

    actual suspend fun chatWithContext(context: String, query: String): String = withContext(Dispatchers.IO) {
        val engine = llmInference ?: return@withContext "模型未加载"
        // 外层 ViewModel 已经按照 Gemma 要求的 <start_of_turn> 格式构建了完整的 prompt，通过 context 参数传入
        try {
            engine.generateResponse(context)
        } catch (e: Exception) {
            "推理错误: ${e.message}"
        }
    }
}
