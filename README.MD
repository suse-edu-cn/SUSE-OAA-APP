
---

# 青蟹 (Project OAA)

**青蟹** 是专为 **四川轻化工大学开放原子开源协会 (SUSE Open Atom Open Source Association)** 量身打造的现代化校园服务客户端。

本项目完全采用 **Kotlin** 与 **Jetpack Compose** 重构，贯彻 **Modern Android Development (MAD)** 理念。项目深度对接四川轻化工大学教务系统，旨在解决传统教务系统移动端体验不佳的问题，同时为协会内部提供高效的服务入口。

## ✨ 核心技术亮点 (Key Highlights)

### 🎨 极致的 UI/UX 体验

* **全屏响应式设计**: 代码中集成了 `WindowWidthSizeClass` 适配逻辑。
    * **手机端**: 使用标准的 `NavigationBar` 底部导航。
    * **平板/折叠屏**: 自动切换为 `NavigationRail` 侧边导航 (`OaaNavRail`)，并支持分栏布局。

* **无缝共享元素转场**: 采用了 Compose 最新实验性特性 `SharedTransitionLayout` 与 `SharedTransitionScope`。
    * 实现了从「教务门户卡片」到「成绩/考试详情页」的丝滑放大、缩小动画，视觉体验对标原生系统级应用。

* **高性能与动效**:
    * 首页与课表页开启了 `graphicsLayer { clip = true }` 硬件层加速，确保在复杂列表滑动时的满帧体验。
    * **动态视差背景**: 个人中心 (`PersonScreen`) 实现了随滚动偏移与缩放的视差头部效果 (`Parallax Header`)。

### 📅 深度定制的智能课表引擎

* **自定义 Layout 布局**: 摒弃传统的 Grid/Table，在 `CourseScreen` 中使用 `Layout` Composable 手动测量和放置组件。
* **完美支持课程重叠**: 能够智能处理同一时间段的多门课程展示。
* **非线性时间轴**: 支持 `DailySchedulePost2025` 等多套作息时间配置，精确映射节次权重。
* **位运算周次解析**: 使用二进制位掩码 (`weeksMask`) 高效处理单双周、跳周等复杂的课程周次逻辑。

### 🔐 稳健的教务网络层

* **反爬虫攻防**:
    * **RSA 加密**: 内置 `RSAEncryptor`，自动获取教务系统公钥并对密码进行 PKCS1 填充加密。
    * **CSRF 防护**: 自动提取登录页面的 `csrftoken` 并在 POST 请求中携带。
    * **Cookie 持久化**: 自定义 `SchoolCookieJar` 结合 `SharedPreferences` 实现 Session 保持，避免频繁登录。

* **智能并发与重试**:
    * **并发详情抓取**: 在成绩查询中，利用 Kotlin Coroutines (`async` + `Semaphore`) 并发抓取单科成绩的详细构成（平时分、实验分、期末分及比例），大幅提升同步速度。
    * **自动保活**: 在 Session 过期（302 跳转或 901 状态码）时，`SchoolAuthRepository` 会自动触发静默重新登录并重试请求，用户无感知。

## 🛠 技术栈 (Tech Stack)

* **语言**: Kotlin 2.0+
* **UI 框架**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material Design 3)
* **架构模式**: MVVM + Clean Architecture (单 Activity 多 Destination 模式)
* **依赖注入**: [Dagger Hilt](https://dagger.dev/hilt/)
* **网络请求**:
    * [Retrofit2](https://square.github.io/retrofit/) & [OkHttp3](https://square.github.io/okhttp/)
    * **Jsoup**: 处理教务系统非标准 API 返回的 HTML 数据（如通知、成绩详情、部分考试信息）。
    * **Kotlin Serialization**: 高效的 JSON 解析。
    * **Coil**: 异步图片加载。
* **本地存储 (Offline-First)**:
    * [Room Database](https://developer.android.com/training/data-storage/room): 缓存课表、成绩、考试信息，支持离线查看。
    * [DataStore](https://developer.android.com/topic/libraries/architecture/datastore): 存储 JWT Token 及用户偏好设置。
* **其他**:
    * **Markdown**: 使用 `com.mikepenz.markdown` 渲染更新日志。
    * **DownloadManager**: 实现应用内更新下载。

## 📂 项目结构 (Project Structure)

```text
com.suseoaa.projectoaa
├── app                 # 应用入口 (MainActivity, AppNavHost, Theme)
├── core                # 核心通用层
│   ├── data            # Repository 模式实现 (协调 Network 与 Database)
│   │   └── repository  # e.g., SchoolGradeRepository, AppUpdateRepository
│   ├── database        # Room 数据库 (Dao, Entity, TypeConverters)
│   ├── datastore       # TokenManager (Proto DataStore 封装)
│   ├── network         # 网络层配置
│   │   ├── di          # Hilt Modules (NetworkModule, SchoolNetworkModule)
│   │   ├── school      # 教务系统专用 (CookieJar, ApiService)
│   │   └── interceptor # AuthInterceptor (应用自身 Token 注入)
│   ├── ui              # 通用 UI 组件 (BaseInfoViewModel, MarkdownText)
│   └── util            # 工具箱 (RSA 加密, Jsoup 解析器, 倒计时算法)
└── feature             # 业务功能模块 (按功能分包)
    ├── login/register  # 登录注册 (含表单验证与 RSA 加密流程)
    ├── home            # 首页容器 (响应式导航栏适配)
    ├── course          # 智能课表 (自定义 Layout, 多账号切换)
    ├── academicPortal  # 教务门户聚合页
    │   ├── getGrades   # 成绩查询 (Room 缓存, 并发详情同步)
    │   ├── getExamInfo # 考试安排 (倒计时高亮, 考场定位)
    │   └── getMessageInfo # 调课通知 (HTML 解析)
    ├── gpa             # 绩点管家 (学位课筛选, 成绩模拟)
    ├── person          # 个人中心 (应用更新, 视差背景)
    └── changePassword  # 密码修改

```

## ✨ 功能详细说明

### 1. 智能课表系统 (Course Schedule)

* **多账号管理**: 支持本地存储多个学生账号，一键无缝切换课表。
* **自定义课程**: 支持手动添加非教务系统的临时课程或活动。
* **校历同步**: 自动抓取并解析学校校历，校准当前周次。

### 2. 教务门户 (Academic Portal)

* **成绩查询**:
* **全量同步**: 能够一次性拉取历史所有成绩，并并发获取**平时成绩**、**实验成绩**及**占比**详情。
* **数据缓存**: `Room` 数据库本地持久化，支持离线查看。
* **自动关联**: 自动同步用户的学院、专业信息，为绩点计算提供基础。


* **考试信息**:
* **智能排序**: 将“未开始”的考试按时间正序排列，“已结束”的考试沉底，重要信息一目了然。
* **倒计时提醒**: 根据当前时间自动计算（如“明天”、“3天后”），并用不同颜色高亮紧急程度。


* **调课通知**:
* 利用 Jsoup 解析教务系统首页 HTML，提取停课、补课等动态通知。



### 3. 绩点管家 (GPA Manager)

* **智能识别学位课**: 自动拉取专业培养计划 (`TeachingPlan`)，通过课程号匹配，精准区分学位课与非学位课。
* **成绩模拟**: 支持用户手动修改课程成绩，实时预览对总绩点和学位绩点的影响，辅助制定学习目标。
* **多维度统计**: 提供总绩点、学位绩点、总学分、学位学分的实时统计面板。

### 4. 软件生态

* **应用内更新**: 集成 GitHub Releases API，自动检测新版本，支持应用内直接下载 (`DownloadManager`) 与安装 (`FileProvider`)，并以 Markdown 格式展示更新日志。

## 🚀 待开发计划 (Roadmap)

* [ ] **桌面小组件 (App Widget)**: 将课表和考试倒计时投射到桌面。
* [ ] **学业预警系统**: 根据本地缓存的成绩，自动计算挂科统计与学分进度。
* [ ] **一键评教**: 实现教务系统期末评教的自动化脚本。
* [ ] **协会社区**: 接入论坛或活动报名功能。

## 🤝 贡献指南

1. **环境准备**: Android Studio Ladybug+ / Kotlin 2.0.0+ / JDK 17+。
2. **Mock 数据**: 开发时可参考 `core/network/model` 下的数据结构。
3. **分支规范**: 功能开发请从 `develop` 分支切出 `feature/your-feature`。

---

**四川轻化工大学开放原子开源协会 (SUSE-OAA) 出品**