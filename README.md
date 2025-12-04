# 青蟹APP

**青蟹APP** 是一个采用现代 Android 技术栈（Jetpack Compose + MVVM + Hilt + Clean Architecture）构建的协会服务应用。


![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blue.svg)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-green.svg)
![Hilt](https://img.shields.io/badge/DI-Hilt-orange.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)



-----

## Mock Server 编译说明

**源码目录：** `/mock_server`

### 1\. Windows 环境

使用 GCC 编译（需链接 `ws2_32` 和 `iphlpapi` 库）：

```bash
gcc mock_server.c -o mock_server.exe -O2 -lws2_32 -liphlpapi -finput-charset=UTF-8 -fexec-charset=UTF-8
```

### 2\. Linux / macOS 环境

标准 GCC 编译：

```bash
gcc mock_server.c -o mock_server -O2 -finput-charset=UTF-8 -fexec-charset=UTF-8
```

-----


## 📚 目录



&nbsp;  [✨ 核心功能](https://www.google.com/search?q=%23-%E6%A0%B8%E5%BF%83%E5%8A%9F%E8%83%BD)

&nbsp;  [🛠 技术栈](https://www.google.com/search?q=%23-%E6%8A%80%E6%9C%AF%E6%A0%88)

&nbsp;  [📂 工程结构](https://www.google.com/search?q=%23-%E5%B7%A5%E7%A8%8B%E7%BB%93%E6%9E%84)

&nbsp;  [🚀 快速开始](https://www.google.com/search?q=%23-%E5%BF%AB%E9%80%9F%E5%BC%80%E5%A7%8B)

&nbsp;  [📝 详细功能介绍与设计思路](https://www.google.com/search?q=%23-%E8%AF%A6%E7%BB%86%E5%8A%9F%E8%83%BD%E4%BB%8B%E7%BB%8D%E4%B8%8E%E8%AE%BE%E8%AE%A1%E6%80%9D%E8%B7%AF)

&nbsp;      [1. 响应式 UI 架构](https://www.google.com/search?q=%231-%E5%93%8D%E5%BA%94%E5%BC%8F-ui-%E6%9E%B6%E6%9E%84)

&nbsp;      [2. 教务系统爬虫与课表解析](https://www.google.com/search?q=%232-%E6%95%99%E5%8A%A1%E7%B3%BB%E7%BB%9F%E7%88%AC%E8%99%AB%E4%B8%8E%E8%AF%BE%E8%A1%A8%E8%A7%A3%E6%9E%90)

&nbsp;      [3. 动态主题与壁纸系统](https://www.google.com/search?q=%233-%E5%8A%A8%E6%80%81%E4%B8%BB%E9%A2%98%E4%B8%8E%E5%A3%81%E7%BA%B8%E7%B3%BB%E7%BB%9F)

&nbsp;  [🚧 TODO List (开发计划)](https://www.google.com/search?q=%23-todo-list-%E5%BC%80%E5%8F%91%E8%AE%A1%E5%88%92)

&nbsp;  [⚠️ 已知问题与风险 (Pitfalls)](https://www.google.com/search?q=%23%EF%B8%8F-%E5%B7%B2%E7%9F%A5%E9%97%AE%E9%A2%98%E4%B8%8E%E9%A3%8E%E9%99%A9-pitfalls)

&nbsp;  [📄 版权说明](https://www.google.com/search?q=%23-%E7%89%88%E6%9D%83%E8%AF%B4%E6%98%8E)



-----



## ✨ 核心功能

 * **多端适配**：基于 `WindowSizeClass` 的自适应布局，自动切换底部导航（手机）、侧边栏（小平板）或双栏布局（大屏设备）。
 * **智能启动流程**：
     - SplashScreen 自动检测 Token 有效性
     - 有效则直接进入主页，无效则跳转登录
     - 基于 LaunchedEffect 的响应式导航
 * **教务系统集成**：
     - 模拟登录（处理 RSA 加密、CSRF Token、Cookie 保持）
     - 课表查询与解析（支持复杂周次、节次解析）
     - 本地持久化缓存（Room 数据库）
 * **学生事务中心**：
     - 动态招新表单
     - 图片压缩与预处理
     - 数据校验与提交
 * **个性化系统**：
     - 内置多套主题（Material Design, 二次元, Android 2.3/4.0 复古风）
     - 集成 Lolicon API 获取二次元壁纸，支持缓存策略与相册保存
 * **依赖注入架构**：
     - 全面使用 Hilt 管理依赖
     - Repository 层通过接口绑定，便于切换实现（Fake/Real）
     - ViewModel 自动注入，无需手动创建



-----



## 🛠 技术栈

 * **语言**: Kotlin 2.1.0
 * **UI 框架**: Jetpack Compose (Material3)
 * **架构**: MVVM (Model-View-ViewModel)
 * **依赖注入**: Hilt (Dagger)
 * **网络**: Retrofit + OkHttp + Kotlinx Serialization
 * **数据库**: Room (SQLite ORM) + KSP
 * **异步处理**: Coroutines + Flow
 * **图片加载**: Coil
 * **导航**: Navigation Compose
 * **编译工具**: KSP (Kotlin Symbol Processing)



-----



## 📂 工程结构

项目采用按**功能特性 (Feature-based)** 分包的结构，便于模块化维护：

```text
com.suseoaa.projectoaa
├── common                  // 公共基础模块
│   ├── base                // BaseViewModel 等基类
│   ├── network             // 网络配置、API服务、NetworkModule
│   ├── navigation          // 路由定义 (AppRoutes)
│   ├── theme               // 主题配置、颜色系统
│   └── util                // 工具类 (SessionManager, ImageCompressor, WallpaperManager)
├── login                   // 认证模块
│   ├── api                 // 登录/注册 API接口
│   ├── model               // 用户数据模型
│   ├── repository          // 认证数据仓库
│   ├── ui                  // 登录、注册、个人资料界面 (含 SplashScreen)
│   └── viewmodel           // 登录业务逻辑 (MainViewModel, ProfileViewModel)
├── courseList              // 课表模块 (核心业务)
│   ├── data                // Room实体, Mapper, API, 爬虫Client
│   ├── ui                  // 课表展示界面
│   └── viewmodel           // 课表业务逻辑
├── student                 // 学生事务模块
│   ├── model               // 表单数据模型
│   ├── network             // 学生事务API
│   ├── repository          // 学生事务数据仓库
│   ├── util                // 表单校验器 (FormValidator)
│   ├── ui                  // 报名表单界面
│   └── viewmodel           // 学生表单业务逻辑
├── startHomeNavigation     // 主导航与首页模块
│   ├── platform            // 平台相关功能
│   ├── repository          // 首页数据仓库
│   ├── ui                  // 自适应布局、主屏幕、详情页
│   ├── viewmodel           // 首页、打卡、详情等业务逻辑
│   ├── AppModule.kt        // Hilt 依赖注入模块
│   └── NavigationSetup.kt  // 自适应导航配置
├── roomDemo                // 数据库演示模块 (开发调试用)
├── MainActivity.kt         // 应用主入口 (@AndroidEntryPoint)
└── MyApplication.kt        // Application类 (@HiltAndroidApp)
```



-----



## 🚀 快速开始

1. **Clone 项目**:
   ```bash
   git clone https://github.com/suse-edu-cn/SUSE-OAA-APP.git
   ```

2. **配置环境**: 
   - 确保 Android Studio 为最新版（支持 Compose）
   - JDK 21+
   - Kotlin 2.1.0+

3. **配置 API 地址**: 
   在项目根目录创建或编辑 `local.properties` 文件，添加：
   ```properties
   BASE_URL="http://your-backend-url:port/"
   ```
   如使用模拟器本地开发，默认为：`http://10.0.2.2:8080/`

4. **同步 Gradle**: 等待依赖下载完成。

5. **运行**:
   - 建议使用模拟器或真机（API 28+）
   - 若要测试"二次元主题"的壁纸下载，请确保设备可以访问互联网

-----



## 📝 详细功能介绍与设计思路

### 1. 响应式 UI 架构

在 `startHomeNavigation` 模块中，通过 `NavigationSetup.kt` 实现了 `AdaptiveApp`。

 * **Compact (手机)**: 使用 `NavigationBar` (底部导航)。
 * **Medium (折叠屏/小平板)**: 使用 `NavigationRail` (侧边导航栏)需要注意的是，开发者没有折叠屏，遂不保证使用效果。
 * **Expanded (大平板/桌面)**: 使用 `PermanentNavigationDrawer` (双栏布局)，左侧为导航抽屉，右侧内容区分为主内容和辅助内容。

### 2. Hilt 依赖注入架构

项目全面采用 Hilt 进行依赖注入，实现松耦合和可测试性：

 * **Application 层**: `MyApplication.kt` 使用 `@HiltAndroidApp` 注解
 * **Activity 层**: `MainActivity.kt` 使用 `@AndroidEntryPoint` 注解
 * **ViewModel 层**: 所有 ViewModel 使用 `@HiltViewModel` 和 `@Inject` 构造器
 * **Repository 层**: 通过 `AppModule.kt` 提供依赖（SharedPreferences 等）
 * **接口绑定**: `RepositoryBindingModule` 使用 `@Binds` 绑定接口到实现，便于切换 Fake/Real 实现

### 3. 智能启动流程

`SplashScreen.kt` 实现了自动化的启动逻辑：

 * 使用 `LaunchedEffect` 异步检查 Token 有效性
 * 根据验证结果自动导航到主页或登录页
 * 通过 `popUpTo` 清理导航栈，避免返回 Splash 页面

### 4. 教务系统爬虫与课表解析

位于 `courseList` 模块。这不是普通的 REST API 调用，而是**模拟浏览器行为**。

 * **流程**: 获取 CSRF Token -> 获取 RSA 公钥 -> 加密密码 -> 提交登录表单 -> 处理 302 重定向 -> 保持 Cookie -> 查询课表 HTML/JSON。
 * **数据存储优化**:
     - 使用 `CourseMapper` 将复杂的 API 返回数据转换为本地 Entity。
     - **周次掩码 (Bitmask)**: 为了高效查询"某周是否有课"，将 "1-16周" 这样的字符串转换为 `Long` 类型的二进制位（如 `0xFFFF`）。查询时只需进行位运算 `(weeksMask & (1L << week)) != 0`，性能极高，我说的。

### 5. 动态主题与壁纸系统

位于 `common/theme` 和 `util/WallpaperManager.kt`。

 * **策略模式**: `ThemeManager` 管理当前主题配置。
 * **智能缓存**: 壁纸下载后缓存到本地 `filesDir`，并通过 `SharedPreferences` 记录更新时间。只有当缓存不足或超过更新时间（2天）时才会联网拉取新图，节省流量。

-----



## 🚧 开发计划



这份清单列出了接下来的开发重点，可能会按优先级执行。

### 🔴 高优先级

 - [ ] **完善 AuthRepository**: `updateUserInfo` 和 `updatePassword` 在 Repository 中目前可能只有空壳或缺失，需要对接真实 API。
 - [ ] **学生表单 API 对接**: `StudentRepository` 目前指向的后端接口可能需要根据实际后端文档调整字段名（`ApplicationRequest` 已根据文档调整，需联调）。
 - [ ] **Token 刷新机制**: 实现 Token 自动刷新逻辑，避免频繁重新登录。

### 🟡 中优先级

 - [ ] **个人资料图片上传**: `ProfileViewModel` 中有头像显示逻辑，但 `updateUserInfo` 目前未包含头像文件的上传逻辑（通常需要 `MultipartBody`）。
 - [ ] **课表 UI 优化**: `CourseListScreen` 目前是简单的列表展示，建议实现类似"超级课程表"的**周视图 (Grid Layout)**。
 - [ ] **错误处理增强**: 全局的网络错误处理（如 Token 过期自动跳转登录页）目前分散在各个 ViewModel，建议在 `NetworkModule` 的拦截器中统一处理 `401/403`。

### 🟢 低优先级

 - [ ] **单元测试**: 为 `FormValidator`、`CourseMapper` 和主要 Repository 添加 JUnit 测试。
 - [ ] **UI 测试**: 使用 Compose Testing 添加关键流程的 UI 自动化测试。
 - [ ] **RoomDemo 清理**: 项目稳定后，可以移除 `roomDemo` 模块。
 - [ ] **性能优化**: 使用 Baseline Profiles 优化应用启动性能。

### ✅ 已完成

 - [x] **Hilt 依赖注入**: 已全面集成 Hilt，包括 ViewModel、Repository 和 Module 配置。
 - [x] **智能启动流程**: SplashScreen 已实现 Token 验证和自动导航。
 - [x] **响应式布局**: 已实现基于 WindowSizeClass 的自适应布局。



-----



## ⚠️ 已知问题与风险 (Pitfalls)



可能存在的问题：



1.  **教务系统反爬虫策略**:



&nbsp;      代码中硬编码了 `User-Agent` 和 `Referer`。如果学校系统升级（如增加验证码、更复杂的加密、或禁止非浏览器指纹），`SchoolSystemClient.kt` 的爬虫逻辑会失效。

&nbsp;      **风险点**: `RSAEncryptorFixed` 依赖特定的模数和指数格式，如果服务端更换密钥生成方式，可能导致加密失败。



2.  **周次解析限制**:



&nbsp;      目前使用 `Long` (64位) 存储周次掩码。这意味着如果学期超过 **64周**，逻辑会溢出失效。当然，我觉得应该不会有那一天的。



3.  **图片加载与内存**:



&nbsp;      `ImageCompressor` 虽然做了压缩，但如果用户选择超大分辨率的原图（如 50MB+），在 `BitmapFactory.decodeStream` 阶段仍有 OOM (Out Of Memory) 风险。后面可能会考虑引入 Glide 或 Coil 的采样加载机制。



4.  **保存到相册权限**:



&nbsp;      `WallpaperManager.saveCurrentToGallery` 在 Android 10+ (Scoped Storage) 和 Android 9- 的处理逻辑不同。代码中已做适配，但在部分国产 ROM 上可能仍需特殊处理权限申请，目前仅能保证对MIUI/HyperOS的适配稍微完善。



-----



## 📄 开源说明



Licensed under the [MIT License](https://www.google.com/search?q=LICENSE).

Copyright 2025 HZR

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.