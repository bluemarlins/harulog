package com.example.harulog.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.harulog.data.repository.DataRepository
import com.example.harulog.data.local.entity.TodoScheduleEntity
import com.example.harulog.data.local.entity.ExerciseStickerEntity
import com.example.harulog.utils.SelectedDateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

enum class CalendarViewMode { MONTH, WEEK, DAY }

data class CalendarUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val currentMonth: YearMonth = YearMonth.now(),
    val viewMode: CalendarViewMode = CalendarViewMode.MONTH,
    val allTodoSchedules: List<TodoScheduleEntity> = emptyList(),
    val allStickers: List<ExerciseStickerEntity> = emptyList()
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: DataRepository,
    private val selectedDateManager: SelectedDateManager
) : ViewModel() {

    val uiState: StateFlow<CalendarUiState> = combine(
        selectedDateManager.selectedDate,
        selectedDateManager.viewMode,
        repository.getAllTodoSchedules(),
        repository.getAllExerciseStickers()
    ) { selectedDate, viewMode, todoSchedules, stickers ->
        CalendarUiState(
            selectedDate = selectedDate,
            currentMonth = YearMonth.from(selectedDate),
            viewMode = viewMode,
            allTodoSchedules = todoSchedules,
            allStickers = stickers
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState()
    )

    fun selectDate(date: LocalDate) {
        selectedDateManager.selectDate(date)
    }

    fun selectMonth(month: YearMonth) {
        val currentSelected = selectedDateManager.selectedDate.value
        if (currentSelected.year != month.year || currentSelected.month != month.month) {
            selectedDateManager.selectDate(month.atDay(1))
        }
    }

    fun setViewMode(mode: CalendarViewMode) {
        selectedDateManager.setViewMode(mode)
    }

    fun toggleWorkout(date: LocalDate) {
        viewModelScope.launch {
            val stickers = uiState.value.allStickers
            val existing = stickers.find { it.date == date }
            if (existing != null) {
                repository.deleteExerciseSticker(existing)
            } else {
                repository.insertExerciseSticker(ExerciseStickerEntity(date = date, isExercised = true))
            }
        }
    }
}
