package com.example.harulog.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.harulog.data.local.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
@Entity(tableName = "diary")
data class DiaryEntity(
    @PrimaryKey
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate, // 1일 1기록 원칙으로 날짜를 기본키로 지정
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
