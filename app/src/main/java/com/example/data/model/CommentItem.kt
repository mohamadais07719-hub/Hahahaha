package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "comments",
    indices = [
        Index(value = ["sessionId", "signature"], unique = true),
        Index(value = ["sessionId"])
    ]
)
data class CommentItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val author: String,
    val content: String,
    val isReply: Boolean = false,
    val parentAuthor: String = "",
    val parentCommentSignature: String = "",
    val signature: String, // deduplication key: author + "|||" + content
    val timestamp: Long = System.currentTimeMillis()
)
