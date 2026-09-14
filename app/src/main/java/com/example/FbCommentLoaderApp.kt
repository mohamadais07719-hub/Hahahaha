package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.CommentRepository
import com.example.engine.ExtractionController

class FbCommentLoaderApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: CommentRepository
        private set

    lateinit var extractionController: ExtractionController
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        repository = CommentRepository(database.commentDao())
        extractionController = ExtractionController(repository)
    }

    companion object {
        lateinit var instance: FbCommentLoaderApp
            private set
    }
}
