package com.example.harulog.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.harulog.data.local.dao.DiaryDao
import com.example.harulog.data.local.dao.ExerciseStickerDao
import com.example.harulog.data.local.dao.TodoScheduleDao
import com.example.harulog.data.local.entity.DiaryEntity
import com.example.harulog.data.local.entity.ExerciseStickerEntity
import com.example.harulog.data.local.entity.TodoScheduleEntity

@Database(
    entities = [
        TodoScheduleEntity::class,
        DiaryEntity::class,
        ExerciseStickerEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoScheduleDao(): TodoScheduleDao
    abstract fun diaryDao(): DiaryDao
    abstract fun exerciseStickerDao(): ExerciseStickerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "harulog_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
