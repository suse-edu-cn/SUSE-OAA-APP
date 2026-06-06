package com.suseoaa.projectoaa.presentation.ailab

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.data.local.TokenManager
import com.suseoaa.projectoaa.shared.data.repository.SchoolGradeRepository
import com.suseoaa.projectoaa.shared.domain.engine.AiToolEngine
import com.suseoaa.projectoaa.shared.domain.engine.CampusAiEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 聊天消息数据模型
 */
data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val isSystem: Boolean = false
)

/**
 * 学业情况分析状态
 */
@Immutable
data class AcademicAnalysisUiState(
    val isModelLoading: Boolean = false,
    val isModelAvailable: Boolean = false,
    val isGenerating: Boolean = false,
    val hasGradesData: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val error: String? = null
)

class AcademicAnalysisViewModel(
    private val tokenManager: TokenManager,
    private val schoolGradeRepository: SchoolGradeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AcademicAnalysisUiState())
    val uiState: StateFlow<AcademicAnalysisUiState> = _uiState.asStateFlow()

    private var aiToolEngine: AiToolEngine? = null

    // 观察学生的成绩数据
    @OptIn(ExperimentalCoroutinesApi::class)
    private val studentGrades = tokenManager.currentStudentId
        .filterNotNull()
        .flatMapLatest { studentId ->
            schoolGradeRepository.observeAllGrades(studentId)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        val isAvailable = com.suseoaa.projectoaa.util.AvailableAiModels.any { 
            com.suseoaa.projectoaa.util.ModelDownloader.isModelDownloaded(it.downloadUrl) 
        }
        _uiState.update { it.copy(isModelAvailable = isAvailable) }

        // 收集成绩数据，初始化引擎
        viewModelScope.launch {
            studentGrades.collect { grades ->
                if (grades.isNotEmpty()) {
                    aiToolEngine = AiToolEngine(grades)
                    _uiState.update { it.copy(hasGradesData = true) }
                    
                    // 第一次进入自动推一条开场白
                    if (_uiState.value.messages.isEmpty()) {
                        addMessage("你好！我是校园本地 AI 学业助手，我已经读取了你的所有成绩数据。你可以问我比如：“我挂了哪些课？”、“距离毕业还差多少学分？”", false, isSystem = true)
                    }
                }
            }
        }
        
        loadModel()
    }

    fun loadModel() {
        println("AiLab: AcademicAnalysisViewModel.loadModel() called")
        if (!uiState.value.isModelAvailable) {
            println("AiLab: AcademicAnalysisViewModel loadModel skipped because isModelAvailable is false")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isModelLoading = true) }
            val success = CampusAiEngine.loadModel()
            println("AiLab: AcademicAnalysisViewModel CampusAiEngine.loadModel() returned $success")
            if (!success) {
                _uiState.update { it.copy(error = "模型加载失败，请重试。") }
            }
            _uiState.update { it.copy(isModelLoading = false) }
        }
    }

    fun sendMessage(query: String) {
        if (query.isBlank()) return
        
        // 1. 回显用户消息
        addMessage(query, isUser = true)

        if (!_uiState.value.hasGradesData) {
            addMessage("抱歉，我没有在本地数据库找到你的成绩数据。请先去成绩页面同步历史成绩。", isUser = false)
            return
        }

        if (!_uiState.value.isModelAvailable) {
            addMessage("抱歉，本地模型未就绪或未下载，暂时无法回答。", isUser = false)
            return
        }

        // 2. 构造上下文和触发生成
        _uiState.update { it.copy(isGenerating = true) }
        
        viewModelScope.launch {
            try {
                // 确保模型已经加载
                com.suseoaa.projectoaa.shared.domain.engine.CampusAiEngine.loadModel()

                // 生成上下文
                val contextStr = aiToolEngine?.buildAcademicContext() ?: ""
                
                // 进行对话
                val reply = CampusAiEngine.chatWithContext(contextStr, query)
                addMessage(reply, isUser = false)
            } catch (e: Exception) {
                addMessage("推理发生异常：${e.message}", isUser = false)
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    private fun addMessage(text: String, isUser: Boolean, isSystem: Boolean = false) {
        _uiState.update { state ->
            val newMessage = ChatMessage(
                id = com.suseoaa.projectoaa.shared.util.OaaClock.now().toEpochMilliseconds().toString() + "_" + (0..1000).random(),
                text = text,
                isUser = isUser,
                isSystem = isSystem
            )
            state.copy(messages = state.messages + newMessage)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        // 页面销毁时释放模型内存
        viewModelScope.launch {
            CampusAiEngine.unloadModel()
        }
    }
}
