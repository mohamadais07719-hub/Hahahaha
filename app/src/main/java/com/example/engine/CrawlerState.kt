package com.example.engine

enum class CrawlerStatus {
    IDLE,
    RUNNING,
    PAUSED,
    FINISHED
}

data class CrawlerState(
    val status: CrawlerStatus = CrawlerStatus.IDLE,
    val currentSessionId: Long = 0,
    val commentsCount: Int = 0,
    val repliesCount: Int = 0,
    val lastDetectedAuthor: String = "",
    val lastDetectedText: String = "",
    val statusMessage: String = "جاهز للبدء",
    val scrollDelayMs: Long = 1200L,
    val isAutoExpandReplies: Boolean = true
)
