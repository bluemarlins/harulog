package com.example.harulog.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.harulog.data.local.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
@Entity(tableName = "exercise_sticker")
data class ExerciseStickerEntity(
    @PrimaryKey
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val isExercised: Boolean
)
