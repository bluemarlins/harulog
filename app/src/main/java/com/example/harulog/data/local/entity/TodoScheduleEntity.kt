package com.example.harulog.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.harulog.data.local.LocalDateSerializer
import com.example.harulog.data.local.LocalTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime

@Serializable
@Entity(tableName = "todo_schedule")
data class TodoScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String?,
    val isTodo: Boolean,             // true: 할 일, false: 일정
    @Serializable(with = LocalDateSerializer::class) val eventDate: LocalDate,        // 일정 날짜 또는 할 일 수행일
    @Serializable(with = LocalTimeSerializer::class) val startTime: LocalTime?,       // 일정 시작 시간 (할 일은 null 가능)
    @Serializable(with = LocalTimeSerializer::class) val endTime: LocalTime?,         // 일정 종료 시간 (할 일은 null 가능)
    val category: CategoryType,      // WORK 또는 PERSONAL
    val isCompleted: Boolean = false, // 할 일 완료 여부
    val isMonthlyScope: Boolean = false // true: 이번 달 안에 해야 하는 것 (날짜 지정 무관)
)
