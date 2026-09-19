package com.suseoaa.projectoaa.presentation.checkin

import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinAccountData
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinTask
import com.suseoaa.projectoaa.shared.domain.model.checkin.EduUserInfo

/**
 * 账号筛选类型
 */
enum class AccountFilterType {
    ALL,           // 全部账号
    PASSWORD,      // 账号密码登录
    QRCODE,        // 扫码登录
    CAMPUS_YIBIN,  // 宜宾校区
    CAMPUS_LIBAIHE,// 李白河校区
    CAMPUS_HUIDONG // 汇东校区
}

/**
 * 652打卡 UI 状态
 */
@Suppress("ArrayInDataClass")
data class CheckinUiState(
    val accounts: List<CheckinAccountData> = emptyList(),
    val accountFilter: AccountFilterType = AccountFilterType.ALL,  // 账号筛选
    val isLoading: Boolean = false,
    val currentCheckingAccount: CheckinAccountData? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    // 任务列表
    val pendingTasks: List<CheckinTask> = emptyList(),      // 待打卡任务
    val completedTasks: List<CheckinTask> = emptyList(),    // 已打卡任务
    val absentTasks: List<CheckinTask> = emptyList(),       // 缺勤任务（未打卡）
    val isLoadingTasks: Boolean = false,
    val selectedAccount: CheckinAccountData? = null,        // 当前查看任务的账号
    val checkingTaskId: Long? = null,                       // 当前正在打卡的任务ID（per-task状态）
    // 已打卡任务分页显示状态
    val displayedCompletedCount: Int = 6,                   // 当前显示的已打卡任务数量
    val isLoadingMoreCompleted: Boolean = false,            // 是否正在加载更多
    // 编辑对话框状态
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingAccount: CheckinAccountData? = null,
    // 验证码对话框状态（密码登录）
    val showCaptchaDialog: Boolean = false,
    val captchaImageBytes: ByteArray? = null,
    val isLoadingCaptcha: Boolean = false,
    val isLoggingIn: Boolean = false,
    // 短信二次验证对话框状态
    val showSmsDialog: Boolean = false,
    val smsMaskedPhone: String? = null,
    val isSendingSmsCode: Boolean = false,
    val isVerifyingSmsCode: Boolean = false,
    val smsResendCountdownSeconds: Int = 0,
    // 扫码登录对话框状态
    val showQrCodeDialog: Boolean = false,
    val qrCodeImage: String? = null,         // 二维码图片 (Base64)
    val qrCodeClientId: String? = null,      // 用于轮询的 ClientId
    val isLoadingQrCode: Boolean = false,
    val qrCodeScanStatus: QrCodeScanStatus = QrCodeScanStatus.WAITING,
    val scannedStudentId: String? = null,    // 扫码后获取的学号
    val scannedName: String? = null,         // 扫码后获取的姓名
    val scannedCookies: String? = null,      // 扫码登录后的完整 Cookie
    // 需要重新扫码登录的账号（Session过期）
    val accountNeedRelogin: CheckinAccountData? = null,
    val showReloginDialog: Boolean = false,
    // WebView 扫码登录对话框状态 (保留兼容)
    val showWebViewLoginDialog: Boolean = false,
    val qrCodeUrl: String? = null,           // 旧字段，保留兼容
    val scannedUserInfo: EduUserInfo? = null // 旧字段，保留兼容
)

/**
 * 二维码扫描状态
 */
enum class QrCodeScanStatus {
    WAITING,    // 等待扫描
    SCANNED,    // 已扫描，等待确认
    CONFIRMED,  // 已确认
    EXPIRED,    // 已过期
    ERROR       // 错误
}

/** 按当前筛选条件过滤后的账号列表 */
val CheckinUiState.filteredAccounts: List<CheckinAccountData>
    get() = when (accountFilter) {
        AccountFilterType.ALL -> accounts
        AccountFilterType.PASSWORD -> accounts.filter { !it.isQrCodeLogin }
        AccountFilterType.QRCODE -> accounts.filter { it.isQrCodeLogin }
        AccountFilterType.CAMPUS_YIBIN -> accounts.filter { it.selectedLocation == "宜宾" }
        AccountFilterType.CAMPUS_LIBAIHE -> accounts.filter { it.selectedLocation == "李白河" }
        AccountFilterType.CAMPUS_HUIDONG -> accounts.filter { it.selectedLocation == "汇东" }
    }
