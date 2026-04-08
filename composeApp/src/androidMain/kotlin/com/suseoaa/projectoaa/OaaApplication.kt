package com.suseoaa.projectoaa

import android.app.Application
import com.suseoaa.projectoaa.di.appModule
import com.suseoaa.projectoaa.di.platformModule
import com.suseoaa.projectoaa.reminder.CourseReminderScheduler
import com.suseoaa.projectoaa.shared.di.getSharedModules
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
                getSharedModules() + listOf(
                    platformModule(),
                    appModule
                )
            )
        }

        CourseReminderScheduler.scheduleNextReminder(this)
    }
}
