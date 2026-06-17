package com.example.harulog.data.local.dao

import androidx.room.*
import com.example.harulog.data.local.entity.TodoScheduleEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TodoScheduleDao {
    @Query("SELECT * FROM todo_schedule ORDER BY eventDate ASC, startTime ASC")
    fun getAllTodoSchedules(): Flow<List<TodoScheduleEntity>>

    @Query("SELECT * FROM todo_schedule WHERE eventDate = :date ORDER BY startTime ASC")
    fun getTodoSchedulesByDate(date: LocalDate): Flow<List<TodoScheduleEntity>>

    @Query("SELECT * FROM todo_schedule WHERE eventDate >= :start AND eventDate <= :end ORDER BY eventDate ASC, startTime ASC")
    fun getTodoSchedulesInRange(start: LocalDate, end: LocalDate): Flow<List<TodoScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodoSchedule(todoSchedule: TodoScheduleEntity)

    @Update
    suspend fun updateTodoSchedule(todoSchedule: TodoScheduleEntity)

    @Delete
    suspend fun deleteTodoSchedule(todoSchedule: TodoScheduleEntity)

    @Query("DELETE FROM todo_schedule")
    suspend fun deleteAllTodoSchedules()
}
