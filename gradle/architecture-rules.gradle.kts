// 架构规则检查。
//
// 本轮重构确立的分层边界如果没有东西守着，几个月后必然重新腐化——上一轮
// "Clean Architecture" 就是这么退化成 UI 写进 presentation 包、UiState 下沉到
// shared/domain 的。这里用一个纯 Gradle 任务（不引第三方依赖，KMP 下也无需
// JVM 测试源集）在构建期做静态扫描，违反即失败。
//
// 新增规则时请连同"为什么"一起写进 reason，报错信息要能让人不看这个文件就懂。

data class ArchRule(
    val name: String,
    val reason: String,
    /** 只检查路径包含这些片段的文件 */
    val includePaths: List<String>,
    /** 命中即违规的正则 */
    val forbidden: Regex,
    /** 豁免：路径包含这些片段的文件跳过 */
    val exemptPaths: List<String> = emptyList(),
)

val architectureRules = listOf(
    ArchRule(
        name = "UI 层不得直接依赖数据层",
        reason = "Screen 只应通过 ViewModel 暴露的 UiState 取数；直接 import Repository " +
            "会让 UI 绕过状态管理，也让领域模型和数据实现无法各自演进。",
        includePaths = listOf("/ui/"),
        forbidden = Regex("""^import com\.suseoaa\.projectoaa\.shared\.data\.repository\.""", RegexOption.MULTILINE),
    ),
    ArchRule(
        name = "presentation 层不得包含 Compose UI",
        reason = "presentation 放 ViewModel 与 UiState；@Composable 属于 ui/screen。" +
            "两者混在一个包里正是上一轮架构失控的起点。",
        includePaths = listOf("/presentation/"),
        forbidden = Regex("""^@Composable""", RegexOption.MULTILINE),
    ),
    ArchRule(
        name = "shared 的领域层不得定义 UI 状态",
        reason = "UiState 是 presentation 的产物。定义在 shared/domain 会让数据模块反向依赖 UI 的形状。",
        includePaths = listOf("/shared/domain/"),
        forbidden = Regex("""\bdata class \w*UiState\b"""),
    ),
    ArchRule(
        name = "禁止用 println 打日志",
        reason = "println 在 Release 包同样输出、无法分级、拿不到 tag。统一走 AppLog。",
        includePaths = listOf("/src/"),
        forbidden = Regex("""(?<![.\w])println\("""),
        exemptPaths = listOf("/AppLog.kt", "Test.kt", "/commonTest/", "/androidTest/"),
    ),
    ArchRule(
        name = "禁止硬编码远端地址",
        reason = "所有 baseUrl 收口在 ApiConfig，否则换域名或加代理要全局搜索替换。",
        includePaths = listOf("/src/"),
        forbidden = Regex(""""https?://(?!schemas\.android\.com|www\.w3\.org)[A-Za-z0-9]"""),
        exemptPaths = listOf("/ApiConfig.kt"),
    ),
    ArchRule(
        name = "ViewModel 应依赖仓库接口而非实现",
        reason = "presentation 与 domain 层只依赖 domain/repository 下的接口，测试里才能换成" +
            "假实现。17 个仓库已全部接口化（实现类以 Impl 结尾留在 data/repository，" +
            "由 DI 绑定），这条规则确保不再有人绕回去直连实现。",
        includePaths = listOf("/presentation/", "/domain/"),
        forbidden = Regex("""^import com\.suseoaa\.projectoaa\.shared\.data\.repository\.""", RegexOption.MULTILINE),
    ),
)

tasks.register("checkArchitecture") {
    group = "verification"
    description = "检查分层边界与编码约定，违反则构建失败"

    val sourceDirs = subprojects.mapNotNull { p ->
        p.layout.projectDirectory.dir("src").asFile.takeIf { it.exists() }
    }
    // 声明输入，让任务可以被 Gradle 增量跳过
    inputs.files(sourceDirs.map { fileTree(it) { include("**/*.kt") } })
    outputs.upToDateWhen { true }

    val rules = architectureRules
    val rootPath = rootDir.absolutePath

    doLast {
        val violations = mutableListOf<String>()
        sourceDirs.forEach { dir ->
            dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
                val path = file.absolutePath.removePrefix(rootPath)
                rules.forEach { rule ->
                    val included = rule.includePaths.any { path.contains(it) }
                    val exempt = rule.exemptPaths.any { path.contains(it) }
                    if (!included || exempt) return@forEach
                    file.readLines().forEachIndexed { idx, line ->
                        if (rule.forbidden.containsMatchIn(line)) {
                            violations += "  ${path.removePrefix("/")}:${idx + 1}\n" +
                                "      违反规则「${rule.name}」\n" +
                                "      ${line.trim().take(120)}"
                        }
                    }
                }
            }
        }
        // 文件规模上限。本轮重构把最大文件从 2476 行压到 900 行以内；没有这道闸，
        // 大文件会慢慢长回来——它们正是当初"改一处要通读两千行"的根源。
        // 超限不是让人硬塞，而是提示该按职责拆文件了。
        val maxLines = 900
        sourceDirs.forEach { dir ->
            dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
                val count = file.readLines().size
                if (count > maxLines) {
                    violations += "  ${file.absolutePath.removePrefix(rootPath).removePrefix("/")}:1\n" +
                        "      违反规则「单文件行数上限」\n" +
                        "      $count 行，超过上限 $maxLines 行"
                }
            }
        }

        if (violations.isNotEmpty()) {
            val byRule = rules.filter { r ->
                violations.any { it.contains("「${r.name}」") }
            }.joinToString("\n\n") { "「${it.name}」\n  ${it.reason}" } +
                if (violations.any { it.contains("「单文件行数上限」") })
                    "\n\n「单文件行数上限」\n  单个 Kotlin 文件不超过 900 行；超了说明该按职责拆分了。"
                else ""
            throw GradleException(
                "架构检查未通过，共 ${violations.size} 处违规：\n\n" +
                    violations.joinToString("\n") + "\n\n涉及的规则：\n\n" + byRule
            )
        }
        logger.lifecycle("架构检查通过：${rules.size} 条分层规则 + 单文件行数上限，${sourceDirs.size} 个模块。")
    }
}
