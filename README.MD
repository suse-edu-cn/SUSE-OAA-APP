# ProjectOAA - Kotlin Multiplatform

这是一个使用 **Kotlin Multiplatform (KMP)** 和 **Compose Multiplatform** 构建的跨平台教务系统应用。

## 🏗️ 项目架构

```
ProjectOAA/
├── shared/                    # 共享业务逻辑模块
│   ├── commonMain/           # 通用代码
│   │   ├── network/          # Ktor 网络层
│   │   ├── database/         # SQLDelight 数据库
│   │   ├── datastore/        # DataStore 偏好设置
│   │   ├── di/               # Koin 依赖注入
│   │   └── util/             # 工具类
│   ├── androidMain/          # Android 特定实现
│   └── iosMain/              # iOS 特定实现
│
├── composeApp/               # Compose Multiplatform UI 模块
│   ├── commonMain/           # 共享 UI 代码
│   │   ├── ui/
│   │   │   ├── theme/        # Material 3 主题
│   │   │   ├── navigation/   # 导航
│   │   │   └── feature/      # 功能模块 UI
│   ├── androidMain/          # Android 入口
│   └── iosMain/              # iOS 入口
│
└── iosApp/                   # iOS 原生壳应用
```

## 🛠️ 技术栈

| 功能 | 技术方案 | 版本 |
|------|----------|------|
| **UI 框架** | Compose Multiplatform | 1.7.3 |
| **网络请求** | Ktor Client | 3.0.2 |
| **本地数据库** | SQLDelight | 2.0.2 |
| **依赖注入** | Koin | 4.0.0 |
| **偏好存储** | DataStore Multiplatform | 1.1.2 |
| **序列化** | Kotlinx Serialization | 1.7.3 |
| **日期时间** | Kotlinx DateTime | 0.6.1 |
| **图片加载** | Coil 3 (KMP) | 3.0.4 |
| **日志** | Napier | 2.7.1 |
| **导航** | JetBrains Navigation | 2.8.0-alpha10 |
| **生命周期** | JetBrains Lifecycle | 2.8.3 |

## 📱 支持平台

- ✅ Android (API 28+)
- ✅ iOS (iOS 14+)
- 🔜 Desktop (可选扩展)

## 🚀 快速开始

### 环境要求

- JDK 11+
- Android Studio Ladybug 或更高版本
- Xcode 15+ (iOS 开发)
- Kotlin 2.1.0+

### 构建运行

#### Android

```bash
./gradlew :composeApp:assembleDebug
```

或在 Android Studio 中直接运行 `composeApp` 模块。

#### iOS

1. 首先构建 shared framework:
```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

2. 用 Xcode 打开 `iosApp/iosApp.xcodeproj`

3. 运行项目

### 同步 Gradle

```bash
./gradlew --refresh-dependencies
```

## 📁 模块说明

### shared 模块

共享的业务逻辑层，包含：

- **网络层**: 基于 Ktor Client 的 HTTP 客户端
- **数据库**: 基于 SQLDelight 的本地持久化
- **偏好设置**: 基于 DataStore 的键值存储
- **依赖注入**: 基于 Koin 的 DI 容器

### composeApp 模块

共享的 UI 层，使用 Compose Multiplatform：

- **主题**: Material 3 设计规范
- **导航**: Jetpack Navigation Compose
- **功能屏幕**: 登录、首页、课程表、成绩查询等

## 🔄 迁移指南

从原 Android 项目迁移的主要变更：

| 原技术 | 新技术 | 说明 |
|--------|--------|------|
| Retrofit + OkHttp | Ktor Client | 跨平台 HTTP 客户端 |
| Room | SQLDelight | 跨平台数据库 |
| Hilt | Koin | 跨平台依赖注入 |
| Android Compose | Compose Multiplatform | 跨平台 UI |
| AndroidX Lifecycle | JetBrains Lifecycle | 跨平台生命周期 |
| AndroidX Navigation | JetBrains Navigation | 跨平台导航 |

## 📝 开发规范

1. 业务逻辑放在 `shared` 模块的 `commonMain` 中
2. 平台特定代码使用 `expect/actual` 机制
3. UI 代码放在 `composeApp` 模块的 `commonMain` 中
4. 遵循 Clean Architecture 分层原则

## 📄 License

MIT License

## 🤝 Contributing

欢迎提交 Issue 和 Pull Request！
