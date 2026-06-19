package com.example.harulog.ui.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.harulog.data.local.entity.CategoryType
import com.example.harulog.data.local.entity.TodoScheduleEntity
import com.example.harulog.data.repository.DataRepository
import com.example.harulog.data.repository.DataBackupDto
import com.example.harulog.utils.SelectedDateManager
import com.example.harulog.ui.calendar.CalendarViewMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class TodoUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedCategory: CategoryType? = null,
    val schedules: List<TodoScheduleEntity> = emptyList(),
    val todos: List<TodoScheduleEntity> = emptyList(),
    val dateRangeText: String = ""
)

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val repository: DataRepository,
    private val selectedDateManager: SelectedDateManager
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<CategoryType?>(null)
    val selectedCategory: StateFlow<CategoryType?> = _selectedCategory.asStateFlow()

    // Backup state combined in background to allow synchronous backup exports
    private val _backupData = combine(
        repository.getAllTodoSchedules(),
        repository.getAllDiaries(),
        repository.getAllExerciseStickers()
    ) { todoSchedules, diaries, stickers ->
        DataBackupDto(todoSchedules, diaries, stickers)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DataBackupDto())

    val uiState: StateFlow<TodoUiState> = combine(
        selectedDateManager.selectedDate,
        selectedDateManager.viewMode,
        _selectedCategory,
        repository.getAllTodoSchedules()
    ) { selectedDate, viewMode, category, allItems ->
        val filteredItems = if (category == null) {
            allItems
        } else {
            allItems.filter { it.category == category }
        }

        val schedules: List<TodoScheduleEntity>
        val todos: List<TodoScheduleEntity>

        when (viewMode) {
            CalendarViewMode.DAY -> {
                schedules = filteredItems.filter { !it.isTodo && it.eventDate == selectedDate }
                todos = filteredItems.filter {
                    it.isTodo && (
                        (!it.isMonthlyScope && it.eventDate == selectedDate) ||
                        (it.isMonthlyScope && it.eventDate.year == selectedDate.year && it.eventDate.month == selectedDate.month)
                    )
                }
            }
            CalendarViewMode.WEEK -> {
                val dayOfWeek = selectedDate.dayOfWeek.value % 7
                val startOfWeek = selectedDate.minusDays(dayOfWeek.toLong())
                val endOfWeek = startOfWeek.plusDays(6)

                schedules = filteredItems.filter {
                    !it.isTodo && it.eventDate >= startOfWeek && it.eventDate <= endOfWeek
                }
                todos = filteredItems.filter {
                    it.isTodo && (
                        (!it.isMonthlyScope && it.eventDate >= startOfWeek && it.eventDate <= endOfWeek) ||
                        (it.isMonthlyScope && it.eventDate.year == selectedDate.year && it.eventDate.month == selectedDate.month)
                    )
                }
            }
            CalendarViewMode.MONTH -> {
                schedules = filteredItems.filter {
                    !it.isTodo && it.eventDate.year == selectedDate.year && it.eventDate.month == selectedDate.month
                }
                todos = filteredItems.filter {
                    it.isTodo && it.eventDate.year == selectedDate.year && it.eventDate.month == selectedDate.month
                }
            }
        }

        val dateRangeText = when (viewMode) {
            CalendarViewMode.DAY -> {
                selectedDate.format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN))
            }
            CalendarViewMode.WEEK -> {
                val dayOfWeek = selectedDate.dayOfWeek.value % 7
                val startOfWeek = selectedDate.minusDays(dayOfWeek.toLong())
                val endOfWeek = startOfWeek.plusDays(6)
                "${startOfWeek.format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN))} ~ ${endOfWeek.format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN))}"
            }
            CalendarViewMode.MONTH -> {
                val lastDay = selectedDate.lengthOfMonth()
                "${selectedDate.monthValue}월 1일 ~ ${selectedDate.monthValue}월 ${lastDay}일"
            }
        }

        TodoUiState(
            selectedDate = selectedDate,
            selectedCategory = category,
            schedules = schedules,
            todos = todos,
            dateRangeText = dateRangeText
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodoUiState()
    )

    fun selectDate(date: LocalDate) {
        selectedDateManager.selectDate(date)
    }

    /** 대시보드 화면 전용: 필터링 없는 전체 할 일 및 일정 목록을 반환합니다. */
    fun getAllTodosForDashboard(): List<TodoScheduleEntity> = _backupData.value.todoSchedules

    fun setCategoryFilter(category: CategoryType?) {
        _selectedCategory.value = category
    }

    fun addTodoSchedule(
        title: String,
        content: String?,
        isTodo: Boolean,
        eventDate: LocalDate,
        startTime: LocalTime?,
        endTime: LocalTime?,
        category: CategoryType,
        isMonthlyScope: Boolean = false
    ) {
        viewModelScope.launch {
            repository.insertTodoSchedule(
                TodoScheduleEntity(
                    title = title,
                    content = content,
                    isTodo = isTodo,
                    eventDate = eventDate,
                    startTime = startTime,
                    endTime = endTime,
                    category = category,
                    isCompleted = false,
                    isMonthlyScope = isMonthlyScope
                )
            )
        }
    }

    fun toggleTodo(todo: TodoScheduleEntity) {
        viewModelScope.launch {
            repository.updateTodoSchedule(todo.copy(isCompleted = !todo.isCompleted))
        }
    }

    fun updateTodoSchedule(
        item: TodoScheduleEntity,
        title: String,
        content: String?,
        isTodo: Boolean,
        eventDate: LocalDate,
        startTime: LocalTime?,
        endTime: LocalTime?,
        category: CategoryType,
        isMonthlyScope: Boolean
    ) {
        viewModelScope.launch {
            repository.updateTodoSchedule(
                item.copy(
                    title = title,
                    content = content,
                    isTodo = isTodo,
                    eventDate = eventDate,
                    startTime = startTime,
                    endTime = endTime,
                    category = category,
                    isMonthlyScope = isMonthlyScope
                )
            )
        }
    }

    fun deleteTodoSchedule(item: TodoScheduleEntity) {
        viewModelScope.launch {
            repository.deleteTodoSchedule(item)
        }
    }

    fun exportToJson(): String? {
        return try {
            val format = kotlinx.serialization.json.Json { prettyPrint = true }
            format.encodeToString(DataBackupDto.serializer(), _backupData.value)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun importFromJson(jsonString: String): Boolean {
        return try {
            val format = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            val dataBackup = format.decodeFromString<DataBackupDto>(jsonString)
            viewModelScope.launch {
                repository.deleteAllTodoSchedules()
                repository.deleteAllDiaries()
                repository.deleteAllExerciseStickers()

                dataBackup.todoSchedules.forEach { repository.insertTodoSchedule(it) }
                dataBackup.diaries.forEach { repository.insertDiary(it) }
                dataBackup.stickers.forEach { repository.insertExerciseSticker(it) }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
