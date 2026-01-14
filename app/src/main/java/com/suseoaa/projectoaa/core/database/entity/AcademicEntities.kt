package com.suseoaa.projectoaa.core.database.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "cached_exams")
data class ExamCacheEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: String,
    val courseName: String,
    val time: String,
    val location: String
)

@Keep
@Entity(tableName = "cached_messages")
data class MessageCacheEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: String,
    val content: String,
    val date: Long = System.currentTimeMillis()
)