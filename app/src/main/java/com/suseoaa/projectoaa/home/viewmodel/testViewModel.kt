import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {
    // 示例：首页显示的问候语状态
    private val _greeting = MutableStateFlow("欢迎来到 Jetpack Compose")
    val greeting = _greeting.asStateFlow()

    fun updateGreeting(isTablet: Boolean) {
        _greeting.value = if (isTablet) "当前是平板/桌面模式" else "当前是手机模式"
    }
}