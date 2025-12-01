package com.suseoaa.projectoaa.student.ui

import android.app.DatePickerDialog
import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.suseoaa.projectoaa.student.viewmodel.StudentFormViewModel
import java.util.Calendar

// 定义部门列表常量
val DEPARTMENTS = listOf("算法竞赛部", "项目实践部", "组织宣传部", "秘书处", "技术服务部")

/**
 * 表单填写页主入口
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentFormScreen(
    onBack: () -> Unit,
    viewModel: StudentFormViewModel = viewModel()
) {
    // 每次进入初始化类型
    LaunchedEffect(Unit) { viewModel.initType("通用") }
    val context = LocalContext.current

    Scaffold(
        containerColor = Color.Transparent, // 保持背景透明，显示壁纸
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("申请报名表") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        // 使用 Box + LocalConfiguration 替代 BoxWithConstraints，避免 Scope 报错
        Box(modifier = Modifier.padding(padding)) {

            // 1. 获取屏幕配置
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp.dp

            // 2. 判断是否为横屏且宽度足够 (平板横屏模式)
            val isWide = screenWidth > 600.dp && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            // 3. 渲染响应式内容
            ResponsiveFormContent(viewModel, isWide) {
                viewModel.submitForm(context) {
                    Toast.makeText(context, "提交成功！", Toast.LENGTH_SHORT).show()
                    onBack()
                }
            }
        }

        // Loading 遮罩
        if (viewModel.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

/**
 * 响应式表单内容
 * 根据 isWide 参数决定是 单列布局 还是 双列布局
 */
@Composable
fun ResponsiveFormContent(
    viewModel: StudentFormViewModel,
    isWide: Boolean,
    onSubmit: () -> Unit
) {
    val formData = viewModel.formData
    val errors = viewModel.formErrors
    val scrollState = rememberScrollState() // 竖屏时的滚动状态

    // 图片选择器
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        viewModel.updatePhoto(uri)
    }

    // 日期选择器
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, y, m, d -> viewModel.updateBirthDate("$y-${m + 1}-$d") },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // --- 子组件：基本信息卡片 ---
    @Composable
    fun BasicInfoSection() {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 顶部：头像与姓名性别
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 头像区域
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray)
                            .clickable { photoLauncher.launch("image/*") }
                            .border(
                                width = if (errors.photoError != null) 2.dp else 1.dp,
                                color = if (errors.photoError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (formData.photoUri != null) {
                            Image(
                                rememberAsyncImagePainter(formData.photoUri),
                                null,
                                Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.AccountCircle, null, Modifier.size(48.dp), tint = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // 姓名与性别
                    Column(modifier = Modifier.weight(1f)) {
                        CompactTextField(formData.name, "姓名", error = errors.name) { viewModel.updateName(it) }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("性别:", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.width(8.dp))
                            RadioButton(selected = formData.gender == "男", onClick = { viewModel.updateGender("男") })
                            Text("男", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.width(8.dp))
                            RadioButton(selected = formData.gender == "女", onClick = { viewModel.updateGender("女") })
                            Text("女", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // 学院班级
                CompactTextField(formData.college, "所在学院", error = errors.college) { viewModel.updateCollege(it) }
                CompactTextField(formData.majorClass, "专业班级", error = errors.majorClass) { viewModel.updateMajorClass(it) }

                // 日期与面貌 (一行两个)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CompactDateTextField(formData.birthDate, "出生年月", Modifier.weight(1f), error = errors.birthDate) { datePickerDialog.show() }
                    CompactTextField(formData.politicalStatus, "政治面貌", Modifier.weight(1f), error = errors.politicalStatus) { viewModel.updatePoliticalStatus(it) }
                }

                // 联系方式 (一行两个)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CompactTextField(formData.phoneNumber, "联系电话", Modifier.weight(1f), isNumber = true, error = errors.phoneNumber) { viewModel.updatePhone(it) }
                    CompactTextField(formData.qq, "QQ号码", Modifier.weight(1f), isNumber = true, error = errors.qq) { viewModel.updateQQ(it) }
                }
            }
        }
    }

    // --- 子组件：详细信息卡片 ---
    @Composable
    fun DetailInfoSection() {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "竞选意向",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                CompactDropdownMenu(formData.firstChoice, "第一志愿(必选)", DEPARTMENTS, error = errors.firstChoice) { viewModel.updateFirstChoice(it) }

                CompactDropdownMenu(
                    formData.secondChoice,
                    "第二志愿(可选)",
                    listOf("无") + DEPARTMENTS.filter { it != formData.firstChoice },
                    error = errors.secondChoice
                ) { viewModel.updateSecondChoice(it) }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("是否服从调剂?", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = formData.isObeyAdjustment, onCheckedChange = { viewModel.updateObeyAdjustment(it) })
                }

                CompactTextArea(formData.resume, "个人简历", error = errors.resume) { viewModel.updateResume(it) }
                CompactTextArea(formData.reason, "竞选理由", error = errors.reason) { viewModel.updateReason(it) }

                Spacer(Modifier.height(12.dp))

                // 提交按钮
                Button(
                    onClick = onSubmit,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !viewModel.isLoading
                ) {
                    Text(if (viewModel.isLoading) "校验中..." else "提交申请", fontSize = 18.sp)
                }
            }
        }
    }

    // --- 布局逻辑 ---
    if (isWide) {
        // 平板横屏：左右双栏
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 左侧基本信息 (可独立滚动)
            Column(modifier = Modifier.weight(0.4f).verticalScroll(rememberScrollState())) {
                BasicInfoSection()
                Spacer(Modifier.height(80.dp))
            }
            // 右侧详细信息 (可独立滚动)
            Column(modifier = Modifier.weight(0.6f).verticalScroll(rememberScrollState())) {
                DetailInfoSection()
                Spacer(Modifier.height(80.dp))
            }
        }
    } else {
        // 手机竖屏：单列垂直
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BasicInfoSection()
            DetailInfoSection()
            Spacer(Modifier.height(30.dp))
        }
    }
}