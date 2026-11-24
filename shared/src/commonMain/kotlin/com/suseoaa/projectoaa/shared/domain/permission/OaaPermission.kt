package com.suseoaa.projectoaa.shared.domain.permission

import com.suseoaa.projectoaa.shared.domain.model.person.CurrentUser
import com.suseoaa.projectoaa.shared.domain.model.person.UserListItem
import com.suseoaa.projectoaa.shared.domain.model.org.Role

/**
 * 按职位等级（level）判断界面上要不要露出某个入口。
 *
 * 阈值取自 v2 接口文档：公告可见范围以 30 / 80 分界，部门、职位、周期的维护要求
 * level >= 80。以前这些判断是按职位名称写死在各个 ViewModel 里的（「部长」「会长」
 * ……），新增或改名一个职位就得改代码；现在只看等级。
 *
 * 这里只决定「显示不显示」，真正的权限由后端校验，后端拒绝时界面会提示原因。
 */
object OaaPermission {

    /** 部长、副部长起：管理本部门公告，参与面试审核，进入成员管理。 */
    const val DEPARTMENT_MANAGER_LEVEL = 30

    /** 副会长及以上：跨部门管理公告，维护部门、职位、招新周期。 */
    const val ADMIN_LEVEL = 80

    fun isAdmin(user: CurrentUser?): Boolean = (user?.level ?: 0) >= ADMIN_LEVEL

    fun isDepartmentManager(user: CurrentUser?): Boolean =
        (user?.level ?: 0) >= DEPARTMENT_MANAGER_LEVEL

    /** 管理员可以管理任意部门的公告，部门负责人只能管理自己部门的。 */
    fun canManageAnnouncements(user: CurrentUser?, departmentName: String): Boolean {
        if (user == null) return false
        if (isAdmin(user)) return true
        return isDepartmentManager(user) && user.person.department == departmentName
    }

    fun canAccessUserManagement(user: CurrentUser?): Boolean = isDepartmentManager(user)

    /** 只能调整等级比自己低的成员。 */
    fun canEditUser(user: CurrentUser?, targetLevel: Int): Boolean =
        user != null && isDepartmentManager(user) && user.level > targetLevel

    fun canManageOrganization(user: CurrentUser?): Boolean = isAdmin(user)

    fun canManageApps(user: CurrentUser?): Boolean = isAdmin(user)

    fun canManageTerms(user: CurrentUser?): Boolean =
        isAdmin(user) && user?.person?.isGraduated != true

    fun canEditGrades(user: CurrentUser?): Boolean = isAdmin(user)

    /** 毕业成员只能任命普通会员；已有开发者可保留其角色及开发权限。 */
    fun canAssignRole(target: UserListItem, role: Role): Boolean =
        target.isGraduated != true || role.level <= 10 ||
            (target.role == "开发者" && role.name == target.role)

    /**
     * 当前用户拥有面试官身份或属于部门负责人及以上，且未毕业时可进入面试审核。
     */
    fun canReviewApplications(user: CurrentUser?): Boolean =
        user != null && user.person.isGraduated != true &&
            (user.person.isInterviewer == true || isDepartmentManager(user))
}
