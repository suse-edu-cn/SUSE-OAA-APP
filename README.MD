
---

# 青蟹 (Project OAA)

**青蟹** 是专为 **四川轻化工大学开放原子开源协会 (SUSE Open Atom Open Source Association)** 开发的校园服务客户端。

本项目采用现代 Android 开发技术栈（Jetpack Compose, MVVM, Hilt, Coroutines），旨在为学生提供便捷的课表查询、成绩查询、考试信息获取以及协会内部服务。项目深度对接学校教务系统，并实现了优雅的响应式布局与转场动画。

## 🛠 技术栈 (Tech Stack)

* **UI 框架**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material Design 3)
* **架构模式**: MVVM + Clean Architecture (Core/Feature 分层)
* **依赖注入**: [Dagger Hilt](https://dagger.dev/hilt/)
* **网络请求**: [Retrofit2](https://square.github.io/retrofit/) & [OkHttp3](https://square.github.io/okhttp/)
* **解析工具**: [Kotlin Serialization](https://kotlinlang.org/docs/serialization.html) (JSON) & [Jsoup](https://jsoup.org/) (HTML 爬虫)
* **本地存储**: [Room Database](https://developer.android.com/training/data-storage/room) (课程/成绩) & [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) (Token/配置)
* **导航与动画**: Navigation Compose + **Shared Element Transitions** (共享元素转场)

## 📂 项目结构 (Project Structure)

```text
com.suseoaa.projectoaa
├── app                 # 入口：MainActivity, AppNavHost, 全局 ViewModel
├── core                # 核心通用层
│   ├── data            # Repository 实现，处理网络与本地数据同步
│   ├── database        # Room 数据库定义 (Entity, Dao)
│   ├── datastore       # TokenManager (JWT 与当前学号存储)
│   ├── designsystem    # 统一主题 (Theme, Color, Type)
│   ├── network         # 网络层 (Service, Interceptor, CookieJar)
│   ├── ui              # 基类 UI 组件 (BaseInfoViewModel)
│   └── util            # 工具类 (RSA 加密、Html 解析、倒计时计算)
└── feature             # 业务功能模块
    ├── login/register  # 用户鉴权模块
    ├── home            # 主页容器与响应式导航 (BottomBar/NavRail)
    ├── course          # 智能课表 (多时间轴适配、自定义课程、多账号)
    ├── academicPortal  # 教务门户 (功能聚合页)
    │   ├── getGrades   # 成绩查询 (全量同步、绩点计算)
    │   ├── getExamInfo # 考试查询 (倒计时提醒、考场定位)
    │   └── getMessageInfo # 调课通知 (HTML 动态解析展示)
    ├── person          # 个人中心
    └── changePassword  # 密码管理

```

## ✨ 功能特性 (Features)

### ✅ 已实现功能

1. **智能课表系统 (Course Schedule)**
* **多版本时间轴**: 自动识别 2025 年前后校区作息时间调整。
* **可视化布局**: 采用自定义 Layout 实现重叠课程处理与非线性时间轴展示。
* **高度自定义**: 支持手动添加非教务系统课程，支持多学生账号快速切换。


2. **教务门户 (Academic Portal)**
* **成绩查询**: 自动计算学期绩点，支持按学年学期筛选，具备全量同步功能。
* **考试信息 (New)**: 实时拉取考试安排，内置**考试倒计时**算法，自动区分考试状态（进行中/已结束）。
* **调课通知 (New)**: 通过 Jsoup 解析教务系统首页动态，第一时间获取停课、补课、教室变动信息。


3. **极致视觉体验**
* **共享元素动画**: 门户首页卡片跳转至详情页时，使用 `SharedTransitionLayout` 实现无缝缩放转场。
* **响应式布局**: 自动适配手机（底部栏）与平板（侧边栏/分屏详情）模式。


4. **安全机制**
* **模拟登录**: 处理教务系统复杂的 CSRF Token 校验与 RSA 密码公钥加密。
* **会话保持**: 自定义 `SchoolCookieJar` 实现教务系统 Session 持久化，减少频繁登录。



---

### 🚀 待开发计划 (Roadmap)

#### 1. 🏠 首页模块 (Home)

* [ ] **协会风采**: 协会简介、部门职责展示。
* [ ] **动态墙**: 展示协会最新活动快讯。

#### 2. 🎓 教务增强

* [ ] **一键评教**: 自动化处理期末教学评价。
* [ ] **学业预警**: 根据成绩自动汇总未修满学分。

#### 3. 👤 个人中心

* [ ] **账号关联**: 协会账号与多个教务账号的一键绑定与管理。
* [ ] **个人资料修改**: 支持修改头像及昵称（对接协会后台）。

## 🔨 开发与贡献

1. **环境要求**: Android Studio Ladybug+ / Kotlin 2.0+。
2. **API 配置**: 协会内部 API 地址定义在 `core/network/BASE_URL.kt`。
3. **运行**: 项目支持 `fallbackToDestructiveMigration`，数据库升级时会自动处理迁移。

---

**四川轻化工大学开放原子开源协会 (SUSE-OAA) 出品**