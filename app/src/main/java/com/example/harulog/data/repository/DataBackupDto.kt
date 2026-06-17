package com.example.harulog.data.repository

import com.example.harulog.data.local.entity.DiaryEntity
import com.example.harulog.data.local.entity.ExerciseStickerEntity
import com.example.harulog.data.local.entity.TodoScheduleEntity
import kotlinx.serialization.Serializable

@Serializable
data class DataBackupDto(
    val todoSchedules: List<TodoScheduleEntity> = emptyList(),
    val diaries: List<DiaryEntity> = emptyList(),
    val stickers: List<ExerciseStickerEntity> = emptyList()
)
