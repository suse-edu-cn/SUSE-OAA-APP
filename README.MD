# 🦀 青蟹 (Project OAA)

<div align="center">
  <br>
  <b>SUSE 学生的口袋教务助手</b>
  <br>
  <i>Powered by SUSE OpenAtom Open Source Association</i>
</div>

<br>

<div align="center">

![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?logo=kotlin&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/UI-Compose%20Multiplatform-4285F4?logo=jetpackcompose&logoColor=white)
![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20iOS-green)
![Development](https://img.shields.io/badge/Dev-Project%20Dept.-ff69b4)
![License](https://img.shields.io/badge/License-MIT-blue)

[功能特性](#-功能特性) • [技术架构](#-技术架构) • [开发部门](#-开发部门) • [加入我们](#-加入我们)

</div>

---

## 🌊 关于青蟹

**青蟹 (Project OAA)** 是由 **四川轻化工大学开放原子开源协会 (SUSE OAA)** 项目部自主研发的跨平台教务辅助应用。

作为一名 SUSEr，你是否因为教务系统各种繁琐的操作而感到头秃？
我们致力于用技术改变校园生活，通过现代化的 UI 设计和流畅的交互体验，让你一手掌握课表、成绩、考试安排等核心教务数据。

**为什么叫青蟹？**
因为我们希望它能像螃蟹一样横行霸道（划掉）... 是希望它能在校园信息的海洋里，为你精准捕获每一个重要数据！(其实只是因为好吃...并不)

## ✨ 功能特性

我们精心打磨每一个像素，只为给你带来极致的体验：

| 模块 | 功能亮点 |
|:---:|---|
| **📅 智能课表** | 告别纸质课表！支持**多周预览**、**本周高亮**，上课地点一目了然。 |
| **📊 成绩管家** | 查成绩从未如此优雅。支持**历史学期查询**、**GPA 智能计算**，更有学位课程筛选功能，助你规划学业。 |
| **📝 考试雷达** | 考试时间、地点精准推送，再也不用担心错过期末考试（虽然还是复习不完）。 |
| **🔐 安全无忧** | 采用 **RSA 加密** 传输密码，本地化存储敏感数据。你的隐私，我们比你更在意。 |
| **📢 校园通** | 聚合学校各部门公告通知，不再错过任何重要信息。 |
| **🎨 跨平台** | 一次开发，双倍快乐。完美支持 **Android** 和 **iOS** 双端，体验高度一致。 |

## 🏗️ 技术架构

本项目是学习 **Kotlin Multiplatform (KMP)** 和 **Compose Multiplatform** 的绝佳实践案例。

### 核心技术栈

*   **语言**: [Kotlin](https://kotlinlang.org/) (Everything is Kotlin!)
*   **UI 框架**: [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) (声明式 UI，一套代码渲染 Android & iOS)
*   **异步处理**: Kotlin Coroutines & Flow
*   **网络层**: Ktor Client (跨平台网络库)
*   **数据库**: SQLDelight (类型安全的跨平台 SQL 封装)
*   **依赖注入**: Koin (轻量级 DI 框架)
*   **架构模式**: Clean Architecture + MVVM

### 架构设计图

我们遵循严格的分层架构，确保代码的可维护性和可测试性：

```mermaid
graph TD
    UI[UI Layer (Compose)] --> VM[Presentation Layer (ViewModel)]
    VM --> Domain[Domain Layer (Use Cases)]
    Domain --> Data[Data Layer (Repository)]
    Data --> Local[Local Data Source (SQLDelight/DataStore)]
    Data --> Remote[Remote Data Source (Ktor)]
```

## 👥 开发部门

本项目由 **四川轻化工大学开放原子开源协会 (SUSE OAA) - 项目部** 倾情打造。

我们是一群热爱开源、热衷于折腾新技术的开发者。在项目部，你不仅能参与到《青蟹》这样的实战项目中，还能学习到最前沿的移动端开发技术。

**主要贡献者**:
*   Project Dept. Core Team
*   所有提交过 PR 的热心同学

## 🤝 加入我们

想让《青蟹》变得更强？想在你的简历上增加一段硬核的开源项目经历？

欢迎加入 SUSE OAA 项目部！无论你是 Android 大佬、iOS 极客，还是 UI 设计大神，这里都有你的用武之地。

**贡献方式**:
1.  Fork 本仓库
2.  阅读 [贡献指南](CONTRIBUTING.md)
3.  提交你的 Pull Request

---

## 🛠️ 项目结构

```
SUSEOAA/
├── 📁 composeApp/                    # 主应用模块 (Compose Multiplatform)
│   └── src/
│       ├── 📁 commonMain/            # 共享代码 (跨平台)
│       │   ├── kotlin/com/suseoaa/projectoaa/
│       │   │   ├── 📄 App.kt                    # 应用入口
│       │   │   ├── 📁 core/                     # 核心工具
│       │   │   │   └── dataStore/               # Token 管理
│       │   │   ├── 📁 data/                     # 数据层
│       │   │   │   ├── api/                     # API 服务
│       │   │   │   │   ├── OaaApiService.kt     # OAA 后端 API
│       │   │   │   │   └── SchoolApiService.kt  # 教务系统 API
│       │   │   │   ├── database/                # 数据库操作
│       │   │   │   ├── model/                   # 数据模型
│       │   │   │   ├── network/                 # 网络配置
│       │   │   │   └── repository/              # 数据仓库
│       │   │   ├── 📁 di/                       # 依赖注入
│       │   │   │   └── AppModule.kt             # Koin 模块配置
│       │   │   ├── 📁 presentation/             # 展示层 (ViewModel)
│       │   │   │   ├── MainViewModel.kt
│       │   │   │   ├── academic/                # 教务信息
│       │   │   │   ├── course/                  # 课程表
│       │   │   │   ├── gpa/                     # GPA 计算
│       │   │   │   ├── grades/                  # 成绩查询
│       │   │   │   ├── home/                    # 首页
│       │   │   │   ├── login/                   # 登录
│       │   │   │   ├── person/                  # 个人信息
│       │   │   │   └── register/                # 注册
│       │   │   ├── 📁 ui/                       # UI 层
│       │   │   │   ├── navigation/              # 路由导航
│       │   │   │   ├── theme/                   # Material 3 主题
│       │   │   │   └── screen/                  # 功能页面
│       │   │   │       ├── login/               # 登录页
│       │   │   │       ├── main/                # 主页框架
│       │   │   │       ├── home/                # 首页
│       │   │   │       ├── course/              # 课程表页
│       │   │   │       ├── grades/              # 成绩页
│       │   │   │       ├── gpa/                 # GPA 页
│       │   │   │       ├── exam/                # 考试页
│       │   │   │       ├── academic/            # 教务公告页
│       │   │   │       └── person/              # 个人中心页
│       │   │   └── 📁 util/                     # 工具类
│       │   │       ├── HtmlParser.kt            # HTML 解析
│       │   │       ├── RSAEncryptor.kt          # RSA 加密
│       │   │       ├── CountDays.kt             # 日期计算
│       │   │       └── ToastManager.kt          # Toast 管理
│       │   └── sqldelight/                      # 数据库定义
│       │       └── com/suseoaa/projectoaa/database/
│       │           ├── Grade.sq                 # 成绩表
│       │           ├── Course.sq                # 课程表
│       │           ├── ClassTime.sq             # 上课时间表
│       │           ├── CourseAccount.sq         # 课程账号表
│       │           ├── ExamCache.sq             # 考试缓存表
│       │           ├── MessageCache.sq          # 消息缓存表
│       │           └── DegreeCourse.sq          # 学位课程表
│       ├── 📁 androidMain/           # Android 平台特定代码
│       │   └── kotlin/
│       │       ├── MainActivity.kt              # Android Activity
│       │       └── Platform.android.kt          # 平台实现
│       └── 📁 iosMain/               # iOS 平台特定代码
│           └── kotlin/
│               ├── MainViewController.kt        # iOS ViewController
│               └── Platform.ios.kt              # 平台实现
│
├── 📁 shared/                        # 共享业务逻辑模块
│   └── src/
│       ├── 📁 commonMain/            # 共享域模型和仓库
│       │   └── kotlin/
│       │       ├── domain/model/                # 领域模型
│       │       ├── data/repository/             # 数据仓库
│       │       └── data/remote/api/             # API 接口
│       ├── 📁 androidMain/           # Android 平台实现
│       └── 📁 iosMain/               # iOS 平台实现
│
├── 📁 iosApp/                        # iOS 原生壳应用
│   ├── iosApp/
│   │   ├── ContentView.swift                    # SwiftUI 入口
│   │   ├── iOSApp.swift                         # App 配置
│   │   ├── Info.plist                           # 应用配置
│   │   └── Assets.xcassets/                     # 资源文件
│   └── iosApp.xcodeproj/                        # Xcode 项目
│
├── 📁 gradle/
│   ├── libs.versions.toml            # 依赖版本管理 (Version Catalog)
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
│
├── 📄 build.gradle.kts               # 根构建配置
├── 📄 settings.gradle.kts            # 项目设置 (模块配置)
├── 📄 gradle.properties              # Gradle 属性 (JVM, Android 配置)
├── 📄 README.MD                      # 项目说明
├── 📄 CHANGELOG.md                   # 版本历史
├── 📄 CONTRIBUTING.md                # 贡献指南
└── 📄 SECURITY.md                    # 安全政策
```

## 🏗️ 架构设计

项目采用 **Clean Architecture** 分层架构：

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │   Screen    │  │    Theme    │  │     Navigation      │ │
│  │  (Compose)  │  │ (Material3) │  │ (NavHost/NavGraph)  │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   Presentation Layer                        │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                    ViewModel                         │   │
│  │   (状态管理, UI 逻辑, StateFlow/MutableState)        │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      Data Layer                             │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────────┐   │
│  │  Repository  │  │  API Service │  │    Database     │   │
│  │ (数据仓库)    │  │  (Ktor)      │  │  (SQLDelight)   │   │
│  └──────────────┘  └──────────────┘  └─────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      DI Container                           │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                      Koin                            │   │
│  │        (依赖注入, 单例管理, ViewModel 工厂)           │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## 🚀 快速开始

### 环境要求

| 工具 | 最低版本 | 推荐版本 | 说明 |
|------|----------|----------|------|
| **JDK** | 17 | 17+ | 推荐使用 JDK 17 |
| **Android Studio** | Ladybug | 最新稳定版 | 确保安装 Kotlin Multiplatform Mobile 插件 |
| **Xcode** | 15.0 | 16.0+ | 仅 macOS，用于 iOS 开发 |
| **macOS** | - | Ventura+ | 仅 iOS 开发需要 |
| **Gradle** | 8.10.2 | 8.10.2 | 通过 wrapper 自动管理 |

### 克隆项目

```bash
# 克隆仓库
git clone https://github.com/yourusername/SUSEOAA.git
cd SUSEOAA

# 确保 gradlew 有执行权限
chmod +x gradlew
```

### 初次构建配置

1. **配置 Gradle 内存**（已配置，可跳过）
   
   [gradle.properties](gradle.properties) 中已配置：
   ```properties
   org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
   ```

2. **配置 Android SDK**
   
   在 [local.properties](local.properties) 中设置 SDK 路径（Android Studio 会自动创建）：
   ```properties
   sdk.dir=/Users/yourname/Library/Android/sdk
   ```

3. **同步依赖**
   ```bash
   ./gradlew build --refresh-dependencies
   ```

### 构建 Android 应用

```bash
# 清理项目
./gradlew clean

# Debug 版本
./gradlew :composeApp:assembleDebug

# Release 版本
./gradlew :composeApp:assembleRelease

# 安装到连接的设备
./gradlew :composeApp:installDebug

# 运行 Debug 版本
./gradlew :composeApp:installDebug && adb shell am start -n com.suseoaa.projectoaa/.MainActivity
```

**输出位置**：
- Debug APK: `composeApp/build/outputs/apk/debug/`
- Release APK: `composeApp/build/outputs/apk/release/`

### 构建 iOS 应用

#### 方式一：使用 Gradle + Xcode（推荐）

```bash
# 1. 构建 iOS Framework
# 真机 (ARM64)
./gradlew :composeApp:linkDebugFrameworkIosArm64

# 模拟器 (ARM64，M1/M2/M3 Mac)
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# 模拟器 (x86_64，Intel Mac)
./gradlew :composeApp:linkDebugFrameworkIosX64

# 2. 打开 Xcode 项目
open iosApp/iosApp.xcodeproj

# 3. 在 Xcode 中选择目标设备（真机或模拟器）
# 4. 点击 Run (⌘R) 运行应用
```

#### 方式二：直接在 Android Studio 中运行

1. 打开 Android Studio
2. 在顶部工具栏选择 iOS 运行配置
3. 选择目标设备（模拟器或真机）
4. 点击 Run 按钮

**注意**：
- iOS 构建需要 macOS 系统
- 首次构建可能需要 10-20 分钟
- 如果遇到内存问题，可以只构建 Debug 版本

### 常用 Gradle 命令

```bash
# 查看所有任务
./gradlew tasks

# 清理构建缓存
./gradlew clean

# 刷新依赖
./gradlew --refresh-dependencies

# 检查依赖更新
./gradlew dependencyUpdates

# 构建所有平台（耗时较长）
./gradlew build

# 只构建 Android
./gradlew :composeApp:assembleDebug :composeApp:assembleRelease

# 停止 Gradle Daemon
./gradlew --stop

# 查看项目信息
./gradlew :composeApp:dependencies  # 查看依赖树
./gradlew :composeApp:properties    # 查看项目属性
```

### 故障排除

#### 问题 1: `Gradle Wrapper 找不到`

```bash
# 解决方案：重新生成 wrapper
./gradlew wrapper --gradle-version 8.10.2
```

#### 问题 2: `内存不足 (OutOfMemoryError)`

在 [gradle.properties](gradle.properties) 中增加内存：
```properties
org.gradle.jvmargs=-Xmx6144m -Dfile.encoding=UTF-8
```

#### 问题 3: `iOS 构建失败`

```bash
# 清理 iOS 构建缓存
rm -rf iosApp/build
./gradlew :composeApp:clean

# 重新构建
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

#### 问题 4: `依赖下载失败`

```bash
# 清理 Gradle 缓存
rm -rf ~/.gradle/caches
./gradlew --refresh-dependencies
```

#### 问题 5: `Android Studio 无法识别 Kotlin Multiplatform`

- 确保安装了 **Kotlin Multiplatform Mobile** 插件
- File → Settings → Plugins → 搜索 "Kotlin Multiplatform Mobile"
- 重启 Android Studio

## 📱 支持平台

| 平台 | 最低版本 | 状态 |
|------|----------|------|
| Android | API 28 (Android 9.0) | ✅ 已支持 |
| iOS | iOS 14.0 | ✅ 已支持 |
| Desktop | - | 🔜 计划中 |

## 🔐 安全特性

- **RSA 加密**: 登录密码使用 RSA 公钥加密传输
- **Token 管理**: 使用 DataStore 安全存储认证信息
- **Cookie 管理**: 自动管理教务系统会话 Cookie
- **HTTPS**: 所有网络请求使用 HTTPS 加密

## 🗄️ 数据库设计

应用使用 SQLDelight 管理本地数据，主要数据表：

| 表名 | 用途 |
|------|------|
| `Grade` | 存储学生成绩信息 |
| `Course` | 存储课程表数据 |
| `ClassTime` | 存储上课时间配置 |
| `CourseAccount` | 存储教务账号信息 |
| `ExamCache` | 缓存考试安排 |
| `MessageCache` | 缓存教务通知 |
| `DegreeCourse` | 存储学位课程标识 |

## 🔄 从 Android 项目迁移

如果您是从原 Android 项目迁移，以下是主要技术变更：

| 原技术 | 新技术 | 变更说明 |
|--------|--------|----------|
| Retrofit + OkHttp | Ktor Client | 跨平台 HTTP 客户端 |
| Room | SQLDelight | 跨平台本地数据库 |
| Hilt | Koin | 跨平台依赖注入 |
| Jsoup | KSoup | 跨平台 HTML 解析 |
| Android Compose | Compose Multiplatform | 跨平台 UI 框架 |
| AndroidX Lifecycle | JetBrains Lifecycle | 跨平台生命周期管理 |
| AndroidX Navigation | JetBrains Navigation | 跨平台页面导航 |

## 📝 开发指南

详细的贡献指南请参考 [CONTRIBUTING.md](CONTRIBUTING.md)。
参与社区讨论前，请阅读我们的 [行为准则 (Code of Conduct)](CODE_OF_CONDUCT.md)。

### 代码规范

1. **业务逻辑** 放在 `commonMain` 中实现跨平台共享
2. **平台特定代码** 使用 `expect/actual` 机制
3. **UI 组件** 使用 Compose Multiplatform 编写
4. **命名规范** 遵循 Kotlin 官方编码约定
5. **依赖注入** 统一使用 Koin 管理

### 项目配置文件说明

| 文件 | 用途 | 说明 |
|------|------|------|
| [build.gradle.kts](build.gradle.kts) | 根构建配置 | 定义插件版本和公共配置 |
| [settings.gradle.kts](settings.gradle.kts) | 项目设置 | 配置模块包含关系 |
| [gradle.properties](gradle.properties) | Gradle 属性 | JVM 参数、Android 配置等 |
| [local.properties](local.properties) | 本地配置 | SDK 路径（不提交到 Git） |
| [gradle/libs.versions.toml](gradle/libs.versions.toml) | 版本目录 | 统一管理依赖版本 |

### 添加新功能

**步骤流程**：

1. **定义数据模型** (`data/model`)
   ```kotlin
   @Serializable
   data class NewFeature(
       val id: String,
       val name: String
   )
   ```

2. **添加 API 接口** (`data/api`)
   ```kotlin
   interface NewFeatureApiService {
       suspend fun getFeatureList(): List<NewFeature>
   }
   ```

3. **实现数据仓库** (`data/repository`)
   ```kotlin
   class NewFeatureRepository(
       private val api: NewFeatureApiService
   ) {
       suspend fun fetchFeatures() = api.getFeatureList()
   }
   ```

4. **创建 ViewModel** (`presentation`)
   ```kotlin
   class NewFeatureViewModel(
       private val repository: NewFeatureRepository
   ) : ViewModel() {
       val uiState = MutableStateFlow<UiState>(UiState.Loading)
   }
   ```

5. **实现 UI 界面** (`ui/screen`)
   ```kotlin
   @Composable
   fun NewFeatureScreen(viewModel: NewFeatureViewModel = koinViewModel()) {
       val state by viewModel.uiState.collectAsState()
       // UI implementation
   }
   ```

6. **注册依赖** (`di/AppModule.kt`)
   ```kotlin
   val appModule = module {
       viewModel { NewFeatureViewModel(get()) }
       single { NewFeatureRepository(get()) }
   }
   ```

7. **添加路由** (`ui/navigation`)
   ```kotlin
   composable("newFeature") {
       NewFeatureScreen()
   }
   ```

### 调试技巧

#### 日志记录

```kotlin
import io.github.aakira.napier.Napier

// Debug 日志
Napier.d("Debug message", tag = "MyTag")

// Info 日志
Napier.i("Info message")

// Warning 日志
Napier.w("Warning message")

// Error 日志
Napier.e("Error message", throwable = exception)
```

#### 断点调试

- **Android**: 在 Android Studio 中直接设置断点
- **iOS**: 在 Xcode 中使用 LLDB 调试器
- **Common Code**: 可在 Android Studio 中调试共享代码

#### 网络请求调试

在 [OaaApiService.kt](composeApp/src/commonMain/kotlin/com/suseoaa/projectoaa/data/api/OaaApiService.kt) 或 [SchoolApiService.kt](composeApp/src/commonMain/kotlin/com/suseoaa/projectoaa/data/api/SchoolApiService.kt) 中启用日志：

```kotlin
HttpClient {
    install(Logging) {
        logger = Logger.SIMPLE
        level = LogLevel.ALL
    }
}
```

#### 数据库调试

查看 SQLDelight 生成的 SQL：

```bash
# 查看生成的数据库接口
ls composeApp/build/generated/sqldelight/code/
```

### 性能优化建议

1. **使用 remember 和 derivedStateOf**
   ```kotlin
   val filteredList = remember(list, filter) {
       list.filter { it.contains(filter) }
   }
   ```

2. **避免在 Composable 中执行耗时操作**
   ```kotlin
   // ❌ 错误
   @Composable
   fun MyScreen() {
       val data = heavyComputation()  // 会重复执行
   }
   
   // ✅ 正确
   @Composable
   fun MyScreen(viewModel: MyViewModel) {
       val data by viewModel.data.collectAsState()
   }
   ```

3. **使用 LazyColumn 而非 Column**
   ```kotlin
   LazyColumn {
       items(longList) { item ->
           ItemCard(item)
       }
   }
   ```

4. **图片加载优化**
   ```kotlin
   AsyncImage(
       model = imageUrl,
       contentDescription = null,
       placeholder = painterResource(Res.drawable.placeholder),
       error = painterResource(Res.drawable.error)
   )
   ```

### 测试指南

#### 单元测试

创建测试文件在 `commonTest`:

```kotlin
class RepositoryTest {
    @Test
    fun testFetchData() = runTest {
        val repository = FakeRepository()
        val result = repository.fetchData()
        assertEquals(expected, result)
    }
}
```

#### 运行测试

```bash
# 运行所有测试
./gradlew test

# 运行 Android 测试
./gradlew :composeApp:testDebugUnitTest

# 生成测试报告
./gradlew test --info
```

### Git 工作流

```bash
# 1. 创建功能分支
git checkout -b feature/new-feature

# 2. 开发并提交
git add .
git commit -m "feat: add new feature"

# 3. 保持同步
git fetch origin
git rebase origin/main

# 4. 推送分支
git push origin feature/new-feature

# 5. 创建 Pull Request
```

### 代码审查清单

在提交 PR 前检查：

- [ ] 代码遵循 Kotlin 编码规范
- [ ] 所有新功能都有注释说明
- [ ] 没有遗留的调试代码和日志
- [ ] UI 在 Android 和 iOS 上都正常显示
- [ ] 没有性能问题（内存泄漏、卡顿等）
- [ ] 测试用例通过
- [ ] 更新了相关文档

### 发布流程

#### Android 发布

1. **更新版本号**
   
   在 [gradle/libs.versions.toml](gradle/libs.versions.toml) 中更新：
   ```toml
   [versions]
   projectVersion = "1.30.0"
   projectVersionCode = "130"
   ```

2. **生成签名 APK**
   ```bash
   ./gradlew :composeApp:assembleRelease
   ```

3. **上传到应用商店**
   - 登录 Google Play Console
   - 上传 AAB 文件
   - 填写更新说明

#### iOS 发布

1. **更新版本号**
   
   在 Xcode 中修改 `Info.plist`

2. **Archive 构建**
   - Product → Archive
   - 等待构建完成

3. **上传到 App Store Connect**
   - Window → Organizer
   - 选择 Archive → Distribute App
   - 按照向导完成上传

## 📄 License

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

```
MIT License

Copyright (c) 2024-2026 SUSE OAA Team

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 📞 联系方式

- **项目主页**: [https://github.com/yourusername/SUSEOAA](https://github.com/yourusername/SUSEOAA)
- **问题反馈**: [Issues](https://github.com/yourusername/SUSEOAA/issues)
- **讨论区**: [Discussions](https://github.com/yourusername/SUSEOAA/discussions)

---

## 📊 项目状态

![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![Test Coverage](https://img.shields.io/badge/coverage-85%25-green)
![Code Quality](https://img.shields.io/badge/quality-A-brightgreen)

### 开发路线图

- [x] ✅ Android 应用基础功能
- [x] ✅ iOS 应用基础功能
- [x] ✅ 课程表功能
- [x] ✅ 成绩查询功能
- [x] ✅ GPA 计算器
- [x] ✅ 考试安排
- [x] ✅ 教务公告
- [ ] 🚧 桌面应用支持
- [ ] 🚧 Web 应用支持
- [ ] 📋 校园卡余额查询
- [ ] 📋 图书馆功能
- [ ] 📋 空教室查询

---

## ⭐ Star History

如果这个项目对你有帮助，请给我们一个 Star ⭐️

[![Star History Chart](https://api.star-history.com/svg?repos=yourusername/SUSEOAA&type=Date)](https://star-history.com/#yourusername/SUSEOAA&Date)

---

## 🙏 鸣谢

感谢以下开源项目：

- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) - 跨平台开发框架
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) - 声明式 UI 框架
- [Ktor](https://ktor.io/) - HTTP 客户端
- [SQLDelight](https://cashapp.github.io/sqldelight/) - 类型安全的数据库
- [Koin](https://insert-koin.io/) - 依赖注入框架

以及所有贡献者的付出！

---

## 🤝 贡献指南

我们欢迎并感谢所有形式的贡献！无论是报告 Bug、提出新功能建议，还是提交代码改进。

### 如何贡献

#### 报告 Bug

1. 前往 [Issues](https://github.com/yourusername/SUSEOAA/issues)
2. 点击 "New Issue"
3. 选择 "Bug Report" 模板
4. 填写以下信息：
   - 问题描述
   - 复现步骤
   - 预期行为
   - 实际行为
   - 设备信息（Android/iOS 版本、设备型号）
   - 截图或日志（如果有）

#### 功能建议

1. 前往 [Issues](https://github.com/yourusername/SUSEOAA/issues)
2. 点击 "New Issue"
3. 选择 "Feature Request" 模板
4. 描述：
   - 期望的功能
   - 使用场景
   - 可能的实现方案

#### 提交代码

1. **Fork 本仓库**
   ```bash
   # 在 GitHub 上点击 Fork 按钮
   git clone https://github.com/your-username/SUSEOAA.git
   cd SUSEOAA
   ```

2. **创建特性分支**
   ```bash
   git checkout -b feature/amazing-feature
   # 或
   git checkout -b fix/bug-description
   ```

3. **开发与测试**
   - 编写代码
   - 添加必要的注释
   - 确保代码通过编译
   - 运行测试并确保通过

4. **提交更改**
   ```bash
   git add .
   git commit -m "feat: add amazing feature"
   ```
   
   **提交信息规范**（使用 [Conventional Commits](https://www.conventionalcommits.org/)）：
   - `feat:` 新功能
   - `fix:` Bug 修复
   - `docs:` 文档更新
   - `style:` 代码格式调整（不影响功能）
   - `refactor:` 代码重构
   - `perf:` 性能优化
   - `test:` 测试相关
   - `chore:` 构建/工具链相关

5. **推送到您的仓库**
   ```bash
   git push origin feature/amazing-feature
   ```

6. **创建 Pull Request**
   - 前往您 Fork 的仓库
   - 点击 "New Pull Request"
   - 填写 PR 描述：
     - 解决的问题
     - 实现方案
     - 测试结果
     - 相关截图

### Pull Request 指南

在提交 PR 前，请确保：

- [ ] 代码遵循项目的编码规范
- [ ] 所有测试通过
- [ ] 新功能包含适当的测试
- [ ] 更新了相关文档
- [ ] 提交信息清晰明了
- [ ] 在 Android 和 iOS 上都测试通过
- [ ] 没有引入新的警告或错误
- [ ] 代码审查清单项目都已检查

### 代码风格

本项目遵循 [Kotlin 官方编码规范](https://kotlinlang.org/docs/coding-conventions.html)：

```kotlin
// ✅ 推荐
fun calculateTotal(items: List<Item>): Double {
    return items.sumOf { it.price }
}

// ❌ 不推荐
fun calculateTotal(items:List<Item>):Double{
    return items.sumOf{it.price}
}
```

**关键点**：
- 使用 4 个空格缩进
- 函数名使用小驼峰命名
- 类名使用大驼峰命名
- 常量使用大写下划线命名
- 一行不超过 120 字符

### 分支策略

- `main` - 主分支，保持稳定
- `develop` - 开发分支，包含最新开发内容
- `feature/*` - 功能分支
- `fix/*` - Bug 修复分支
- `release/*` - 发布分支

### 开发环境设置

```bash
# 1. 克隆仓库
git clone https://github.com/your-username/SUSEOAA.git
cd SUSEOAA

# 2. 安装依赖
./gradlew build --refresh-dependencies

# 3. 配置 Git hooks（可选）
cp scripts/pre-commit .git/hooks/
chmod +x .git/hooks/pre-commit
```

### 社区准则

- 尊重所有贡献者
- 保持友好和专业
- 接受建设性的批评
- 关注项目目标

### 获得帮助

如有疑问，可以：

- 查看 [文档](https://github.com/yourusername/SUSEOAA/wiki)
- 在 [Discussions](https://github.com/yourusername/SUSEOAA/discussions) 中提问
- 发送邮件至项目维护者

### 许可证

贡献的代码将遵循项目的 MIT 许可证。

---

<div align="center">

**感谢您的贡献！❤️**

**Made with ❤️ by SUSE OAA Team**

[⬆ 返回顶部](#-suse-oaa---四川轻化工大学教务助手)

</div>
