package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CommentItem
import com.example.data.model.ExtractionSession
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {
    @Query("SELECT * FROM sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<ExtractionSession>>

    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): ExtractionSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ExtractionSession): Long

    @Update
    suspend fun updateSession(session: ExtractionSession)

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("DELETE FROM comments WHERE sessionId = :sessionId")
    suspend fun deleteCommentsForSession(sessionId: Long)

    @Query("SELECT * FROM comments WHERE sessionId = :sessionId ORDER BY id ASC")
    fun getCommentsForSession(sessionId: Long): Flow<List<CommentItem>>

    @Query("SELECT * FROM comments WHERE sessionId = :sessionId ORDER BY id ASC")
    suspend fun getCommentsForSessionList(sessionId: Long): List<CommentItem>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertComment(comment: CommentItem): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertComments(comments: List<CommentItem>): List<Long>

    @Query("SELECT COUNT(*) FROM comments WHERE sessionId = :sessionId AND isReply = 0")
    suspend fun countComments(sessionId: Long): Int

    @Query("SELECT COUNT(*) FROM comments WHERE sessionId = :sessionId AND isReply = 1")
    suspend fun countReplies(sessionId: Long): Int
}
