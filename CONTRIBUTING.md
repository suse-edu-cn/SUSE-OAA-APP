# 贡献指南

感谢您对 SUSE OAA 项目的关注！我们欢迎所有形式的贡献。

## 目录

- [行为准则](#行为准则)
- [如何贡献](#如何贡献)
- [开发流程](#开发流程)
- [代码规范](#代码规范)
- [提交规范](#提交规范)
- [测试指南](#测试指南)

## 行为准则

### 我们的承诺

为了营造一个开放和友好的环境，我们作为贡献者和维护者承诺：无论年龄、体型、残疾、种族、性别认同和表达、经验水平、国籍、外貌、种族、宗教或性认同和取向如何，参与我们的项目和社区的每个人都将获得无骚扰的体验。

### 我们的标准

有助于创建积极环境的行为示例：

- ✅ 使用友好和包容的语言
- ✅ 尊重不同的观点和经验
- ✅ 优雅地接受建设性批评
- ✅ 关注对社区最有利的事情
- ✅ 对其他社区成员表示同理心

不可接受的行为示例：

- ❌ 使用性化的语言或图像，以及不受欢迎的性关注或示好
- ❌ 挑衅、侮辱/贬损评论，以及人身或政治攻击
- ❌ 公开或私下骚扰
- ❌ 未经明确许可发布他人的私人信息，如地址或电子邮件
- ❌ 其他在专业环境中可能被合理认为不适当的行为

## 如何贡献

### 报告 Bug

报告 Bug 前，请先检查 [Issues](https://github.com/yourusername/SUSEOAA/issues) 中是否已存在相同问题。

如果没有，请创建一个新 Issue 并提供：

1. **清晰的标题** - 简短描述问题
2. **详细描述** - 问题的详细说明
3. **复现步骤** - 如何触发这个问题
4. **期望行为** - 应该发生什么
5. **实际行为** - 实际发生了什么
6. **环境信息**:
   - 设备型号
   - 操作系统版本
   - 应用版本
7. **截图/日志** - 如果有的话

**Bug 报告模板**:

```markdown
## Bug 描述
简要描述遇到的问题

## 复现步骤
1. 进入 '...'
2. 点击 '....'
3. 滚动到 '....'
4. 看到错误

## 期望行为
应该显示课程表

## 实际行为
应用崩溃

## 环境信息
- 设备: iPhone 14 Pro
- OS: iOS 17.2
- 应用版本: 1.29.12

## 截图
如果适用，添加截图帮助解释问题

## 额外信息
添加其他相关信息
```

### 建议新功能

功能建议前，请检查 [Issues](https://github.com/yourusername/SUSEOAA/issues) 和 [Discussions](https://github.com/yourusername/SUSEOAA/discussions)。

创建功能建议时，请提供：

1. **功能描述** - 详细说明建议的功能
2. **使用场景** - 为什么需要这个功能
3. **可能的实现** - 如何实现（如果有想法）
4. **替代方案** - 考虑过的其他方案
5. **额外信息** - 相关的截图、链接等

**功能建议模板**:

```markdown
## 功能描述
详细描述建议的新功能

## 动机
为什么需要这个功能？它解决什么问题？

## 建议的实现
如何实现这个功能

## 替代方案
考虑过哪些其他方案

## 额外信息
添加其他相关信息、截图等
```

## 开发流程

### 1. Fork 项目

点击仓库页面右上角的 "Fork" 按钮，创建您自己的副本。

### 2. 克隆仓库

```bash
git clone https://github.com/your-username/SUSEOAA.git
cd SUSEOAA
```

### 3. 创建分支

```bash
# 基于 develop 分支创建新分支
git checkout develop
git pull origin develop
git checkout -b feature/your-feature-name
```

分支命名规范：
- `feature/` - 新功能
- `fix/` - Bug 修复
- `docs/` - 文档更新
- `refactor/` - 代码重构
- `test/` - 测试相关
- `chore/` - 构建/工具相关

### 4. 开发

- 编写代码
- 遵循代码规范
- 添加必要的注释
- 编写/更新测试
- 更新文档

### 5. 测试

```bash
# 运行所有测试
./gradlew test

# Android 测试
./gradlew :composeApp:testDebugUnitTest

# 构建检查
./gradlew build
```

### 6. 提交

```bash
git add .
git commit -m "feat: add amazing feature"
```

### 7. 推送

```bash
git push origin feature/your-feature-name
```

### 8. 创建 Pull Request

1. 访问您 Fork 的仓库
2. 点击 "New Pull Request"
3. 选择 `develop` 作为目标分支
4. 填写 PR 描述
5. 等待审查

## 代码规范

### Kotlin 编码规范

遵循 [Kotlin 官方编码约定](https://kotlinlang.org/docs/coding-conventions.html)。

#### 命名规范

```kotlin
// ✅ 类名：大驼峰
class CourseViewModel

// ✅ 函数名：小驼峰
fun fetchCourseList()

// ✅ 变量名：小驼峰
val userName: String

// ✅ 常量：大写下划线
const val MAX_RETRY_COUNT = 3

// ✅ 私有属性：下划线前缀（可选）
private val _uiState = MutableStateFlow<UiState>()
val uiState = _uiState.asStateFlow()
```

#### 格式化

```kotlin
// ✅ 4 个空格缩进
fun example() {
    if (condition) {
        doSomething()
    }
}

// ✅ 一行不超过 120 字符
fun longFunction(
    param1: String,
    param2: Int,
    param3: Boolean
): Result {
    // ...
}

// ✅ 链式调用换行
val result = list
    .filter { it.isActive }
    .map { it.name }
    .sorted()
```

#### 注释

```kotlin
/**
 * 课程数据仓库
 * 
 * 负责从网络和本地数据库获取课程信息
 * 
 * @property api 网络 API 服务
 * @property database 本地数据库
 */
class CourseRepository(
    private val api: ApiService,
    private val database: Database
) {
    /**
     * 获取课程列表
     * 
     * @param semester 学期，格式: "2023-2024-1"
     * @return 课程列表
     * @throws NetworkException 网络错误
     */
    suspend fun getCourses(semester: String): List<Course> {
        // 实现
    }
}
```

### Compose 规范

```kotlin
// ✅ Composable 函数名：大驼峰
@Composable
fun CourseCard(
    course: Course,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // modifier 参数放在最后
    // 提供默认值 Modifier
}

// ✅ 使用预览
@Preview
@Composable
fun CourseCardPreview() {
    AppTheme {
        CourseCard(
            course = Course.sample(),
            onClick = {}
        )
    }
}

// ✅ 提取可重用组件
@Composable
private fun CourseTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium
    )
}
```

## 提交规范

使用 [Conventional Commits](https://www.conventionalcommits.org/) 规范。

### 格式

```
<类型>[可选的作用域]: <描述>

[可选的正文]

[可选的脚注]
```

### 类型

- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更新
- `style`: 代码格式（不影响功能）
- `refactor`: 代码重构
- `perf`: 性能优化
- `test`: 测试相关
- `chore`: 构建/工具相关
- `ci`: CI 配置
- `revert`: 回滚提交

### 示例

```bash
# 新功能
git commit -m "feat: add course schedule export feature"

# Bug 修复
git commit -m "fix: resolve crash when loading grades"

# 文档
git commit -m "docs: update README installation guide"

# 重构
git commit -m "refactor: extract network logic to repository"

# 带作用域
git commit -m "feat(grades): add semester filter"

# 带正文
git commit -m "feat: add dark mode support

- Add theme toggle in settings
- Persist user preference
- Update all screens for dark theme"

# Breaking Change
git commit -m "feat!: change API response format

BREAKING CHANGE: API now returns camelCase instead of snake_case"
```

## 测试指南

### 单元测试

```kotlin
// commonTest/kotlin/com/example/RepositoryTest.kt
class CourseRepositoryTest {
    private lateinit var repository: CourseRepository
    private lateinit var fakeApi: FakeApiService
    
    @BeforeTest
    fun setup() {
        fakeApi = FakeApiService()
        repository = CourseRepository(fakeApi)
    }
    
    @Test
    fun `fetch courses returns data`() = runTest {
        val result = repository.getCourses("2023-2024-1")
        assertEquals(3, result.size)
    }
}
```

### UI 测试

```kotlin
@Test
fun courseCard_displays_correct_info() {
    composeTestRule.setContent {
        CourseCard(
            course = Course.sample(),
            onClick = {}
        )
    }
    
    composeTestRule
        .onNodeWithText("Kotlin Programming")
        .assertIsDisplayed()
}
```

### 运行测试

```bash
# 所有测试
./gradlew test

# 特定模块
./gradlew :composeApp:test

# 带覆盖率
./gradlew test --coverage
```

## Pull Request 指南

### PR 描述模板

```markdown
## 描述
简要描述此 PR 的目的

## 类型
- [ ] Bug 修复
- [ ] 新功能
- [ ] 代码重构
- [ ] 文档更新
- [ ] 其他

## 变更内容
- 变更 1
- 变更 2

## 测试
描述如何测试这些变更

## 截图（如适用）
添加截图

## 检查清单
- [ ] 代码遵循项目规范
- [ ] 已添加/更新测试
- [ ] 所有测试通过
- [ ] 已更新文档
- [ ] 在 Android 和 iOS 上测试通过
```

### 审查流程

1. 自动检查（CI）
2. 代码审查（至少 1 人）
3. 测试验证
4. 合并到 develop

### 合并要求

- ✅ 所有 CI 检查通过
- ✅ 至少 1 个审查批准
- ✅ 无冲突
- ✅ 遵循代码规范

## 开发环境

### 必需工具

- JDK 17+
- Android Studio Ladybug+
- Xcode 15+ (macOS，iOS 开发)

### 推荐插件

- Kotlin Multiplatform Mobile
- Rainbow Brackets
- GitToolBox
- SonarLint

### 配置

```bash
# 克隆项目
git clone https://github.com/your-username/SUSEOAA.git
cd SUSEOAA

# 设置 Git 用户
git config user.name "Your Name"
git config user.email "your.email@example.com"

# 安装 Git hooks（可选）
./scripts/install-hooks.sh
```

## 获取帮助

如有疑问：

- 📖 查看 [README](README.MD)
- 💬 [Discussions](https://github.com/yourusername/SUSEOAA/discussions)
- 📧 联系维护者

## 许可证

贡献代码时，您同意您的贡献将遵循项目的 MIT 许可证。

---

再次感谢您的贡献！🎉
