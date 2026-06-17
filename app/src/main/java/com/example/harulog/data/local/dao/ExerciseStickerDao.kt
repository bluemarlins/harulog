package com.example.harulog.data.local.dao

import androidx.room.*
import com.example.harulog.data.local.entity.ExerciseStickerEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ExerciseStickerDao {
    @Query("SELECT * FROM exercise_sticker ORDER BY date DESC")
    fun getAllExerciseStickers(): Flow<List<ExerciseStickerEntity>>

    @Query("SELECT * FROM exercise_sticker WHERE date = :date LIMIT 1")
    fun getExerciseStickerByDate(date: LocalDate): Flow<ExerciseStickerEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseSticker(sticker: ExerciseStickerEntity)

    @Delete
    suspend fun deleteExerciseSticker(sticker: ExerciseStickerEntity)

    @Query("DELETE FROM exercise_sticker")
    suspend fun deleteAllExerciseStickers()
}
