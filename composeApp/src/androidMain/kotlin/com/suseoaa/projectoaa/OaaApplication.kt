package com.suseoaa.projectoaa

import android.app.Application
import com.suseoaa.projectoaa.di.appModule
import com.suseoaa.projectoaa.di.platformModule
// import com.suseoaa.projectoaa.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class OaaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@OaaApplication)
            modules(
                // sharedModule, // 暂时注释掉，如果需要再打开
                platformModule(), // 使用函数调用，加载 composeApp 的 platformModule
                appModule
            )
        }
    }
}
