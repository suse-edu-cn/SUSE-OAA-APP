package com.suseoaa.projectoaa.shared.data.remote

/**
 * API 配置
 */
object ApiConfig {
    const val BASE_URL = "https://api.suseoaa.com/"
    const val SCHOOL_BASE_URL = "https://jwgl.suse.edu.cn/"
    
    // API Endpoints
    object Endpoints {
        const val LOGIN = "user/login"
        const val REGISTER = "user/register"
        const val USER_INFO = "user/Info"
        const val UPDATE_USER = "user/update"
        const val CHANGE_PASSWORD = "user/updatePassword"
        const val UPLOAD_AVATAR = "user/uploadimg"
        
        // 公告相关
        const val GET_ANNOUNCEMENT = "announcement/GetAnnouncement"
        const val UPDATE_ANNOUNCEMENT = "announcement/UpdateAnnouncement"
    }
}
