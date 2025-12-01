package com.suseoaa.projectoaa.navigation.repository.detail

import com.suseoaa.projectoaa.navigation.viewmodel.DetailBlock

/**
 * 详情页数据仓库的 "契约" (Interface)。
 * ViewModel 只依赖这个接口，不关心实现是 Fake 还是 Real。
 */
interface DetailRepository {

    /**
     * 从数据源获取任务详情
     * @param token 用户的认证Token
     * @param taskId 任务ID
     * @return Result 包含一个 Pair (标题, 信息块列表)
     */
    suspend fun getTaskDetails(token: String, taskId: String): Result<Pair<String, List<DetailBlock>>>
}