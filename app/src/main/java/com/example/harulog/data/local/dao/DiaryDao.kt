package com.example.harulog.data.local.dao

import androidx.room.*
import com.example.harulog.data.local.entity.DiaryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary ORDER BY date DESC")
    fun getAllDiaries(): Flow<List<DiaryEntity>>

    @Query("SELECT * FROM diary WHERE date = :date LIMIT 1")
    fun getDiaryByDate(date: LocalDate): Flow<DiaryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiary(diary: DiaryEntity)

    @Update
    suspend fun updateDiary(diary: DiaryEntity)

    @Delete
    suspend fun deleteDiary(diary: DiaryEntity)

    @Query("DELETE FROM diary")
    suspend fun deleteAllDiaries()
}
