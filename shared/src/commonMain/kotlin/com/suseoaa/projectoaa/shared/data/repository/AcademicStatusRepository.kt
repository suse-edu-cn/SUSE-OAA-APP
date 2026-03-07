package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.remote.api.SchoolApiService
import com.suseoaa.projectoaa.shared.domain.model.teachingplan.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * 学业情况仓库 - 处理学业情况查询相关功能
 * 从教务系统HTML中解析课程类别树、学分要求、已获学分等信息
 */
class AcademicStatusRepository(
    private val api: SchoolApiService,
    private val json: Json
) {

    /**
     * 获取学业情况页面，解析课程类别和学分要求
     * @param studentId 学号
     * @return 包含类别列表和总体计划信息的 Pair
     */
    suspend fun getAcademicStatusCategories(studentId: String): Result<Pair<AcademicPlanOverview, List<AcademicStatusCategory>>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getAcademicStatusPage(studentId)
                if (response.status.value == 200) {
                    val bodyText = response.bodyAsText()
                    // 从HTML（内嵌JS）中解析课程类别和学分要求
                    val result = parseCategoriesFromHtml(bodyText)
                    Result.success(result)
                } else {
                    Result.failure(Exception("获取学业情况失败: ${response.status.value}"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }

    /**
     * 从HTML中解析课程类别、学分要求
     * 教务系统使用内嵌JS动态构建树形结构，关键数据在JS代码中
     *
     * HTML结构示例:
     * <p id='pXXXX' yxxf='115.0' yqzdxf='159' sftg='0'>
     *   2023网络工程&nbsp;要求学分:159.0&nbsp;获得学分:115.0&nbsp;未获得学分:44.0
     * </p>
     */
    private fun parseCategoriesFromHtml(html: String): Pair<AcademicPlanOverview, List<AcademicStatusCategory>> {
        val categories = mutableListOf<AcademicStatusCategory>()
        var planOverview = AcademicPlanOverview()

        // 1. 解析总体计划信息（根节点）
        // 根节点特征: 有 yqzdxf 属性且在树的最顶层
        // 匹配: id='pXXX' yxxf='115.0' yqzdxf='159' sftg='0'
        val rootNodePattern = Regex(
            """id='p([^']+)'\s+yxxf='([^']*)'\s+yqzdxf='([^']*)'\s+sftg='([^']*)'"""
        )

        // 收集所有节点信息
        data class NodeInfo(
            val nodeId: String,
            val yxxf: Double,
            val yqzdxf: Double,
            val sftg: Boolean,
            val name: String,
            val parentId: String,
            val jdkcsx: String,
            val childRelation: String,
            val isLeaf: Boolean
        )

        val allNodes = mutableListOf<NodeInfo>()

        // 解析所有 p 标签中的节点信息
        val pMatches = rootNodePattern.findAll(html)
        val seenNodeIds = mutableSetOf<String>()

        for (match in pMatches) {
            val nodeId = match.groupValues[1]
            if (nodeId in seenNodeIds) continue
            seenNodeIds.add(nodeId)

            val yxxf = match.groupValues[2].toDoubleOrNull() ?: 0.0
            val yqzdxf = match.groupValues[3].toDoubleOrNull() ?: 0.0
            val sftg = match.groupValues[4] == "1"

            // 提取节点名称 - 在 p 标签之后到 &nbsp; 之前
            val afterMatch = html.substring(match.range.last + 1, minOf(html.length, match.range.last + 300))
            val nameMatch = Regex(""">([^<&]+)""").find(afterMatch)
            var name = nameMatch?.groupValues?.get(1)?.trim() ?: ""
            // 清理 JS 字符串拼接残余
            name = name.replace("\" +", "").replace("\"", "").replace("+", "").trim()

            allNodes.add(
                NodeInfo(
                    nodeId = nodeId,
                    yxxf = yxxf,
                    yqzdxf = yqzdxf,
                    sftg = sftg,
                    name = name,
                    parentId = "",
                    jdkcsx = "",
                    childRelation = "",
                    isLeaf = false
                )
            )
        }

        // 2. 解析 li 节点获取父子关系和额外属性
        // 匹配: <li id='liXXX' class='' fxfyqjd_id='YYY' xfyqzjdgx='1'
        val liPattern = Regex(
            """<li\s+id='li([^']+)'\s+class='[^']*'\s+fxfyqjd_id='([^']*)'\s+xfyqzjdgx='([^']*)'"""
        )
        val liMap = mutableMapOf<String, Pair<String, String>>() // nodeId -> (parentId, relation)
        for (liMatch in liPattern.findAll(html)) {
            val nodeId = liMatch.groupValues[1]
            val parentId = liMatch.groupValues[2]
            val relation = liMatch.groupValues[3]
            if (nodeId !in liMap) {
                liMap[nodeId] = Pair(parentId, relation)
            }
        }

        // 3. 解析 div.title 获取 jdkcsx (节点课程属性) 和 sfmjd (是否末节点)
        // 匹配: xfyqjd_id='XXX' jdkcsx='1' leaf='' sfmjd='1'
        val divPattern = Regex(
            """xfyqjd_id='([^']+)'\s+jdkcsx='([^']*)'\s+leaf='[^']*'\s+sfmjd='([^']*)'"""
        )
        data class DivInfo(val jdkcsx: String, val sfmjd: String)
        val divMap = mutableMapOf<String, DivInfo>()
        for (divMatch in divPattern.findAll(html)) {
            val nodeId = divMatch.groupValues[1]
            val jdkcsx = divMatch.groupValues[2]
            val sfmjd = divMatch.groupValues[3]
            if (nodeId !in divMap) {
                divMap[nodeId] = DivInfo(jdkcsx, sfmjd)
            }
        }

        // 4. 合并信息，构建最终节点列表
        val enrichedNodes = allNodes.map { node ->
            val liInfo = liMap[node.nodeId]
            val divInfo = divMap[node.nodeId]
            node.copy(
                parentId = liInfo?.first ?: "",
                childRelation = liInfo?.second ?: "",
                jdkcsx = divInfo?.jdkcsx ?: "",
                isLeaf = divInfo?.sfmjd == "1"
            )
        }

        // 5. 找识根节点（parent 为空的节点）
        val rootNode = enrichedNodes.firstOrNull { it.parentId.isEmpty() }
        if (rootNode != null) {
            planOverview = AcademicPlanOverview(
                planName = rootNode.name,
                totalRequiredCredits = rootNode.yqzdxf,
                totalEarnedCredits = rootNode.yxxf,
                totalRemainingCredits = rootNode.yqzdxf - rootNode.yxxf,
                isPassed = rootNode.sftg
            )
        }

        // 6. 筛选叶子节点作为类别（这些是实际有课程的节点）
        // 叶子节点: sfmjd='1' 或者有 jdkcsx 属性且没有子节点
        val leafNodes = enrichedNodes.filter { node ->
            node.isLeaf && node.nodeId != rootNode?.nodeId
        }

        for (node in leafNodes) {
            categories.add(
                AcademicStatusCategory(
                    categoryId = node.nodeId,
                    categoryName = node.name,
                    requiredCredits = node.yqzdxf,
                    systemEarnedCredits = node.yxxf,
                    isPassed = node.sftg,
                    jdkcsx = node.jdkcsx,
                    parentId = node.parentId,
                    childRelation = node.childRelation
                )
            )
        }

        // 如果没有找到叶子节点，回退到所有非根节点
        if (categories.isEmpty()) {
            for (node in enrichedNodes) {
                if (node.nodeId != rootNode?.nodeId) {
                    categories.add(
                        AcademicStatusCategory(
                            categoryId = node.nodeId,
                            categoryName = node.name,
                            requiredCredits = node.yqzdxf,
                            systemEarnedCredits = node.yxxf,
                            isPassed = node.sftg,
                            jdkcsx = node.jdkcsx,
                            parentId = node.parentId,
                            childRelation = node.childRelation
                        )
                    )
                }
            }
        }

        // 7. 解析隐藏字段获取额外参数（用于"其它课程"节点）
        val hiddenFields = parseHiddenFields(html)

        // 8. 按预定义顺序排序
        val sortedCategories = categories.sortedBy { category ->
            when {
                category.categoryName.contains("通识必修") || category.categoryName.contains("素质教育通识必修") -> 0
                category.categoryName.contains("素质实践必修") || category.categoryName.contains("素质教育实践必修") -> 1
                category.categoryName.contains("学科基础必修") -> 2
                category.categoryName.contains("学科基础选修") -> 3
                category.categoryName.contains("专业基础必修") -> 4
                category.categoryName.contains("专业核心必修") || category.categoryName.contains("专业必修") -> 5
                category.categoryName.contains("专业选修") -> 6
                category.categoryName.contains("集中实践") -> 7
                category.categoryName.contains("复合培养") -> 8
                category.categoryName.contains("通识选修") || category.categoryName.contains("素质教育通识选修") -> 9
                else -> 10
            }
        }

        return Pair(planOverview, sortedCategories)
    }

    /**
     * 从HTML中解析隐藏字段的值
     */
    private fun parseHiddenFields(html: String): Map<String, String> {
        val fields = mutableMapOf<String, String>()
        val fieldNames = listOf("xh_id", "cjlrxn", "cjlrxq", "bkcjlrxn", "bkcjlrxq", "xscjcxkz", "cjcxkzzt", "cjztkz", "cjzt")
        for (fieldName in fieldNames) {
            // 匹配 id="xxx" value="yyy" 或 value="yyy" id="xxx"
            val pattern1 = Regex("""id=['"]?${Regex.escape(fieldName)}['"]?[^>]*value=['"]([^'"]*)['"]""")
            val pattern2 = Regex("""value=['"]([^'"]*)['"]\s*[^>]*id=['"]?${Regex.escape(fieldName)}['"]?""")
            val match = pattern1.find(html) ?: pattern2.find(html)
            if (match != null) {
                fields[fieldName] = match.groupValues[1]
            }
        }
        return fields
    }

    /**
     * 获取指定类别下的课程列表
     * @param categoryId 类别ID
     * @param studentId 学号
     */
    suspend fun getCategoryCourses(
        categoryId: String,
        studentId: String
    ): Result<List<AcademicStatusCourseItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAcademicStatusCourses(categoryId, studentId)
            if (response.status.value == 200) {
                val bodyText = response.bodyAsText()
                val courses = try {
                    json.decodeFromString<List<AcademicStatusCourseItem>>(bodyText)
                } catch (e: Exception) {
                    // 可能返回空数组或错误格式
                    emptyList()
                }
                Result.success(courses)
            } else {
                Result.failure(Exception("获取课程列表失败: ${response.status.value}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * 获取"其它课程学分要求"的课程列表
     * 这个节点使用特殊的请求参数
     */
    suspend fun getOtherCourses(
        studentId: String,
        htmlContent: String
    ): Result<List<AcademicStatusCourseItem>> = withContext(Dispatchers.IO) {
        try {
            val fields = parseHiddenFields(htmlContent)
            val response = api.getAcademicStatusOtherCourses(
                studentId = studentId,
                cjlrxn = fields["cjlrxn"] ?: "",
                cjlrxq = fields["cjlrxq"] ?: "",
                bkcjlrxn = fields["bkcjlrxn"] ?: "",
                bkcjlrxq = fields["bkcjlrxq"] ?: "",
                xscjcxkz = fields["xscjcxkz"] ?: "0",
                cjcxkzzt = fields["cjcxkzzt"] ?: "",
                cjztkz = fields["cjztkz"] ?: "0",
                cjzt = fields["cjzt"] ?: ""
            )
            if (response.status.value == 200) {
                val bodyText = response.bodyAsText()
                val courses = try {
                    json.decodeFromString<List<AcademicStatusCourseItem>>(bodyText)
                } catch (e: Exception) {
                    emptyList()
                }
                Result.success(courses)
            } else {
                Result.failure(Exception("获取其它课程列表失败: ${response.status.value}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * 获取非计划内课程
     */
    suspend fun getNonPlanCourses(
        categoryId: String,
        studentId: String
    ): Result<List<AcademicStatusCourseItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAcademicStatusNonPlanCourses(categoryId, studentId)
            if (response.status.value == 200) {
                val bodyText = response.bodyAsText()
                val courses = try {
                    json.decodeFromString<List<AcademicStatusCourseItem>>(bodyText)
                } catch (e: Exception) {
                    emptyList()
                }
                Result.success(courses)
            } else {
                Result.failure(Exception("获取非计划课程失败: ${response.status.value}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * 计算类别统计信息
     * 使用教务系统同款绩点计算方式：JD字段直接来自服务器
     */
    fun calculateCategoryStats(courses: List<AcademicStatusCourseItem>): AcademicStatusCategory {
        var totalCredits = 0.0
        var earnedCredits = 0.0
        var passedCount = 0
        var failedCount = 0
        var studyingCount = 0
        var notStudiedCount = 0

        for (course in courses) {
            val credits = course.credits.toDoubleOrNull() ?: 0.0
            totalCredits += credits

            when (course.studyStatus) {
                StudyStatusUtils.PASSED -> {
                    passedCount++
                    earnedCredits += credits
                }

                StudyStatusUtils.FAILED -> {
                    failedCount++
                }

                StudyStatusUtils.STUDYING -> {
                    studyingCount++
                }

                StudyStatusUtils.NOT_STUDIED -> {
                    notStudiedCount++
                }
            }
        }

        return AcademicStatusCategory(
            categoryId = "",
            categoryName = "",
            courses = courses,
            totalCredits = totalCredits,
            earnedCredits = earnedCredits,
            passedCount = passedCount,
            failedCount = failedCount,
            studyingCount = studyingCount,
            notStudiedCount = notStudiedCount,
            isLoaded = true
        )
    }

    /**
     * 使用教务系统同款绩点计算方式计算平均绩点
     * 直接使用服务器返回的 JD（绩点）字段
     * 加权平均绩点 = Σ(课程绩点 × 课程学分) / Σ(课程学分)
     * 包含已通过（XDZT=4）和不及格（XDZT=2）的课程
     * 不及格课程 JD=0，会拉低平均绩点
     */
    fun calculateWeightedGpa(allCourses: List<AcademicStatusCourseItem>): Double {
        var totalGradePoints = 0.0
        var totalCreditsForGpa = 0.0

        for (course in allCourses) {
            val credits = course.credits.toDoubleOrNull() ?: 0.0
            // 使用教务系统返回的绩点（JD字段）
            val gradePoint = course.gradePoint

            // 已通过和不及格的课程都参与绩点计算
            // 不及格课程 JD=0，其学分计入分母，拉低平均绩点
            if (credits > 0 &&
                (course.studyStatus == StudyStatusUtils.PASSED || course.studyStatus == StudyStatusUtils.FAILED)
            ) {
                totalGradePoints += gradePoint * credits
                totalCreditsForGpa += credits
            }
        }

        return if (totalCreditsForGpa > 0) {
            totalGradePoints / totalCreditsForGpa
        } else 0.0
    }
}
