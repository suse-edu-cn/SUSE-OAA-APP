package com.suseoaa.projectoaa.shared.domain.engine

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import java.io.File

actual object CampusAiEngine {
    private var context: android.content.Context? = null

    /**
     * Engine 是LiteRT-LM框架的核心入口对象，负责加载模型权重并管理底层推理运行时的完整生命周期。
     * 与旧版MediaPipe Tasks GenAI的LlmInference相比，Engine采用了全新的Backend抽象层设计，
     * 使得GPU/CPU切换可以通过声明式配置完成，而不再依赖特定硬件驱动的符号表绑定。
     * Engine是一个重量级对象，单个进程生命周期内仅应持有一个实例，推理结束后必须调用close()释放资源。
     */
    private var inferenceEngine: Engine? = null

    actual fun initContext(context: Any) {
        if (context is android.content.Context) {
            this.context = context.applicationContext
        }
    }

    /**
     * 用户设置的GPU待假首选开关，true表示优先尝试GPU后端，false表示直接使用CPU后端。
     * 用户在界面切换后需先调用unloadModel()释放当前引擎，再调loadModel()重建新配置的引擎实例。
     */
    private var userPreferGpu: Boolean = true
    private var gpuCrashDetected: Boolean = false

    actual fun setPreferGpu(preferGpu: Boolean) {
        userPreferGpu = preferGpu
        // 如果引擎已加载，需先卸载再重新加载才能生效
        if (inferenceEngine != null) {
            inferenceEngine?.close()
            inferenceEngine = null
        }
    }

    private var targetModelFileName: String? = null

    actual fun setTargetModelFileName(fileName: String?) {
        targetModelFileName = fileName
        android.util.Log.d("AiLab", "Target model file set to: ${fileName ?: "<auto>"}")
        if (inferenceEngine != null) {
            inferenceEngine?.close()
            inferenceEngine = null
        }
    }

    actual fun lastGpuCrashDetected(): Boolean {
        val result = gpuCrashDetected
        gpuCrashDetected = false
        return result
    }

    actual fun isModelAvailable(): Boolean {
        val applicationContext = context ?: return false
        val modelDirectory = File(applicationContext.filesDir, "ai_models")
        return modelDirectory.listFiles()?.any(::isLoadableModelFile) == true
    }

    /**
     * 加载并初始化端侧推理引擎。
     *
     * 本函数采用"GPU优先，异常安全降级至CPU"的双后端策略：
     * 1. 首先尝试以Backend.GPU()配置构建EngineConfig并完成engine.initialize()。
     *    在骁龙8 Gen 3（Adreno 750）等现代SoC上，AdrenoGPU的半精度矩阵运算吞吐量
     *    显著高于大核CPU集群，可大幅降低首Token延迟和整体推理功耗。
     *    LiteRT-LM内置的弱符号探测机制（Weak Symbol Probing）会在运行时检测驱动符号的
     *    实际可用性，对于clSetPerfHintQCOM等厂商私有扩展符号缺失的情况会自动跳过而非崩溃。
     * 2. 若GPU后端初始化过程中抛出任何异常，则自动降级至Backend.CPU()模式。
     *    降级时，通过Runtime.getRuntime().availableProcessors()获取设备实际可用核心数，
     *    并将其作为CPU线程数配置，确保在所有Android设备上均能完成推理而不会进程闪退。
     *
     * @return 初始化成功返回 true，模型文件不存在或引擎初始化失败返回 false。
     */
    actual suspend fun loadModel(): Boolean = withContext(Dispatchers.IO) {
        val applicationContext = context ?: return@withContext false

        // 如果引擎已完成初始化，直接返回成功，避免重复加载
        if (inferenceEngine != null) return@withContext true

        try {
            val modelDirectory = File(applicationContext.filesDir, "ai_models")
            val liteRtCacheDirectory = File(applicationContext.cacheDir, "litertlm").apply {
                mkdirs()
            }
            android.util.Log.d("AiLab", "CampusAiEngine.loadModel() started. Checking modelDir: ${modelDirectory.absolutePath}")

            val validModelFiles = modelDirectory.listFiles()?.filter(::isLoadableModelFile) ?: emptyList()

            // 如果设置了确切的目标文件，则优先寻找该文件
            var selectedModelFile: File? = null
            if (targetModelFileName != null) {
                val targetCandidates = buildTargetModelFileCandidates(targetModelFileName!!)
                android.util.Log.d("AiLab", "Looking for target model: $targetModelFileName, candidates: $targetCandidates")
                selectedModelFile = validModelFiles.find { it.name in targetCandidates }
                if (selectedModelFile == null) {
                    android.util.Log.w(
                        "AiLab",
                        "Target model file not found. Available valid models: ${validModelFiles.joinToString { it.name }}"
                    )
                }
            }

            // 如果没有命中目标文件，回退到以前的逻辑：优先加载包含 cpu 标记的版本，或者取第一个可用模型
            if (selectedModelFile == null) {
                selectedModelFile = validModelFiles.find { it.name.contains("cpu") } ?: validModelFiles.firstOrNull()
            }

            if (selectedModelFile == null || !selectedModelFile.exists()) {
                val allFileDescriptions = modelDirectory.listFiles()?.joinToString { it.name + "(" + it.length() + ")" } ?: "null"
                android.util.Log.e("AiLab", "No valid model file found! Directory files: $allFileDescriptions")
                return@withContext false
            }

            android.util.Log.d("AiLab", "Found model file: ${selectedModelFile.name}, size: ${selectedModelFile.length()}")

            // 如果之前错误地将 .litertlm 加上了 .task 后缀，则纠正回 .bin
            // 注意：LiteRT-LM框架原生支持 .litertlm 格式，此处重命名逻辑针对历史遗留文件
            var correctedModelFile = selectedModelFile
            if (correctedModelFile.name.endsWith(".task") && correctedModelFile.name.contains(".litertlm")) {
                val renamedFile = File(modelDirectory, correctedModelFile.name.removeSuffix(".task") + ".bin")
                correctedModelFile.renameTo(renamedFile)
                correctedModelFile = renamedFile
                android.util.Log.d("AiLab", "Corrected previous .task extension to .bin: ${correctedModelFile.name}")
            }

            // 强制重命名：MediaPipe 底层严格校验后缀必须是 .bin 或 .task
            // 对于单纯的权重文件（如 HuggingFace 下载的 .litertlm），必须是 .bin 才能按 FlatBuffer 解析，.task 会被当做 zip 解析导致崩溃！
            // 注意：若文件本身已是标准 .litertlm 格式，LiteRT-LM可直接解析，此处额外兼容旧版文件命名规则
            if (!correctedModelFile.name.endsWith(".task") && !correctedModelFile.name.endsWith(".bin") && !correctedModelFile.name.endsWith(".litertlm")) {
                val renamedFile = File(modelDirectory, correctedModelFile.name + ".bin")
                correctedModelFile.renameTo(renamedFile)
                correctedModelFile = renamedFile
                android.util.Log.d("AiLab", "Appended .bin to raw model file: ${correctedModelFile.name}")
            }

            if (userPreferGpu) {
                if (tryInitializeWithGpuBackend(correctedModelFile.absolutePath, liteRtCacheDirectory.absolutePath)) {
                    return@withContext true
                }
            } else {
                android.util.Log.d("AiLab", "User preference: CPU mode. Skipping GPU backend attempt.")
            }

            if (tryInitializeWithCpuBackend(correctedModelFile.absolutePath, correctedModelFile.name, liteRtCacheDirectory.absolutePath)) {
                return@withContext true
            }

            val fallbackModelFiles = validModelFiles
                .filter { it.absolutePath != correctedModelFile.absolutePath }
                .sortedWith(
                    compareBy<File> { it.name.contains("qualcomm", ignoreCase = true) }
                        .thenBy { !it.name.endsWith(".litertlm", ignoreCase = true) }
                        .thenBy { it.name }
                )

            for (fallbackModelFile in fallbackModelFiles) {
                val fallbackCorrectedModelFile = normalizeModelFile(modelDirectory, fallbackModelFile)
                android.util.Log.w(
                    "AiLab",
                    "Selected model ${correctedModelFile.name} is not compatible with LiteRT-LM on this device/runtime. Trying fallback model: ${fallbackCorrectedModelFile.name}"
                )
                if (tryInitializeWithCpuBackend(fallbackCorrectedModelFile.absolutePath, fallbackCorrectedModelFile.name, liteRtCacheDirectory.absolutePath)) {
                    return@withContext true
                }
            }

            false
        } catch (e: Exception) {
            android.util.Log.e("AiLab", "Exception in loadModel: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }

    private fun buildTargetModelFileCandidates(fileName: String): Set<String> {
        val withoutTask = fileName.removeSuffix(".task")
        val withoutBin = withoutTask.removeSuffix(".bin")
        val withoutLiteRtLm = withoutBin.removeSuffix(".litertlm")
        return setOf(
            fileName,
            "$fileName.task",
            "$fileName.bin",
            withoutTask,
            "$withoutTask.task",
            "$withoutTask.bin",
            "$withoutLiteRtLm.litertlm",
            "$withoutLiteRtLm.litertlm.task",
            "$withoutLiteRtLm.bin"
        )
    }

    private fun isLoadableModelFile(file: File): Boolean {
        if (!file.isFile || file.length() <= 100L * 1024 * 1024) return false
        val lowerName = file.name.lowercase()
        if (lowerName.contains("cache")) return false
        return lowerName.endsWith(".litertlm") ||
            lowerName.endsWith(".task") ||
            lowerName.endsWith(".bin")
    }

    private fun normalizeModelFile(modelDirectory: File, modelFile: File): File {
        var correctedModelFile = modelFile
        if (correctedModelFile.name.endsWith(".task") && correctedModelFile.name.contains(".litertlm")) {
            val renamedFile = File(modelDirectory, correctedModelFile.name.removeSuffix(".task") + ".bin")
            correctedModelFile.renameTo(renamedFile)
            correctedModelFile = renamedFile
            android.util.Log.d("AiLab", "Corrected previous .task extension to .bin: ${correctedModelFile.name}")
        }

        if (!correctedModelFile.name.endsWith(".task") &&
            !correctedModelFile.name.endsWith(".bin") &&
            !correctedModelFile.name.endsWith(".litertlm")
        ) {
            val renamedFile = File(modelDirectory, correctedModelFile.name + ".bin")
            correctedModelFile.renameTo(renamedFile)
            correctedModelFile = renamedFile
            android.util.Log.d("AiLab", "Appended .bin to raw model file: ${correctedModelFile.name}")
        }

        return correctedModelFile
    }

    /**
     * 尝试以GPU后端构建并初始化推理引擎。
     * 若GPU后端初始化成功则将inferenceEngine赋值并返回 true，
     * 若捕获到任何异常（包括驱动不兼容、符号缺失等底层错误）则返回 false。
     *
     * @param applicationContext Android应用上下文，用于获取缓存目录路径。
     * @param modelFilePath 模型文件的绝对路径。
     * @return GPU后端初始化是否成功。
     */
    private fun tryInitializeWithGpuBackend(
        modelFilePath: String,
        cacheDirPath: String
    ): Boolean {
        return try {
            // Backend.GPU() 向LiteRT-LM运行时声明期望使用GPU硬件加速路径。
            // 在Adreno GPU上，框架会优先尝试OpenCL后端，失败时自动降级至Vulkan Compute Shader后端。
            val gpuBackendConfiguration = Backend.GPU()
            val gpuEngineConfig = EngineConfig(
                modelPath = modelFilePath,
                backend = gpuBackendConfiguration,
                cacheDir = cacheDirPath
            )

            android.util.Log.d("AiLab", "Creating Engine with GPU backend for modelPath: $modelFilePath, cacheDir: $cacheDirPath")
            val gpuEngine = Engine(gpuEngineConfig)
            gpuEngine.initialize()
            inferenceEngine = gpuEngine
            android.util.Log.d("AiLab", "Engine initialized successfully with GPU backend.")
            gpuCrashDetected = false
            true
        } catch (gpuInitializationException: Exception) {
            android.util.Log.w(
                "AiLab",
                "GPU backend initialization failed: ${gpuInitializationException.message}. Will attempt CPU fallback."
            )
            gpuCrashDetected = true
            false
        }
    }

    private fun tryInitializeWithCpuBackend(modelFilePath: String, modelFileName: String, cacheDirPath: String): Boolean {
        return try {
            val availableProcessorCount = Runtime.getRuntime().availableProcessors()
            val cpuBackendConfiguration = Backend.CPU(numOfThreads = availableProcessorCount)
            val cpuEngineConfig = EngineConfig(
                modelPath = modelFilePath,
                backend = cpuBackendConfiguration,
                cacheDir = cacheDirPath
            )

            android.util.Log.d(
                "AiLab",
                "Creating Engine with CPU backend for $modelFileName using $availableProcessorCount threads..."
            )
            val cpuEngine = Engine(cpuEngineConfig)
            cpuEngine.initialize()
            inferenceEngine = cpuEngine
            android.util.Log.d("AiLab", "Engine initialized successfully with CPU backend for $modelFileName.")
            true
        } catch (cpuInitializationException: Exception) {
            android.util.Log.w(
                "AiLab",
                "CPU backend initialization failed for $modelFileName: ${cpuInitializationException.message}"
            )
            false
        }
    }

    /**
     * 卸载推理引擎并释放所有持有的GPU/CPU硬件资源。
     * close() 方法会触发LiteRT-LM底层对OpenCL上下文、Vulkan设备句柄以及模型权重内存映射的清理工作。
     * 必须在推理任务结束后调用，否则会导致后台进程持续占用高功耗硬件资源。
     */
    actual suspend fun unloadModel() {
        inferenceEngine?.close()
        inferenceEngine = null
    }

    /**
     * 对单条教务通知执行端侧AI摘要推理。
     *
     * 本函数通过以下流程完成推理：
     * 1. 从inferenceEngine创建一个独立的Conversation会话对象。
     *    Conversation封装了单次对话的完整KV缓存（Key-Value Cache）状态，
     *    多次调用之间互不干扰，天然支持并发安全。
     * 2. 构建符合Gemma Instruct模型规范的控制令牌格式prompt，通过Message.of()封装为标准消息对象。
     * 3. 调用conversation.sendMessageAsync()发起异步流式推理，通过Flow收集全部token后拼接为完整响应。
     * 4. 调用conversation.close()释放此次会话所占用的KV缓存内存。
     *
     * @param content 原始教务通知文本。
     * @return AI提炼的结构化摘要字符串，或错误描述。
     */
    actual suspend fun summarizeAcademicMessage(content: String): String = withContext(Dispatchers.IO) {
        val activeEngine = inferenceEngine ?: return@withContext "模型未加载"

        val structuredPrompt = """<start_of_turn>user
你是一个教务通知信息提取助手。请根据提供的单条调课通知，提取出关键信息，并严格按照以下两行格式输出：
【原】[原时间/地点/教师]
【新】[新时间/地点/教师]

要求：
1. 必须且只能输出这两行，绝对不能输出其他任何多余的文字、原话或例子！
2. 语言极其精简，去掉所有废话。
3. 若为换教室/时间/老师，按【新】[变动后内容]输出。
4. 若为停课/取消，则【新】一行直接输出"停课"或"取消"。
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

        // Conversation是LiteRT-LM中负责管理单次对话KV缓存状态的核心组件。
        // 每次推理任务独立创建并在完成后销毁，确保摘要任务之间不会产生上下文污染。
        val conversation = activeEngine.createConversation()
        return@withContext try {
            // sendMessageAsync返回Flow<String>，每个元素代表模型实时生成的一个token片段（流式解码）。
            // 对于摘要场景，我们收集全部token后拼接为完整字符串。
            val tokenFlow = conversation.sendMessageAsync(Message.user(structuredPrompt))
            val allTokens = tokenFlow.toList()
            allTokens.joinToString(separator = "")
        } catch (inferenceException: Exception) {
            android.util.Log.e("AiLab", "Inference error in summarizeAcademicMessage: ${inferenceException.message}", inferenceException)
            "推理错误: ${inferenceException.message}"
        } finally {
            // 释放本次会话占用的KV缓存内存，防止多次点击"重新总结"时内存持续累积
            conversation.close()
        }
    }

    /**
     * 执行自由对话推理。
     *
     * 外层ViewModel（AiChatViewModel）已经按照Gemma Instruct模型要求的<start_of_turn>控制令牌格式
     * 构建了包含完整多轮历史的prompt字符串，通过context参数传入本函数。
     * 本函数负责创建会话、执行流式推理并将完整响应返回给调用方。
     *
     * @param context 由外层ViewModel构建的完整上下文或多轮对话prompt字符串。
     * @param query 当前用户输入的原始文本；非空时会和context一起构造成最终问题。
     * @return 模型生成的完整响应字符串，或错误描述。
     */
    actual suspend fun chatWithContext(context: String, query: String): String = withContext(Dispatchers.IO) {
        val activeEngine = inferenceEngine ?: return@withContext "模型未加载"

        val prompt = if (query.isBlank()) {
            context
        } else {
            """
            你是校园本地 AI 学业助手。请只根据下面给出的本地真实数据回答用户问题。
            如果数据中没有对应字段或无法确认，请直接说明“当前本地数据中没有记录”，不要编造。
            如果用户问的是课程、老师、学分、成绩、挂科、绩点等具体信息，应优先给出直接结论。

            $context

            用户问题：$query
            """.trimIndent()
        }

        val conversation = activeEngine.createConversation()
        return@withContext try {
            val tokenFlow = conversation.sendMessageAsync(Message.user(prompt))
            val allTokens = tokenFlow.toList()
            allTokens.joinToString(separator = "")
        } catch (inferenceException: Exception) {
            android.util.Log.e("AiLab", "Inference error in chatWithContext: ${inferenceException.message}", inferenceException)
            "推理错误: ${inferenceException.message}"
        } finally {
            conversation.close()
        }
    }
}
