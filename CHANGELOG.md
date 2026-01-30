# Changelog

所有值得注意的项目更改都将记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
项目遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [未发布]

### 计划中
- 桌面应用支持 (Windows, macOS, Linux)
- Web 应用支持
- 校园卡余额查询
- 图书馆功能集成
- 空教室查询
- 消息推送功能

## [1.30.0] - 2026-01-31

### 变更
- 🧹 **项目清理**: 移除旧的原生 Android `app` 模块
- 📁 完成从原生 Android 到 KMP 的完全迁移
- 📝 更新项目结构文档

### 移除
- ❌ 删除 `app/` 目录（128 个旧文件）
- ❌ 删除构建日志文件
- ❌ 移除对 Hilt、Retrofit、Room 的依赖

### 当前结构
- `composeApp/` - 主应用模块 (Compose Multiplatform)
- `shared/` - 共享业务逻辑模块
- `iosApp/` - iOS 原生壳应用

## [1.29.12] - 2026-01-30

### 新增
- 📝 完善的项目文档和 README
- 🔧 优化的 .gitignore 配置
- 🐛 修复 iOS Release 构建内存问题

### 变更
- ⚡️ 将 Gradle JVM 内存从 2GB 提升至 4GB
- 📦 更新依赖管理配置

### 修复
- 🔨 修复 Gradle Wrapper 缺失问题
- 🛠️ 修复 iOS Release Framework 链接失败

## [1.29.0] - 2026-01

### 新增
- ✨ 迁移到 Kotlin Multiplatform 架构
- 🍎 支持 iOS 平台
- 📱 Compose Multiplatform UI 框架
- 🔄 跨平台代码共享
- 🗄️ SQLDelight 数据库
- 🌐 Ktor 网络请求
- 💉 Koin 依赖注入

### 变更
- 🔄 从 Retrofit 迁移到 Ktor Client
- 🔄 从 Room 迁移到 SQLDelight
- 🔄 从 Hilt 迁移到 Koin
- 🔄 从 Jsoup 迁移到 KSoup
- 🎨 使用 Material 3 设计系统

### 移除
- ❌ 移除 Android 特定依赖
- ❌ 移除 Jetpack Compose 依赖（替换为 Compose Multiplatform）

## [1.0.0] - 2024

### 新增
- 🎓 基础 Android 应用
- 🔐 教务系统登录
- 📅 课程表查询
- 📊 成绩查询
- 🧮 GPA 计算器
- 📝 考试安排查询
- 📢 教务公告
- 👤 个人信息管理
- 🔑 密码修改功能

---

## 版本说明

### [未发布]
正在开发中的功能，尚未发布

### [主版本号.次版本号.修订号]
- **主版本号**: 不兼容的 API 修改
- **次版本号**: 向下兼容的功能性新增
- **修订号**: 向下兼容的问题修正

### 变更类型
- `新增` - 新功能
- `变更` - 现有功能的变更
- `弃用` - 即将移除的功能
- `移除` - 已移除的功能
- `修复` - Bug 修复
- `安全` - 安全相关的修复
