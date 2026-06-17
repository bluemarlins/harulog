package com.example.harulog.di

import android.content.Context
import com.example.harulog.data.local.AppDatabase
import com.example.harulog.data.local.dao.DiaryDao
import com.example.harulog.data.local.dao.ExerciseStickerDao
import com.example.harulog.data.local.dao.TodoScheduleDao
import com.example.harulog.data.repository.DataRepository
import com.example.harulog.data.repository.DefaultDataRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDataRepository(
        defaultDataRepository: DefaultDataRepository
    ): DataRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideTodoScheduleDao(database: AppDatabase): TodoScheduleDao {
        return database.todoScheduleDao()
    }

    @Provides
    fun provideDiaryDao(database: AppDatabase): DiaryDao {
        return database.diaryDao()
    }

    @Provides
    fun provideExerciseStickerDao(database: AppDatabase): ExerciseStickerDao {
        return database.exerciseStickerDao()
    }
}
