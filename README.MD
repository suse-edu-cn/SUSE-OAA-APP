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
![Compose Multiplatform](https://img.shields.io/badge/UI-Compose%20Multiplatform%201.7.3-4285F4?logo=jetpackcompose&logoColor=white)
![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20iOS-green)
![Ktor](https://img.shields.io/badge/Ktor-3.0.2-087CFA?logo=ktor&logoColor=white)
![SQLDelight](https://img.shields.io/badge/SQLDelight-2.0.2-blue)
![Koin](https://img.shields.io/badge/Koin-4.0.0-orange)
![Development](https://img.shields.io/badge/Dev-Project%20Dept.-ff69b4)
![License](https://img.shields.io/badge/License-MIT-blue)

[功能特性](#-功能特性) • [技术架构](#-技术架构) • [项目结构](#-项目结构) • [技术栈](#-技术栈详解) • [快速开始](#-快速开始) • [加入我们](#-加入我们)

</div>

---

## 🌊 关于青蟹

**青蟹 (Project OAA)** 是由 **四川轻化工大学开放原子开源协会 (SUSE OAA)** 项目部自主研发的跨平台教务辅助应用。基于 **Kotlin Multiplatform (KMP)** 构建，一套代码同时支持 Android 和 iOS，是学习 KMP + Compose Multiplatform 的完整实战案例。

应用通过对接学校教务系统和 OAA 后端服务，聚合了课表、成绩、考试、GPA 计算、教学计划、校园公告、打卡签到等核心教务功能，用现代化的 UI 和流畅的交互体验，替代繁琐的网页操作。

## ✨ 功能特性

| 模块 | 功能亮点 |
|:---:|---|
| **📅 智能课表** | 多周预览、本周高亮、自定义课程、上课地点一目了然，支持本地 SQLDelight 离线缓存 |
| **📊 成绩查询** | 历史学期成绩查询，含平时/实验/期末分项成绩，支持学位课程筛选 |
| **🎓 GPA 计算** | 智能绩点计算器，自动匹配学位课程，助你精准规划学业 |
| **📝 考试安排** | 考试时间、地点、倒计时精准展示，再也不用担心错过考试 |
| **📖 教学计划** | 培养方案查看、课程信息检索、学业完成度跟踪 |
| **📢 校园公告** | 聚合各院系部门公告通知，支持按部门筛选 |
| **✅ 打卡签到** | 652 打卡系统集成，支持密码登录和扫码签到两种方式 |
| **🔐 安全加密** | RSA 加密传输教务密码，DataStore 安全存储 Token，HTTPS 全链路加密 |
| **🤖 验证码 OCR** | Android 端集成 ML Kit + ONNX Runtime，iOS 端集成 ONNX + Vision Framework，自动识别登录验证码 |
| **🎨 跨平台** | Compose Multiplatform 一套 UI 代码，Android / iOS 体验高度一致 |

## 🏗️ 技术架构

项目采用 **Clean Architecture + MVVM** 分层架构，严格遵循 **关注点分离** 原则。代码按照职责拆分为两个 Gradle 模块：

```
┌─────────────────────────── composeApp ───────────────────────────┐
│                                                                  │
│  ┌────────────────────── UI Layer ──────────────────────┐        │
│  │  Screen (Compose)  ·  Theme (Material 3)  ·  Navigation      │
│  └──────────────────────────────────────────────────────┘        │
│                              │                                   │
│  ┌──────────────── Presentation Layer ──────────────────┐        │
│  │  ViewModel (StateFlow / MutableState)                │        │
│  │  17 个 ViewModel 覆盖全部功能模块                      │        │
│  └──────────────────────────────────────────────────────┘        │
│                              │                                   │
│  ┌───── DI ─────┐   ┌─── Platform ───┐                          │
│  │  AppModule   │   │ AppUpdate Repo │                          │
│  │ (ViewModels) │   │ CaptchaOCR     │                          │
│  └──────────────┘   └────────────────┘                          │
└─────────────────────────────┬────────────────────────────────────┘
                              │ depends on
┌─────────────────────────── shared ───────────────────────────────┐
│                                                                  │
│  ┌──────────────────── Domain Layer ────────────────────┐        │
│  │  Model (领域模型 · @Serializable 数据类)              │        │
│  │  course / grades / exam / checkin / person / ...      │        │
│  └──────────────────────────────────────────────────────┘        │
│                              │                                   │
│  ┌──────────────────── Data Layer ──────────────────────┐        │
│  │  Repository (17个) ── API Service (4个) ── Database   │        │
│  │  Ktor HttpClient ×5 · SQLDelight · DataStore          │        │
│  └──────────────────────────────────────────────────────┘        │
│                              │                                   │
│  ┌───── DI ──────┐   ┌─── Platform ────┐                        │
│  │ SharedModule  │   │ DB Driver       │                        │
│  │ (Data Layer)  │   │ DataStore       │                        │
│  └───────────────┘   │ TokenManager    │                        │
│                      │ RSAEncryptor    │                        │
│                      └─────────────────┘                        │
└──────────────────────────────────────────────────────────────────┘
```

### 模块职责

| 模块 | 职责 | 关键内容 |
|------|------|----------|
| **shared** | 全部数据层 + 领域模型 | Model、API Service、Repository、Database、TokenManager、工具类 |
| **composeApp** | UI + Presentation | Screen、ViewModel、Navigation、Theme、平台特定 UI 工具 |
| **iosApp** | iOS 原生壳 | SwiftUI 入口、验证码 OCR（ONNX + Vision Framework） |

### 数据流

```
用户操作 → Screen (Compose UI)
             → ViewModel (状态管理)
               → Repository (数据编排)
                 ├── API Service (Ktor) → 远程服务器
                 └── Database (SQLDelight) / DataStore → 本地存储
               ← Flow/StateFlow 响应式更新
             ← UI 自动重组
```

### 依赖注入结构

应用使用 **Koin 4.0** 管理依赖，按职责拆分为 4 个 Module：

| Module | 位置 | 注册内容 |
|--------|------|----------|
| **SharedModule** | `shared` | Json、5 个 HttpClient、4 个 API Service、CourseDatabase、2 个 CookieStorage、15+ 个 Repository |
| **Shared PlatformModule** | `shared` (expect/actual) | CourseDatabaseDriverFactory、DataStore、TokenManager |
| **AppModule** | `composeApp` | 17 个 ViewModel |
| **App PlatformModule** | `composeApp` (expect/actual) | AppUpdateRepository（平台特定版本号获取） |

## 📐 项目结构

```
SUSEOAA/
├── 📁 composeApp/                          # 应用模块 (UI + Presentation)
│   └── src/
│       ├── commonMain/kotlin/.../projectoaa/
│       │   ├── 📄 App.kt                  # Compose 应用根组件
│       │   ├── 📁 di/                     # 依赖注入
│       │   │   ├── AppModule.kt           # ViewModel DI 注册
│       │   │   └── PlatformModule.kt      # 平台特定 DI (expect)
│       │   ├── 📁 presentation/           # ViewModel 层 (17个)
│       │   │   ├── MainViewModel.kt
│       │   │   ├── login/LoginViewModel.kt
│       │   │   ├── course/CourseViewModel.kt
│       │   │   ├── grades/GradesViewModel.kt
│       │   │   ├── gpa/GpaViewModel.kt
│       │   │   ├── exam/ExamViewModel.kt
│       │   │   ├── home/HomeViewModel.kt
│       │   │   ├── academic/AcademicViewModel.kt
│       │   │   ├── person/PersonViewModel.kt
│       │   │   ├── checkin/CheckinViewModel.kt
│       │   │   └── ...
│       │   ├── 📁 ui/
│       │   │   ├── navigation/            # 导航路由 (NavHost)
│       │   │   ├── theme/                 # Material 3 主题
│       │   │   ├── components/            # 通用 UI 组件
│       │   │   └── screen/               # 功能页面 (13个)
│       │   │       ├── login/             # 登录
│       │   │       ├── register/          # 注册
│       │   │       ├── main/              # 主页容器 (底部导航)
│       │   │       ├── home/              # 首页 + 院系公告详情
│       │   │       ├── academic/          # 学业总览
│       │   │       ├── person/            # 个人中心
│       │   │       ├── course/            # 课程表 (在 academic 内)
│       │   │       ├── grades/            # 成绩查询
│       │   │       ├── gpa/               # GPA 计算
│       │   │       ├── exam/              # 考试安排
│       │   │       ├── teachingplan/      # 教学计划 (3个子页面)
│       │   │       ├── checkin/           # 打卡签到
│       │   │       ├── changepassword/    # 修改密码
│       │   │       └── recruitment/       # 招聘/转专业
│       │   └── 📁 util/                  # UI 工具类
│       │       ├── PlatformCaptchaOcr.kt  # 验证码 OCR (expect)
│       │       ├── ExamCountDown.kt       # 考试倒计时 (含 Color)
│       │       ├── ToastManager.kt        # Toast 管理
│       │       └── AdaptiveLayout.kt      # 自适应布局
│       ├── androidMain/                   # Android 平台实现
│       │   └── OaaApplication.kt          # Application + Koin 初始化
│       └── iosMain/                       # iOS 平台实现
│           └── MainViewController.kt      # iOS 入口 + Koin 初始化
│
├── 📁 shared/                             # 共享数据层模块
│   └── src/
│       ├── commonMain/kotlin/.../shared/
│       │   ├── 📁 di/                    # 数据层 DI
│       │   │   ├── SharedModule.kt       # 全部数据层依赖注册
│       │   │   └── PlatformModule.kt     # 平台 DI (expect)
│       │   ├── 📁 domain/model/          # 领域模型
│       │   │   ├── course/               # 课程模型
│       │   │   ├── grades/               # 成绩模型
│       │   │   ├── exam/                 # 考试模型
│       │   │   ├── checkin/              # 签到模型
│       │   │   ├── person/               # 个人信息模型
│       │   │   ├── school/               # 教务 API 响应模型
│       │   │   ├── teachingplan/         # 教学计划模型
│       │   │   └── ...
│       │   ├── 📁 data/
│       │   │   ├── api/                  # API 服务 (4个)
│       │   │   │   ├── OaaApiService.kt          # OAA 后端
│       │   │   │   ├── SchoolApiService.kt       # 教务系统
│       │   │   │   ├── CheckinApiService.kt      # 打卡系统
│       │   │   │   └── QrCodeCheckinApiService.kt # 扫码签到
│       │   │   ├── network/              # 网络配置
│       │   │   │   ├── HttpClientFactory.kt      # 5个 HttpClient 实例
│       │   │   │   └── ClearableCookieStorage.kt # Cookie 管理
│       │   │   ├── repository/           # 数据仓库 (17个)
│       │   │   │   ├── OaaAuthRepository.kt
│       │   │   │   ├── SchoolAuthRepository.kt
│       │   │   │   ├── SchoolCourseRepository.kt
│       │   │   │   ├── SchoolGradeRepository.kt
│       │   │   │   ├── LocalCourseRepository.kt
│       │   │   │   ├── CheckinRepository.kt
│       │   │   │   ├── GpaRepository.kt
│       │   │   │   └── ...
│       │   │   └── local/                # 本地存储
│       │   │       ├── TokenManager.kt           # Token/偏好 (DataStore)
│       │   │       └── database/                 # DB Driver (expect)
│       │   └── 📁 util/                  # 共享工具类
│       │       ├── RSAEncryptor.kt       # RSA 加密 (expect/actual)
│       │       ├── HtmlParser.kt         # HTML 解析
│       │       └── CountDays.kt          # 日期/学期计算
│       ├── commonMain/sqldelight/        # 数据库定义 (8张表)
│       │   ├── Course.sq                 # 课程表
│       │   ├── ClassTime.sq              # 上课时间表
│       │   ├── Grade.sq                  # 成绩表
│       │   ├── CourseAccount.sq          # 教务账号表
│       │   ├── ExamCache.sq              # 考试缓存表
│       │   ├── MessageCache.sq           # 消息缓存表
│       │   ├── DegreeCourse.sq           # 学位课程表
│       │   ├── CheckinAccount.sq         # 签到账号表
│       │   └── migrations/              # 数据库迁移 (1.sqm, 2.sqm)
│       ├── androidMain/                  # Android expect 实现
│       └── iosMain/                      # iOS expect 实现
│
├── 📁 iosApp/                            # iOS 原生壳应用
│   └── iosApp/
│       ├── ContentView.swift             # SwiftUI 入口
│       ├── iOSApp.swift                  # App 配置
│       ├── CaptchaOCR.swift              # 验证码 OCR 入口
│       ├── DdddOcrRecognizer.swift       # ddddocr ONNX 推理
│       ├── CaptchaOCRBridge.h/.m         # ObjC 桥接 (供 KMP 调用)
│       └── common_old.onnx              # ONNX 验证码模型
│
├── 📁 gradle/
│   └── libs.versions.toml               # 统一版本管理 (Version Catalog)
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 🔧 技术栈详解

### 核心框架

| 技术 | 版本 | 用途 |
|------|------|------|
| **Kotlin** | 2.1.0 | 开发语言，100% Kotlin |
| **Compose Multiplatform** | 1.7.3 | 声明式 UI 框架，一套代码渲染 Android & iOS |
| **Kotlin Coroutines** | 1.9.0 | 异步并发，配合 Flow 实现响应式数据流 |
| **Gradle** | 8.10.2 | 构建系统，使用 Version Catalog 管理依赖 |

### 网络层

| 技术 | 版本 | 用途 |
|------|------|------|
| **Ktor Client** | 3.0.2 | 跨平台 HTTP 客户端 |
| **kotlinx-serialization** | 1.7.3 | JSON 序列化/反序列化 |

项目配置了 **5 个独立 HttpClient 实例**，通过 Koin `named()` 限定符隔离不同服务的会话和 Cookie：

| Client | 用途 | 特性 |
|--------|------|------|
| `github` | GitHub API (检查更新) | 无认证 |
| `oaa` | OAA 后端 API | Bearer Token 自动注入 |
| `school` | 教务系统 | Session Cookie 管理 |
| `checkin` | 652 打卡系统 | 独立 ClearableCookieStorage |
| `qrCheckin` | 扫码签到 | 独立 CookieStorage 隔离 |

### 数据持久化

| 技术 | 版本 | 用途 |
|------|------|------|
| **SQLDelight** | 2.0.2 | 类型安全的跨平台 SQL 数据库，8 张数据表 |
| **DataStore** | 1.1.2 | 键值对存储 (Token、用户偏好) |

**数据库表结构**：

| 表名 | 用途 |
|------|------|
| `Course` | 课程主表（支持自定义课程） |
| `ClassTime` | 上课时间配置（外键关联 Course） |
| `Grade` | 成绩缓存（含平时/实验/期末分项） |
| `CourseAccount` | 教务系统账号存储 |
| `ExamCache` | 考试安排缓存 |
| `MessageCache` | 调课通知消息缓存 |
| `DegreeCourse` | 学位课程标记（GPA 筛选用） |
| `CheckinAccount` | 签到账号（密码/扫码双模式） |

### 依赖注入 & 架构

| 技术 | 版本 | 用途 |
|------|------|------|
| **Koin** | 4.0.0 | 轻量级 DI，支持 KMP + Compose ViewModel 注入 |
| **Lifecycle ViewModel** | 2.8.3 | 跨平台 ViewModel 生命周期管理 |
| **Navigation Compose** | 2.8.0-alpha10 | 跨平台页面导航 |

### UI & 多媒体

| 技术 | 版本 | 用途 |
|------|------|------|
| **Material 3** | (Compose 内置) | 设计系统 + 动态主题 |
| **Coil** | 3.0.4 | 跨平台图片异步加载 (Ktor 网络后端) |
| **Markdown Renderer** | 0.28.0 | Markdown 富文本渲染 |
| **Window Size Class** | 0.5.0 | 自适应布局（手机/平板适配） |

### 安全 & 加密

| 技术 | 版本 | 用途 |
|------|------|------|
| **cryptography-core** | 0.4.0 | 跨平台 RSA 加密 (JDK / Apple provider) |
| **KSoup** | 0.1.9 | 跨平台 HTML 解析（替代 Jsoup） |

### AI & OCR

| 技术 | 平台 | 用途 |
|------|------|------|
| **ML Kit Text Recognition** | Android | 验证码识别 (含中文模型) |
| **ONNX Runtime** | Android 1.22.0 / iOS | ddddocr 验证码模型推理 |
| **Vision Framework** | iOS (原生) | 备选 OCR 方案 |

### 日志

| 技术 | 版本 | 用途 |
|------|------|------|
| **Napier** | 2.7.1 | 跨平台日志框架 |

## 🗺️ 页面导航

```
                    ┌─────────┐
                    │  Login  │◄──────────────┐
                    └────┬────┘               │
                         │                    │
                    ┌────▼────┐         ┌─────┴──────────┐
              ┌─────┤  Main   ├─────┐   │ ChangePassword │
              │     └────┬────┘     │   └────────────────┘
              │          │          │
        ┌─────▼──┐  ┌────▼───┐  ┌──▼──────┐
        │  Home  │  │Academic│  │ Person  │
        │  (Tab) │  │ (Tab)  │  │  (Tab)  │
        └───┬────┘  └───┬────┘  └─────────┘
            │           │
   ┌────────┼─────┐   ┌─┼──────────┬──────────┐
   │        │     │   │ │          │          │
┌──▼───┐ ┌──▼──┐  │ ┌▼─▼──┐  ┌────▼──┐  ┌───▼────┐
│Dept. │ │Exam │  │ │Grade│  │  GPA  │  │Teaching│
│Detail│ │Info │  │ │     │  │       │  │  Plan  │
└──────┘ └─────┘  │ └─────┘  └───────┘  └────────┘
                  │
           ┌──────▼──────┐
           │  Checkin     │
           │ (隐藏功能)    │
           └──────────────┘
```

**底部导航 Tab**：首页 (Home) · 学业 (Academic) · 我的 (Person)

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

## 🚀 快速开始

### 环境要求

| 工具 | 最低版本 | 推荐版本 | 说明 |
|------|----------|----------|------|
| **JDK** | 17 | 17+ | 推荐使用 JDK 17 |
| **Android Studio** | Ladybug | 最新稳定版 | 安装 Kotlin Multiplatform 插件 |
| **Xcode** | 15.0 | 16.0+ | 仅 macOS，用于 iOS 开发 |
| **Gradle** | 8.10.2 | 8.10.2 | 通过 wrapper 自动管理 |

### 克隆与构建

```bash
git clone https://github.com/yourusername/SUSEOAA.git
cd SUSEOAA
chmod +x gradlew

# Android Debug 构建
./gradlew :composeApp:assembleDebug

# iOS 模拟器构建 (Apple Silicon Mac)
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
open iosApp/iosApp.xcodeproj   # 在 Xcode 中 Run (⌘R)
```

**APK 输出**：`composeApp/build/outputs/apk/debug/`

### 常用命令

```bash
./gradlew :composeApp:installDebug          # 安装到 Android 设备
./gradlew :composeApp:assembleRelease       # Release 构建
./gradlew clean                             # 清理
./gradlew --refresh-dependencies            # 刷新依赖
```

## 📱 支持平台

| 平台 | 最低版本 | 状态 |
|------|----------|------|
| Android | API 28 (Android 9.0) | ✅ 已支持 |
| iOS | iOS 14.0 | ✅ 已支持 |
| Desktop | - | 🔜 计划中 |

## 🔐 安全特性

- **RSA 加密**：登录密码使用 RSA 公钥加密传输（跨平台 expect/actual 实现）
- **Token 管理**：使用 DataStore 安全存储认证 Token
- **Cookie 隔离**：教务系统、打卡系统、扫码签到各自独立的 CookieStorage，互不干扰
- **HTTPS**：所有网络请求使用 HTTPS 加密

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

1. **数据层代码**（Model、Repository、API）放在 `shared` 模块的 `commonMain` 中
2. **UI/ViewModel 代码** 放在 `composeApp` 模块
3. **平台特定代码** 使用 `expect/actual` 机制
4. **依赖注入**：数据层注册在 `SharedModule`，ViewModel 注册在 `AppModule`
5. **命名规范** 遵循 [Kotlin 官方编码约定](https://kotlinlang.org/docs/coding-conventions.html)

### 添加新功能步骤

1. **在 `shared` 中定义数据模型** (`domain/model/`)

   ```kotlin
   @Serializable
   data class NewFeature(val id: String, val name: String)
   ```

2. **在 `shared` 中添加 API 接口** (`data/api/`)

   ```kotlin
   class NewFeatureApiService(private val client: HttpClient) {
       suspend fun getList(): List<NewFeature> = client.get("...").body()
   }
   ```

3. **在 `shared` 中实现 Repository** (`data/repository/`)

   ```kotlin
   class NewFeatureRepository(private val api: NewFeatureApiService) {
       suspend fun fetchFeatures() = api.getList()
   }
   ```

4. **在 `SharedModule` 中注册 DI**

   ```kotlin
   single { NewFeatureApiService(get(named("oaa"))) }
   single { NewFeatureRepository(get()) }
   ```

5. **在 `composeApp` 中创建 ViewModel** (`presentation/`)

   ```kotlin
   class NewFeatureViewModel(private val repo: NewFeatureRepository) : ViewModel() {
       val uiState = MutableStateFlow<UiState>(UiState.Loading)
   }
   ```

6. **在 `AppModule` 中注册 ViewModel**

   ```kotlin
   viewModel { NewFeatureViewModel(get()) }
   ```

7. **实现 UI 并添加路由** (`ui/screen/` + `ui/navigation/`)

   ```kotlin
   @Composable
   fun NewFeatureScreen(viewModel: NewFeatureViewModel = koinViewModel()) {
       val state by viewModel.uiState.collectAsState()
       // ...
   }
   ```

## 📊 项目状态

### 开发路线图

- [x] Android / iOS 双端基础功能
- [x] 智能课表（多周预览、自定义课程）
- [x] 成绩查询 + GPA 计算器
- [x] 考试安排 + 倒计时
- [x] 教务公告聚合
- [x] 教学计划查看
- [x] 打卡签到（密码/扫码双模式）
- [x] 验证码自动识别 (ML Kit / ONNX / Vision)
- [x] shared 模块架构重构（Clean Architecture）
- [ ] 桌面应用支持
- [ ] 空教室查询
- [ ] 校园卡功能

## 🙏 鸣谢

- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) - 跨平台开发框架
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) - 声明式 UI 框架
- [Ktor](https://ktor.io/) - HTTP 客户端
- [SQLDelight](https://cashapp.github.io/sqldelight/) - 类型安全的数据库
- [Koin](https://insert-koin.io/) - 依赖注入框架
- [Coil](https://coil-kt.github.io/coil/) - 图片加载
- [Napier](https://github.com/AAkira/Napier) - 跨平台日志
- [ddddocr](https://github.com/sml2h3/ddddocr) - 验证码识别模型

## 📄 License

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

---

<div align="center">

**Made with ❤️ by SUSE OAA Team**

[⬆ 返回顶部](#-青蟹-project-oaa)

</div>
