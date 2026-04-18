package com.suseoaa.projectoaa.shared.data.remote.api

import com.suseoaa.projectoaa.shared.domain.model.school.CourseResponseJson
import com.suseoaa.projectoaa.shared.domain.model.school.RSAKey
import com.suseoaa.projectoaa.shared.domain.model.exam.ExamResponse
import com.suseoaa.projectoaa.shared.domain.model.grade.StudentGradeResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

class SchoolApiService(
    private val client: HttpClient,
    private val json: Json
) {
    private val baseUrl = "https://jwgl.suse.edu.cn"

    suspend fun getCSRFToken(): HttpResponse {
        return client.get("$baseUrl/xtgl/login_slogin.html")
    }

    suspend fun getRSAKey(): RSAKey {
        // 使用 GET 请求，与原生 Android 一致
        val response = client.get("$baseUrl/xtgl/login_getPublicKey.html") {
            parameter("time", com.suseoaa.projectoaa.shared.util.OaaClock.now().toEpochMilliseconds())
        }
        return response.body()
    }

    suspend fun login(
        timestamp: String,
        username: String,
        encryptedPassword: String,
        csrfToken: String
    ): HttpResponse {
        return client.submitForm(
            url = "$baseUrl/xtgl/login_slogin.html?time=$timestamp",
            formParameters = parameters {
                append("csrftoken", csrfToken)
                append("yhm", username)
                append("mm", encryptedPassword)
            }
        ) {
            header("Referer", "$baseUrl/xtgl/login_slogin.html")
        }
    }

    suspend fun visitUrl(url: String): HttpResponse {
        return client.get(url)
    }

    suspend fun querySchedule(year: String, semester: String): HttpResponse {
        println("[SchoolApiService] 正在发起课表查询 xskbcx_cxXsgrkb 学年=$year, 学期=$semester")
        return client.submitForm(
            url = "$baseUrl/kbcx/xskbcx_cxXsgrkb.html?gnmkdm=N2151",
            formParameters = parameters {
                append("xnm", year)
                append("xqm", semester)
                append("kzlx", "ck")
                append("xsdm", "")
                append("kclbdm", "")
                append("kclxdm", "")
            }
        ) {
            header("Accept", "*/*")
            header("X-Requested-With", "XMLHttpRequest")
            header("Origin", baseUrl)
            header("Referer", "$baseUrl/kbcx/xskbcx_cxXskbcxIndex.html?gnmkdm=N2151&layout=default")
        }
    }

    suspend fun getCalendar(): HttpResponse {
        return client.post("$baseUrl/xtgl/index_cxAreaSix.html") {
            parameter("localeKey", "zh_CN")
            parameter("gnmkdm", "index")
            header("X-Requested-With", "XMLHttpRequest")
        }
    }

    // ==================== 成绩查询 API ====================

    /**
     * 获取学生成绩
     */
    suspend fun getStudentGrade(
        year: String = "",
        semester: String = "",
        showCount: Int = 100,
        currentPage: Int = 1
    ): HttpResponse {
        return client.submitForm(
            url = "$baseUrl/cjcx/cjcx_cxDgXscj.html",
            formParameters = parameters {
                append("xnm", year)
                append("xqm", semester)
                append("queryModel.showCount", showCount.toString())
                append("queryModel.currentPage", currentPage.toString())
                append("queryModel.sortName", "")
                append("queryModel.sortOrder", "asc")
                append("_search", "false")
                append("nd", com.suseoaa.projectoaa.shared.util.OaaClock.now().toEpochMilliseconds().toString())
                append("time", "0")
            }
        ) {
            parameter("doType", "query")
            parameter("gnmkdm", "N305005")
            header("X-Requested-With", "XMLHttpRequest")
        }
    }

    /**
     * 查询单科成绩详情
     */
    suspend fun getGradeDetail(
        xnm: String,
        xqm: String,
        kcmc: String,
        jxbId: String
    ): HttpResponse {
        return client.submitForm(
            url = "$baseUrl/cjcx/cjcx_cxCjxqGjh.html",
            formParameters = parameters {
                append("xnm", xnm)
                append("xqm", xqm)
                append("kcmc", kcmc)
                append("jxb_id", jxbId)
            }
        ) {
            parameter("time", com.suseoaa.projectoaa.shared.util.OaaClock.now().toEpochMilliseconds())
            parameter("gnmkdm", "N305005")
            header("X-Requested-With", "XMLHttpRequest")
        }
    }

    // ==================== 考试信息 API ====================

    /**
     * 获取考试列表
     */
    suspend fun getExamList(
        year: String,
        semester: String,
        showCount: Int = 100,
        currentPage: Int = 1
    ): HttpResponse {
        return client.submitForm(
            url = "$baseUrl/kwgl/kscx_cxXsksxxIndex.html",
            formParameters = parameters {
                append("xnm", year)
                append("xqm", semester)
                append("queryModel.showCount", showCount.toString())
                append("queryModel.currentPage", currentPage.toString())
                append("queryModel.sortName", "")
                append("queryModel.sortOrder", "asc")
            }
        ) {
            parameter("doType", "query")
            parameter("gnmkdm", "N358105")
            header("X-Requested-With", "XMLHttpRequest")
        }
    }

    // ==================== 教务通知 API ====================

    /**
     * 获取调课通知信息
     */
    suspend fun getAcademicMessageInfo(): HttpResponse {
        return client.post("$baseUrl/xtgl/index_cxAreaThree.html") {
            parameter("localeKey", "zh_CN")
            parameter("gnmkdm", "index")
            header("X-Requested-With", "XMLHttpRequest")
        }
    }

    /**
     * 获取课程更新信息
     */
    suspend fun getAcademicCourseInfo(): HttpResponse {
        return client.post("$baseUrl/xtgl/index_cxAreaOne.html") {
            parameter("localeKey", "zh_CN")
            parameter("gnmkdm", "index")
            header("X-Requested-With", "XMLHttpRequest")
        }
    }

    // ==================== GPA 计算相关 API ====================

    /**
     * 获取专业代码列表
     */
    suspend fun getMajorList(jgId: String): HttpResponse {
        return client.submitForm(
            url = "$baseUrl/xtgl/comm_cxZydmList.html",
            formParameters = parameters {
                append("dn", "ai")
            }
        ) {
            parameter("jg_id", jgId)
            parameter("gnmkdm", "N153540")
            header("X-Requested-With", "XMLHttpRequest")
        }
    }

    /**
     * 获取专业培养计划信息
     */
    suspend fun getProfessionInfo(
        jgId: String,
        grade: String,
        majorId: String
    ): HttpResponse {
        return client.submitForm(
            url = "$baseUrl/jxzxjhgl/jxzxjhck_cxJxzxjhckIndex.html",
            formParameters = parameters {
                append("jg_id", jgId)
                append("njdm_id", grade)
                append("zyh_id", majorId)
                append("dlbs", "")
                append("currentPage_cx", "")
                append("_search", "false")
                append("nd", com.suseoaa.projectoaa.shared.util.OaaClock.now().toEpochMilliseconds().toString())
                append("queryModel.showCount", "100")
                append("queryModel.currentPage", "1")
                append("queryModel.sortName", " ")
                append("queryModel.sortOrder", "asc")
                append("time", "0")
            }
        ) {
            parameter("doType", "query")
            parameter("gnmkdm", "N153540")
            header("X-Requested-With", "XMLHttpRequest")
        }
    }

    /**
     * 获取课程列表（培养方案）- 课程信息查询
     */
    suspend fun getTeachingPlan(
        planId: String,
        suggestedYear: String = "",
        suggestedSemester: String = "",
        courseCode: String = "",
        studyType: String = "",
        showCount: Int = 1000,
        currentPage: Int = 1
    ): HttpResponse {
        return client.submitForm(
            url = "$baseUrl/jxzxjhgl/jxzxjhkcxx_cxJxzxjhkcxxIndex.html",
            formParameters = parameters {
                append("jxzxjhxx_id", planId)
                append("jyxdxnm", suggestedYear)
                append("jyxdxqm", suggestedSemester)
                append("yxxdxnm", "")
                append("yxxdxqm", "")
                append("shzt", "")
                append("kch", courseCode)
                append("xdlx", studyType)
                append("_search", "false")
                append("nd", com.suseoaa.projectoaa.shared.util.OaaClock.now().toEpochMilliseconds().toString())
                append("queryModel.showCount", showCount.toString())
                append("queryModel.currentPage", currentPage.toString())
                append("queryModel.sortName", "jyxdxnm,jyxdxqm,kch ")
                append("queryModel.sortOrder", "asc")
                append("time", "0")
            }
        ) {
            parameter("doType", "query")
            parameter("gnmkdm", "N153540")
            header("X-Requested-With", "XMLHttpRequest")
        }
    }

    // ==================== 教学执行计划查询 API ====================

    /**
     * 获取专业列表（不带学院ID参数时返回所有专业，可用于提取学院列表）
     * @param collegeId 学院ID，为空时返回所有专业
     */
    suspend fun getAllMajorList(collegeId: String = ""): HttpResponse {
        return client.get("$baseUrl/xtgl/comm_cxZydmList.html") {
            if (collegeId.isNotEmpty()) {
                parameter("jg_id", collegeId)
            }
            parameter("gnmkdm", "N153540")
            header("X-Requested-With", "XMLHttpRequest")
        }
    }

    /**
     * 获取修读要求节点下的课程列表
     * @param requirementNodeId 修读要求节点ID
     * @param nodeType 节点课程属性（1=课程，2=课程类别，3=课程归属，4=课程组）
     */
    suspend fun getStudyRequirementCourses(
        requirementNodeId: String,
        nodeType: String = "1"
    ): HttpResponse {
        return client.submitForm(
            url = "$baseUrl/jxzxjhgl/jxzxjhxfyq_cxJxzxjhxfyqKcxx.html",
            formParameters = parameters {
                append("xfyqjd_id", requirementNodeId)
                append("jdkcsx", nodeType)
            }
        ) {
            parameter("gnmkdm", "N153540")
            header("X-Requested-With", "XMLHttpRequest")
        }
    }

    /**
     * 获取修读要求树形结构
     */
    suspend fun getStudyRequirementTree(planId: String): HttpResponse {
        return client.submitForm(
            url = "$baseUrl/jxzxjhgl/jxzxjhxfyq_cxJxzxjhxfyqIndex.html",
            formParameters = parameters {
                append("jxzxjhxx_id", planId)
            }
        ) {
            parameter("gnmkdm", "N153540")
            header("X-Requested-With", "XMLHttpRequest")
        }
    }

    // ==================== 学业情况查询 API ====================

    /**
     * 获取学业情况页面（包含课程类别树形结构）
     * @param studentId 学号
     */
    suspend fun getAcademicStatusPage(studentId: String): HttpResponse {
        return client.get("$baseUrl/xsxy/xsxyqk_cxXsxyqkIndex.html") {
            parameter("gnmkdm", "N105515")
            parameter("layout", "default")
            header("X-Requested-With", "XMLHttpRequest")
        }
    }

    /**
     * 获取学业情况 - 指定类别下的课程列表（计划内课程）
     * @param categoryId 课程类别ID (xfyqjd_id)
     * @param studentId 学号
     */
    suspend fun getAcademicStatusCourses(
        categoryId: String,
        studentId: String
    ): HttpResponse {
        return client.submitForm(
            url = "$baseUrl/xsxy/xsxyqk_cxJxzxjhxfyqKcxx.html",
            formParameters = parameters {
                append("fromXh_id", "")
                append("xfyqjd_id", categoryId)
                append("xh_id", studentId)
            }
        ) {
            parameter("gnmkdm", "N105515")
            header("X-Requested-With", "XMLHttpRequest")
        }
    }

    /**
     * 获取学业情况 - 非计划内课程（如自由选修等）
     * @param categoryId 课程类别ID (xfyqjd_id)
     * @param studentId 学号
     */
    suspend fun getAcademicStatusNonPlanCourses(
        categoryId: String,
        studentId: String
    ): HttpResponse {
        return client.submitForm(
            url = "$baseUrl/xsxy/xsxyqk_cxJxzxjhxfyqFKcxx.html",
            formParameters = parameters {
                append("fromXh_id", "")
                append("xfyqjd_id", categoryId)
                append("xh_id", studentId)
            }
        ) {
            parameter("gnmkdm", "N105515")
            header("X-Requested-With", "XMLHttpRequest")
        }
    }

    /**
     * 获取学业情况 - "其它课程学分要求"节点的课程列表
     * 此节点使用特殊接口和额外参数（从HTML隐藏字段中获取）
     */
    suspend fun getAcademicStatusOtherCourses(
        studentId: String,
        cjlrxn: String = "",
        cjlrxq: String = "",
        bkcjlrxn: String = "",
        bkcjlrxq: String = "",
        xscjcxkz: String = "0",
        cjcxkzzt: String = "",
        cjztkz: String = "0",
        cjzt: String = ""
    ): HttpResponse {
        return client.submitForm(
            url = "$baseUrl/xsxy/xsxyqk_cxJxzxjhxfyqKcxx.html",
            formParameters = parameters {
                append("fromXh_id", "")
                append("xfyqjd_id", "qtkcxfyq")
                append("xh_id", studentId)
                append("cjlrxn", cjlrxn)
                append("cjlrxq", cjlrxq)
                append("bkcjlrxn", bkcjlrxn)
                append("bkcjlrxq", bkcjlrxq)
                append("xscjcxkz", xscjcxkz)
                append("cjcxkzzt", cjcxkzzt)
                append("cjztkz", cjztkz)
                append("cjzt", cjzt)
            }
        ) {
            parameter("gnmkdm", "N105515")
            header("X-Requested-With", "XMLHttpRequest")
        }
    }
}
