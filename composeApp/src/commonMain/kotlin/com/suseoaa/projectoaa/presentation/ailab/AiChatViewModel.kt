package com.suseoaa.projectoaa.presentation.ailab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class AiChatMessage(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val isThinking: Boolean = false // 用于指示正在深度思考中
)

data class AiChatUiState(
    val messages: List<AiChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val isDeepThinkingEnabled: Boolean = false,
    val isModelLoading: Boolean = true,
    val isModelLoaded: Boolean = false
)

class AiChatViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    init {
        // 添加一句初始欢迎语
        _uiState.update {
            it.copy(
                messages = listOf(
                    AiChatMessage(
                        id = "welcome",
                        content = "你好！我是你的本地学术 AI 助手。\n\n由于模型运行在你的设备本地，我的所有回答和你的数据**绝不会上传云端**。你可以问我关于课程表、考试安排、或者让我帮你总结资料。试试看吧！",
                        isUser = false
                    )
                )
            )
        }
        
        ensureModelLoaded()
    }

    private fun ensureModelLoaded() {
        println("AiLab: AiChatViewModel.ensureModelLoaded() called")
        viewModelScope.launch {
            _uiState.update { it.copy(isModelLoading = true) }
            val success = com.suseoaa.projectoaa.shared.domain.engine.CampusAiEngine.loadModel()
            println("AiLab: AiChatViewModel CampusAiEngine.loadModel() returned $success")
            _uiState.update { it.copy(isModelLoading = false, isModelLoaded = success) }
        }
    }

    fun toggleDeepThinking(enabled: Boolean) {
        _uiState.update { it.copy(isDeepThinkingEnabled = enabled) }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _uiState.value.isGenerating) return

        val userMessage = AiChatMessage(
            id = Clock.System.now().toEpochMilliseconds().toString() + "_user",
            content = text,
            isUser = true
        )

        // 添加用户消息，并设置生成状态
        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage,
                isGenerating = true
            )
        }

        // 调用真实 AI 引擎
        viewModelScope.launch {
            val aiMessageId = Clock.System.now().toEpochMilliseconds().toString() + "_ai"
            
            // 先在界面上显示一个空的或“思考中...”的状态
            _uiState.update { state ->
                state.copy(
                    messages = state.messages + AiChatMessage(
                        id = aiMessageId,
                        content = if (_uiState.value.isDeepThinkingEnabled) "<think>\n正在深度思考中...\n</think>\n" else "思考中...",
                        isUser = false,
                        isThinking = _uiState.value.isDeepThinkingEnabled
                    )
                )
            }
            
            // 确保模型已经加载 (考虑到可能尚未加载)
            com.suseoaa.projectoaa.shared.domain.engine.CampusAiEngine.loadModel()

            // 构建历史上下文，严格遵守 Gemma 模型指令微调（IT）格式：
            // <start_of_turn>user\n...\n<end_of_turn>\n<start_of_turn>model\n...\n<end_of_turn>\n
            val historyTokens = _uiState.value.messages
                .filter { it.id != aiMessageId && it.id != "welcome" } // 排除系统预设欢迎语，避免干扰
                .takeLast(6)
                .joinToString("") { msg ->
                    if (msg.isUser) {
                        "<start_of_turn>user\n${msg.content}<end_of_turn>\n"
                    } else {
                        // 如果有思考过程（<think>...</think>），最好保留，这有助于模型保持逻辑连贯
                        "<start_of_turn>model\n${msg.content}<end_of_turn>\n"
                    }
                }
            
            val deepThinkingInstruction = if (_uiState.value.isDeepThinkingEnabled) {
                "\n[系统要求：必须进行深度推理。请在回答的最开头使用 <think> 和 </think> 标签包裹你的详细思考逻辑，然后再输出正式回答！]"
            } else ""

            val finalQuery = text + deepThinkingInstruction
            
            // 拼接最新一条 user 提问并加上 model 生成的触发头
            val fullPrompt = historyTokens + "<start_of_turn>user\n$finalQuery<end_of_turn>\n<start_of_turn>model\n"

            // 进行真实推理 (底层已修改为直接传递完整 formatted prompt)
            val answer = com.suseoaa.projectoaa.shared.domain.engine.CampusAiEngine.chatWithContext(fullPrompt, "")
            
            updateLastAiMessage(answer, isThinking = false)
            _uiState.update { it.copy(isGenerating = false) }
        }
    }

    private fun updateLastAiMessage(newContent: String, isThinking: Boolean = false) {
        _uiState.update { state ->
            val messages = state.messages.toMutableList()
            if (messages.isNotEmpty() && !messages.last().isUser) {
                messages[messages.lastIndex] = messages.last().copy(
                    content = newContent,
                    isThinking = isThinking
                )
            }
            state.copy(messages = messages)
        }
    }
}
