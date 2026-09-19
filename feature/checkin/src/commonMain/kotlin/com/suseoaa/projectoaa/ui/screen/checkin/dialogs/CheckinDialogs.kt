package com.suseoaa.projectoaa.ui.screen.checkin.dialogs

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.suseoaa.projectoaa.presentation.checkin.QrCodeScanStatus
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinAccountData
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinLocations
import com.suseoaa.projectoaa.shared.util.AppLog
import com.suseoaa.projectoaa.util.normalizeFont

// 签到流程里的四个对话框：账号编辑、图形验证码、短信验证、扫码登录。

/**
 * 账号编辑对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountDialog(
    title: String,
    initialAccount: CheckinAccountData? = null,
    onDismiss: () -> Unit,
    onConfirm: (studentId: String, password: String, name: String, remark: String, campus: String) -> Unit
) {
    var studentId by remember { mutableStateOf(initialAccount?.studentId ?: "") }
    var password by remember { mutableStateOf(initialAccount?.password ?: "") }
    var name by remember { mutableStateOf(initialAccount?.name ?: "") }
    var remark by remember { mutableStateOf(initialAccount?.remark ?: "") }
    var showPassword by remember { mutableStateOf(false) }
    var selectedCampus by remember {
        mutableStateOf(
            initialAccount?.selectedLocation ?: CheckinLocations.DEFAULT_CAMPUS.name
        )
    }
    var showCampusDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = studentId,
                    onValueChange = { studentId = it.normalizeFont() },
                    label = { Text("学号 *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = initialAccount == null, // 编辑时不允许修改学号
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it.normalizeFont() },
                    label = { Text("密码 *") },
                    singleLine = true,
                    visualTransformation = if (showPassword)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = if (showPassword) "隐藏密码" else "显示密码",
                                tint = if (showPassword)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("姓名（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = { Text("备注（可选）") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // 签到校区选择
                ExposedDropdownMenuBox(
                    expanded = showCampusDropdown,
                    onExpandedChange = { showCampusDropdown = it }
                ) {
                    OutlinedTextField(
                        value = selectedCampus,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("签到校区") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCampusDropdown)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    ExposedDropdownMenu(
                        containerColor = MaterialTheme.colorScheme.background,
                        expanded = showCampusDropdown,
                        onDismissRequest = { showCampusDropdown = false }
                    ) {
                        CheckinLocations.ALL_CAMPUSES.forEach { campus ->
                            DropdownMenuItem(
                                text = { Text("${campus.name}（${campus.locations.size}个位置）") },
                                onClick = {
                                    selectedCampus = campus.name
                                    showCampusDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(studentId, password, name, remark, selectedCampus) },
                enabled = studentId.isNotBlank() && password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("取消")
            }
        }
    )
}

/**
 * 验证码输入对话框
 */
@Composable
internal fun CaptchaDialog(
    captchaImageBytes: ByteArray?,
    isLoading: Boolean,
    isLoggingIn: Boolean,
    accountName: String,
    onRefresh: () -> Unit,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var captchaCode by remember { mutableStateOf("") }
    var isRecognizing by remember { mutableStateOf(false) }
    var ocrError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // 当验证码图片加载后自动进行OCR识别
    LaunchedEffect(captchaImageBytes) {
        if (captchaImageBytes != null && captchaImageBytes.isNotEmpty()) {
            isRecognizing = true
            ocrError = null
            try {
                val result =
                    com.suseoaa.projectoaa.util.PlatformCaptchaOcr.recognize(captchaImageBytes)
                result.onSuccess { recognizedCode ->
                    if (recognizedCode.length == 4) {
                        captchaCode = recognizedCode
                        AppLog.d("[OCR] 自动识别成功: $recognizedCode")
                    } else {
                        AppLog.d("[OCR] 识别结果长度不正确: $recognizedCode (长度: ${recognizedCode.length})")
                        ocrError = "识别结果异常，请手动输入"
                    }
                }.onFailure { e ->
                    AppLog.e("[OCR] 识别失败: ${e.message}")
                    ocrError = "识别失败，请手动输入"
                }
            } catch (e: Throwable) {
                AppLog.e("[OCR] 识别异常: ${e.message}")
                ocrError = "识别异常，请手动输入"
            }
            isRecognizing = false
        }
    }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.background,
        onDismissRequest = { if (!isLoggingIn) onDismiss() },
        title = {
            Text("输入验证码")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 账号信息
                Text(
                    text = "正在为 $accountName 打卡",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 验证码图片
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(enabled = !isLoading && !isLoggingIn) { onRefresh() },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }

                        captchaImageBytes != null -> {
                            Image(
                                painter = rememberAsyncImagePainter(captchaImageBytes),
                                contentDescription = "验证码",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }

                        else -> {
                            Text(
                                text = "点击加载验证码",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 刷新提示
                Text(
                    text = "点击图片刷新验证码",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // OCR识别状态
                if (isRecognizing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "正在自动识别验证码...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (captchaCode.length == 4 && ocrError == null) {
                    Text(
                        text = "已自动识别验证码",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (ocrError != null) {
                    Text(
                        text = ocrError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // 验证码输入框
                OutlinedTextField(
                    value = captchaCode,
                    onValueChange = {
                        // 只允许输入数字和字母，最多4位
                        if (it.length <= 4 && it.all { c -> c.isLetterOrDigit() }) {
                            captchaCode = it
                        }
                    },
                    label = { Text("验证码") },
                    placeholder = { Text("请输入4位验证码") },
                    singleLine = true,
                    enabled = !isLoggingIn,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (captchaCode.length == 4 && !isLoggingIn) {
                                onSubmit(captchaCode)
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // 登录中指示
                if (isLoggingIn) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "正在登录并打卡...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(captchaCode) },
                enabled = captchaCode.length == 4 && !isLoggingIn
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoggingIn
            ) {
                Text("取消")
            }
        }
    )
}

@Composable
internal fun SmsVerificationDialog(
    accountName: String,
    maskedPhone: String?,
    isSendingSms: Boolean,
    isVerifying: Boolean,
    smsResendCountdownSeconds: Int,
    onSendSms: () -> Unit,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var smsCode by remember(accountName, maskedPhone) { mutableStateOf("") }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.background,
        onDismissRequest = { if (!isSendingSms && !isVerifying) onDismiss() },
        title = { Text("短信二次验证") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "正在为 $accountName 登录并打卡",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = maskedPhone?.let { "验证码将发送到：$it" } ?: "请先发送短信验证码",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onSendSms,
                    enabled = smsResendCountdownSeconds == 0 && !isSendingSms && !isVerifying,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (smsResendCountdownSeconds > 0) {
                        Text("${smsResendCountdownSeconds}秒后重发")
                    } else {
                        Text("发送短信验证码")
                    }
                }

                OutlinedTextField(
                    value = smsCode,
                    onValueChange = {
                        if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                            smsCode = it
                        }
                    },
                    label = { Text("短信验证码") },
                    placeholder = { Text("请输入6位验证码") },
                    singleLine = true,
                    enabled = !isVerifying,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (smsCode.length >= 4 && !isVerifying) {
                                onSubmit(smsCode)
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (isVerifying) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "正在验证短信码并打卡...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(smsCode) },
                enabled = smsCode.length >= 4 && !isVerifying
            ) {
                Text("验证并打卡")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSendingSms && !isVerifying
            ) {
                Text("取消")
            }
        }
    )
}

