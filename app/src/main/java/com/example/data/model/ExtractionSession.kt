package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class ExtractionSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val postPreview: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val commentCount: Int = 0,
    val replyCount: Int = 0
)
