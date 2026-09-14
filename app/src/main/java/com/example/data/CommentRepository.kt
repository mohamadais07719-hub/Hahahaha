package com.example.data

import com.example.data.dao.CommentDao
import com.example.data.model.CommentItem
import com.example.data.model.ExtractionSession
import kotlinx.coroutines.flow.Flow

class CommentRepository(private val commentDao: CommentDao) {

    val allSessions: Flow<List<ExtractionSession>> = commentDao.getAllSessions()

    fun getCommentsForSession(sessionId: Long): Flow<List<CommentItem>> =
        commentDao.getCommentsForSession(sessionId)

    suspend fun getCommentsForSessionList(sessionId: Long): List<CommentItem> =
        commentDao.getCommentsForSessionList(sessionId)

    suspend fun createSession(title: String, preview: String = ""): Long {
        val session = ExtractionSession(
            title = title,
            postPreview = preview,
            timestamp = System.currentTimeMillis(),
            commentCount = 0,
            replyCount = 0
        )
        return commentDao.insertSession(session)
    }

    suspend fun updateSessionCounts(sessionId: Long, comments: Int, replies: Int) {
        val session = commentDao.getSessionById(sessionId)
        if (session != null) {
            commentDao.updateSession(
                session.copy(
                    commentCount = comments,
                    replyCount = replies
                )
            )
        }
    }

    suspend fun saveComment(comment: CommentItem): Long {
        return commentDao.insertComment(comment)
    }

    suspend fun saveComments(comments: List<CommentItem>): List<Long> {
        return commentDao.insertComments(comments)
    }

    suspend fun deleteSession(sessionId: Long) {
        commentDao.deleteCommentsForSession(sessionId)
        commentDao.deleteSession(sessionId)
    }
}
