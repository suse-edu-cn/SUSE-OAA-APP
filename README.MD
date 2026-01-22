# 青蟹 (Project OAA) - 四川轻化工大学教务助手

**青蟹 (Project OAA)** 是专为 **四川轻化工大学开放原子开源协会 (SUSE Open Atom Open Source Association)** 量身打造的现代化校园服务客户端。本项目作为协会内部的技术标杆，旨在通过 **Modern Android Development (MAD)** 理念重构传统的教务体验。

---

## 📚 目录

- [核心亮点 (Highlights)](#-核心亮点-highlights)
- [功能全景 (Features)](#-功能全景-features)
- [技术架构 (Architecture)](#-技术架构-architecture)
- [数据持久化 (Database Design)](#-数据持久化-database-design)
- [网络与安全 (Network & Security)](#-网络与安全-network--security)
- [UI/UX 设计系统](#-uiux-设计系统)
- [项目结构 (Structure)](#-项目结构-structure)
- [开发环境与贡献](#-开发环境与贡献)

---

## ✨ 核心亮点 (Highlights)

### 🎨 极致的 UI/UX 体验
* **全平台响应式设计**: 基于 `WindowWidthSizeClass` 的自适应布局策略。
    * **手机端**: 采用标准底部导航栏 (`NavigationBar`)，针对单手操作优化。
    * **平板/折叠屏**: 自动切换为侧边导航栏 (`NavigationRail`)，并在课表页面启用 **分栏布局**（左侧课表，右侧详情），充分利用大屏优势。
* **高性能渲染**:
    * 首页与课表页开启 `graphicsLayer { clip = true }` 硬件层加速，确保复杂列表滑动满帧。
    * 采用 `Layout` Composable 手动测量和布局课表，避免了传统嵌套 `LazyColumn` 或 `Grid` 带来的性能开销。
* **动效细节**: 
    * **共享元素转场**: 利用 `SharedTransitionLayout` 实现卡片展开/收起的丝滑动画。
    * **视差滚动**: 个人中心 (`PersonScreen`) 实现了头部背景随滚动缩放的视差效果。

### 📅 深度定制的智能课表引擎
* **非线性时间轴映射**: 支持多套作息时间配置（如 `DailySchedulePost2025`），将课程节次精准映射为屏幕坐标。
* **智能冲突处理**: 完美支持同一时间段多门课程的重叠展示逻辑。
* **位运算周次解析**: 使用二进制掩码 (`weeksMask`) 高效处理单双周、跳周逻辑，解析速度达到纳秒级。
* **当前时刻高亮**: 自动计算并在课表时间轴上高亮显示“当前时刻”及“今日列”，提供直观的时间感知。

---

## 🛠 技术架构 (Architecture)

本项目严格遵循 **Google 推荐的应用架构指南**，采用 **MVVM + Clean Architecture** 模式。

### 🏗️ 架构分层
1.  **UI Layer (Presentation)**:
    * **Components**: 100% Jetpack Compose (Material Design 3).
    * **State Management**: `ViewModel` + `StateFlow` + `collectAsStateWithLifecycle`。
    * **Navigation**: 单 Activity 多 Destination 模式，使用 Compose Navigation 进行路由管理。
2.  **Domain Layer (Optional)**:
    * 包含复杂的业务逻辑用例（如 GPA 计算算法、课表冲突检测）。
3.  **Data Layer**:
    * **Repository Pattern**: 统一数据出口，协调 `NetworkDataSource` 和 `LocalDataSource`。
    * **Offline-First**: 优先从 Room 数据库加载缓存数据，网络请求成功后静默更新 UI。

### 🔧 核心技术栈
* **语言**: Kotlin 2.0+ (启用 K2 编译器)
* **依赖注入**: Dagger Hilt (解耦组件依赖，单例管理)
* **网络通信**: 
    * **Retrofit2 & OkHttp3**: 标准 RESTful API 请求。
    * **Kotlin Serialization**: 现代化的 JSON 解析库，配置了 `ignoreUnknownKeys` 和 `isLenient` 以应对不规范的教务接口。
    * **Jsoup**: 处理教务系统遗留的非接口页面（如通知公告、部分考试信息）。
* **本地存储**:
    * **Room Database**: 类型安全的 SQLite 封装，支持 Flow 响应式查询。
    * **DataStore (Proto)**: 存储用户偏好设置与加密 Token。
* **异步处理**: Kotlin Coroutines + Flow。

---

## 💾 数据持久化 (Database Design)

数据库设计充分考虑了教务数据的复杂性，当前版本 (v13) 包含以下核心实体：

| 实体 (Entity) | 描述 | 关键字段 |
| :--- | :--- | :--- |
| **CourseEntity** | 课程基础信息 | `courseName`, `teacher`, `credit`, `category` (课程性质) |
| **ClassTimeEntity** | 上课时间地点 | `weekday`, `startNode`, `step` (持续节数), `weeks` (周次) |
| **GradeEntity** | 成绩详情 | `regularScore` (平时), `finalScore` (期末), `experimentScore` (实验), `ratios` (占比) |
| **CourseAccountEntity** | 多账号存储 | `studentId`, `password` (AES/RSA 加密存储) |
| **ExamCacheEntity** | 考试安排缓存 | `examName`, `time`, `location`, `seatNumber` |

> **版本迁移策略**:
> * `MIGRATION_10_11` -> `11_12` -> `12_13`: 逐步完善了成绩详情字段，支持记录平时分、期末分及其具体占比，为精细化的学情分析提供数据支撑。

---

## 🌐 网络与安全 (Network & Security)

### 🔐 安全机制
1.  **RSA 非对称加密**: 内置 `RSAEncryptor` 工具类，登录时自动获取教务系统公钥，对用户密码进行 PKCS1 填充加密，防止明文传输。
2.  **CSRF 防护**: 自动提取登录页面的 `csrftoken` 并在后续 POST 请求中携带，通过服务器验证。
3.  **敏感数据隔离**: 用户的学号与密码仅存储在本地加密数据库中，绝不上传至任何第三方服务器（除教务系统本身）。

### 📡 稳健的网络层
* **Cookie 持久化**: 自定义 `SchoolCookieJar` 结合 SharedPreferences，实现 Session 的长期保持，减少用户重复登录频率。
* **智能并发抓取**: 
    * 在成绩查询模块，使用 `async` + `Semaphore` 实现高并发详情页抓取。
    * 针对教务系统的慢速响应进行超时重试优化。
* **自动保活 (Keep-Alive)**: 
    * `AuthInterceptor` 拦截器自动监测 `302 Redirect` 或特定错误码 (`901`)，在后台静默触发重新登录流程，用户无感知。

---

## 📱 功能全景 (Features)

### 1. 智能课表系统 (Course Schedule)
* **多维度视图**:
    * 支持 **按周切换** (HorizontalPager) 与 **回到本周** 一键跳转。
    * 支持 **自定义开学日期**，灵活适配不同学期的校历。
* **多账号管理**: 本地安全存储多个学生账号，支持一键无缝切换，方便社团干部或情侣使用。
* **自定义课程**: 提供 `AddCustomCourseDialog`，支持用户手动添加非教务系统的临时课程、社团活动或兼职安排。

### 2. 教务门户聚合 (Academic Portal)
* **全量成绩查询**:
    * 能够拉取入学以来的所有历史成绩。
    * **详情透视**: 点击成绩卡片即可查看平时分、实验分、期末分及各自占比。
* **考试日程管家**:
    * **智能排序**: 未考科目置顶，已考科目沉底。
    * **倒计时提醒**: 醒目颜色标注剩余天数（如“明天”、“3天后”）。

### 3. 绩点管家 (GPA Manager)
* **学位课识别**: 基于专业培养计划，自动标记学位课程。
* **模拟计算**: 支持用户本地修改某门课的成绩，实时预览对总绩点的影响，辅助制定复习策略。

### 4. 软件生态
* **应用内更新**: 集成 GitHub API，自动检测 Releases 更新，支持断点续传下载。
* **公告系统**: 解析教务处官网通知，重要信息不错过。

---

## 📂 项目结构 (Structure)

```text
com.suseoaa.projectoaa
├── app                 # 壳工程 (MainActivity, Theme)
├── core                # 核心基础层
│   ├── database        # Room 数据库定义 (Dao, Entity, Migration)
│   ├── network         # 网络模块 (Retrofit, Interceptor, Di)
│   ├── ui              # 通用 UI 组件 (Icons, Themes, BaseViewModel)
│   └── util            # 工具类 (RSA, Date, Encryption)
└── feature             # 业务功能模块
    ├── login           # 登录注册 (状态机管理)
    ├── course          # 智能课表 (核心复杂 UI)
    ├── academicPortal  # 教务功能聚合
    │   ├── getGrades   # 成绩查询
    │   └── getExamInfo # 考试查询
    ├── gpa             # 绩点计算
    └── person          # 个人中心

```

---

## 🤝 贡献指南

我们非常欢迎对 **Kotlin**, **Compose**, 或 **Android 逆向** 感兴趣的同学加入开发！

1. **环境准备**:
* Android Studio Ladybug+
* Kotlin 2.0.0+
* JDK 17+


2. **分支规范**:
* `main`: 稳定发布分支。
* `develop`: 日常开发分支，所有 PR 请提交至此。
* `feature/your-feature`: 功能开发分支。


3. **Code Style**: 请遵循官方 Kotlin Coding Conventions。

---

**四川轻化工大学开放原子开源协会 (SUSE-OAA) 出品** *Code with ❤️ by SUSE-OAA Developers*