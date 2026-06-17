package com.example.harulog.data.repository

import com.example.harulog.data.local.dao.DiaryDao
import com.example.harulog.data.local.dao.ExerciseStickerDao
import com.example.harulog.data.local.dao.TodoScheduleDao
import com.example.harulog.data.local.entity.DiaryEntity
import com.example.harulog.data.local.entity.ExerciseStickerEntity
import com.example.harulog.data.local.entity.TodoScheduleEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

interface DataRepository {
    // Todo & Schedules
    fun getAllTodoSchedules(): Flow<List<TodoScheduleEntity>>
    fun getTodoSchedulesByDate(date: LocalDate): Flow<List<TodoScheduleEntity>>
    fun getTodoSchedulesInRange(start: LocalDate, end: LocalDate): Flow<List<TodoScheduleEntity>>
    suspend fun insertTodoSchedule(todoSchedule: TodoScheduleEntity)
    suspend fun updateTodoSchedule(todoSchedule: TodoScheduleEntity)
    suspend fun deleteTodoSchedule(todoSchedule: TodoScheduleEntity)
    suspend fun deleteAllTodoSchedules()

    // Diaries
    fun getAllDiaries(): Flow<List<DiaryEntity>>
    fun getDiaryByDate(date: LocalDate): Flow<DiaryEntity?>
    suspend fun insertDiary(diary: DiaryEntity)
    suspend fun updateDiary(diary: DiaryEntity)
    suspend fun deleteDiary(diary: DiaryEntity)
    suspend fun deleteAllDiaries()

    // Exercise Stickers
    fun getAllExerciseStickers(): Flow<List<ExerciseStickerEntity>>
    fun getExerciseStickerByDate(date: LocalDate): Flow<ExerciseStickerEntity?>
    suspend fun insertExerciseSticker(sticker: ExerciseStickerEntity)
    suspend fun deleteExerciseSticker(sticker: ExerciseStickerEntity)
    suspend fun deleteAllExerciseStickers()
}

class DefaultDataRepository @Inject constructor(
    private val todoScheduleDao: TodoScheduleDao,
    private val diaryDao: DiaryDao,
    private val exerciseStickerDao: ExerciseStickerDao
) : DataRepository {

    override fun getAllTodoSchedules(): Flow<List<TodoScheduleEntity>> =
        todoScheduleDao.getAllTodoSchedules()

    override fun getTodoSchedulesByDate(date: LocalDate): Flow<List<TodoScheduleEntity>> =
        todoScheduleDao.getTodoSchedulesByDate(date)

    override fun getTodoSchedulesInRange(start: LocalDate, end: LocalDate): Flow<List<TodoScheduleEntity>> =
        todoScheduleDao.getTodoSchedulesInRange(start, end)

    override suspend fun insertTodoSchedule(todoSchedule: TodoScheduleEntity) =
        todoScheduleDao.insertTodoSchedule(todoSchedule)

    override suspend fun updateTodoSchedule(todoSchedule: TodoScheduleEntity) =
        todoScheduleDao.updateTodoSchedule(todoSchedule)

    override suspend fun deleteTodoSchedule(todoSchedule: TodoScheduleEntity) =
        todoScheduleDao.deleteTodoSchedule(todoSchedule)

    override suspend fun deleteAllTodoSchedules() =
        todoScheduleDao.deleteAllTodoSchedules()

    // Diaries
    override fun getAllDiaries(): Flow<List<DiaryEntity>> = diaryDao.getAllDiaries()

    override fun getDiaryByDate(date: LocalDate): Flow<DiaryEntity?> =
        diaryDao.getDiaryByDate(date)

    override suspend fun insertDiary(diary: DiaryEntity) = diaryDao.insertDiary(diary)

    override suspend fun updateDiary(diary: DiaryEntity) = diaryDao.updateDiary(diary)

    override suspend fun deleteDiary(diary: DiaryEntity) = diaryDao.deleteDiary(diary)

    override suspend fun deleteAllDiaries() = diaryDao.deleteAllDiaries()

    // Exercise Stickers
    override fun getAllExerciseStickers(): Flow<List<ExerciseStickerEntity>> =
        exerciseStickerDao.getAllExerciseStickers()

    override fun getExerciseStickerByDate(date: LocalDate): Flow<ExerciseStickerEntity?> =
        exerciseStickerDao.getExerciseStickerByDate(date)

    override suspend fun insertExerciseSticker(sticker: ExerciseStickerEntity) =
        exerciseStickerDao.insertExerciseSticker(sticker)

    override suspend fun deleteExerciseSticker(sticker: ExerciseStickerEntity) =
        exerciseStickerDao.deleteExerciseSticker(sticker)

    override suspend fun deleteAllExerciseStickers() =
        exerciseStickerDao.deleteAllExerciseStickers()
}
