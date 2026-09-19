package com.suseoaa.projectoaa.shared.domain.model.school

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RSAKey(
    @SerialName("modulus") val modulus: String,
    @SerialName("exponent") val exponent: String
)

@Serializable
data class CourseResponseJson(
    @SerialName("kbList") val kbList: List<CourseItem>? = null,
    // 整周实践课（实习、集中实践环节），它们不在课表格子里，教务系统单独放一个列表
    @SerialName("sjkList") val sjkList: List<PracticeCourseItem>? = null,
    @SerialName("xsxx") val xsxx: StudentInfo? = null
)

@Serializable
data class StudentInfo(
    @SerialName("BJMC") val className: String? = null,
    @SerialName("NJDM_ID") val njdmId: String? = null,
    @SerialName("XH") val studentNo: String? = null,
    @SerialName("XM") val name: String? = null,
    @SerialName("XNM") val xnm: String? = null,
    @SerialName("XQM") val xqm: String? = null,
    @SerialName("ZYH_ID") val zyhId: String? = null,
    @SerialName("ZYMC") val major: String? = null
)

@Serializable
data class CourseItem(
    @SerialName("kch_id") val kchId: String? = null,
    @SerialName("kcmc") val kcmc: String? = null,
    @SerialName("xnm") val xnm: String? = null,
    @SerialName("xqm") val xqm: String? = null,
    @SerialName("xqj") val xqj: String? = null,
    @SerialName("jcs") val jcs: String? = null,
    @SerialName("zcd") val zcd: String? = null,
    @SerialName("cdmc") val cdmc: String? = null,
    @SerialName("xm") val xm: String? = null,
    @SerialName("kcxzmc") val kcxzmc: String? = null,
    @SerialName("kclbmc") val kclbmc: String? = null,
    @SerialName("kclb") val kclb: String? = null,
    @SerialName("khfsmc") val khfsmc: String? = null,
    @SerialName("xf") val xf: String? = null,
    @SerialName("xqmc") val xqmc: String? = null,
    @SerialName("kkxy") val kkxy: String? = null,
    @SerialName("jxbmc") val jxbmc: String? = null,
    @SerialName("jxbzc") val jxbzc: String? = null
)

/**
 * 整周实践课，如 `IT项目实习 赵良军 19-20周`。
 *
 * 与 [CourseItem] 不同，它没有 `xqj`(星期) 和 `jcs`(节次)——整周进行，不落在课表格子上；
 * 也没有 `xnm`/`xqm`，学年学期要用外层的学生信息补。
 */
@Serializable
data class PracticeCourseItem(
    @SerialName("kcmc") val kcmc: String? = null,
    @SerialName("jsxm") val jsxm: String? = null,      // 教师姓名
    @SerialName("jxbzh") val jxbzh: String? = null,    // 教学班
    @SerialName("qsjsz") val qsjsz: String? = null,    // 起始结束周，如 "19-20周"
    @SerialName("xf") val xf: String? = null,          // 学分
    @SerialName("khfsmc") val khfsmc: String? = null,  // 考核方式
    @SerialName("xqmc") val xqmc: String? = null,      // 校区
    @SerialName("kclb") val kclb: String? = null       // 课程类别
)
