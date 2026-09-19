package com.suseoaa.projectoaa.shared.data.local.store

/**
 * 退出登录时的统一清理入口。
 *
 * 会话数据分散在三个 store 里，清理必须一起做；把顺序集中到这里，
 * 免得各调用方各清一半。主题、学期、应用设置等不随登录态清除，因此不在此列。
 *
 * 网络会话（各 HttpClient 的 Cookie）也要一并清掉，但那属于网络层——
 * 本模块位于依赖链底层，不能反向依赖同级的网络模块，所以通过
 * [clearNetworkSessions] 回调注入，由 DI 在装配时接上。
 */
class UserDataCleaner(
    private val sessionStore: SessionStore,
    private val credentialStore: CredentialStore,
    private val userProfileStore: UserProfileStore,
    private val clearNetworkSessions: suspend () -> Unit,
) {
    suspend fun clearSession() {
        sessionStore.clear()
        credentialStore.clear()
        userProfileStore.clear()
        // 不清 Cookie 的话，下一个账号会复用上一个人的会话
        clearNetworkSessions()
    }
}
