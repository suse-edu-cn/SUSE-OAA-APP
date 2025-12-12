package com.suseoaa.projectoaa.startHomeNavigation.repository

import com.suseoaa.projectoaa.startHomeNavigation.viewmodel.DailyFortune

/**
 * 用户数据仓库接口
 * 修改说明：全部改为 suspend 函数，为将来接入网络请求（Retrofit）做准备。
 * 返回值目前保持基本类型，未来可根据需要改为 Result<T> 或 Flow<T>。
 */
interface UserDataRepository {
    //获取最后打卡日期
    suspend fun getLastCheckInDate(userId: String): String?

    //保存打卡日期
    // [TODO] API对接: 在实现类(Impl)中，除了保存本地 DataStore，还需调用 apiService.submitCheckIn() 上传
    suspend fun saveCheckInDate(userId: String, date: String)

    //获取打卡次数
    suspend fun getCheckInCount(userId: String): Int

    //保存打卡次数
    suspend fun saveCheckInCount(userId: String, count: Int)

    //图片缓存相关
    suspend fun getCachedImage(): Pair<String?, String?>
    suspend fun saveCachedImage(date: String, url: String)

    //[预留]手动同步数据：用于将来刷新时强制从后端拉取最新进度
    suspend fun syncUserData(userId: String): Boolean

    // [TODO] API对接: 新增获取每日运势的方法
    // 建议返回值封装为 Kotlin 的 Result 类，以便 ViewModel 处理网络错误并降级
    // suspend fun getDailyFortune(userId: String): Result<DailyFortune>
}