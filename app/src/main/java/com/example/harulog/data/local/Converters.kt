package com.example.harulog.data.local

import androidx.room.TypeConverter
import com.example.harulog.data.local.entity.CategoryType
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class Converters {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? {
        return value?.format(dateFormatter)
    }

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it, dateFormatter) }
    }

    @TypeConverter
    fun fromLocalTime(value: LocalTime?): String? {
        return value?.format(timeFormatter)
    }

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? {
        return value?.let { LocalTime.parse(it, timeFormatter) }
    }

    @TypeConverter
    fun fromCategoryType(value: CategoryType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toCategoryType(value: String?): CategoryType? {
        return value?.let { CategoryType.valueOf(it) }
    }
}
