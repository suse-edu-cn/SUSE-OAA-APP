package com.suseoaa.projectoaa.core.database.di

import android.content.Context
import androidx.room.Room
import com.suseoaa.projectoaa.core.database.CourseDatabase
import com.suseoaa.projectoaa.core.database.dao.CourseDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideCourseDatabase(
        @ApplicationContext context: Context
    ): CourseDatabase {
        return CourseDatabase.getInstance(context)
    }

    @Provides
    fun provideCourseDao(database: CourseDatabase): CourseDao {
        return database.courseDao()
    }
}