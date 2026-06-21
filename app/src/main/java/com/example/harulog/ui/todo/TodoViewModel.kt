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

data class MonthlyDashboardSummary(
    val totalWorkoutCount: Int = 0,
    val workoutMotivationMessage: String = "",
    val annualLeaveCount: Int = 0,
    val annualLeaveDates: List<LocalDate> = emptyList(),
    val longestScheduleTitle: String? = null,
    val longestSchedulePeriodText: String? = null,
    val longestScheduleDuration: Int = 0
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

    fun getMonthlyDashboardSummary(yearMonth: java.time.YearMonth): Flow<MonthlyDashboardSummary> {
        return combine(
            repository.getAllExerciseStickers(),
            repository.getAllTodoSchedules()
        ) { stickers, schedules ->
            // 1. 당월 운동 완료 횟수 계산 (isExercised가 true인 스티커)
            val monthStickers = stickers.filter {
                it.isExercised && it.date.year == yearMonth.year && it.date.month == yearMonth.month
            }
            val workoutCount = monthStickers.size

            // 동기부여 문구 결정
            val motivation = when {
                workoutCount == 0 -> "이번 달엔 아직 운동 기록이 없네요. 가볍게 시작해보는 건 어떨까요?"
                workoutCount in 1..3 -> "시작이 반이에요! 다음 운동도 화이팅!"
                workoutCount in 4..10 -> "꾸준한 운동은 삶에 큰 활력을 줍니다. 아주 멋져요!"
                else -> "진정한 운동 마스터! 당신의 끈기에 박수를 보냅니다!"
            }

            // 2. 연차 사용 횟수 (제목이 "연차"인 일정)
            val monthLeaves = schedules.filter {
                !it.isTodo && it.title.trim() == "연차" &&
                it.eventDate.year == yearMonth.year && it.eventDate.month == yearMonth.month
            }.sortedBy { it.eventDate }

            val leaveCount = monthLeaves.size
            val leaveDates = monthLeaves.map { it.eventDate }

            // 3. 가장 긴 구간 일정 계산
            val monthSchedules = schedules.filter {
                !it.isTodo && it.eventDate.year == yearMonth.year && it.eventDate.month == yearMonth.month
            }

            var maxDuration = 0
            var longestTitle: String? = null
            var periodText: String? = null

            // 타이틀별로 그룹화
            val grouped = monthSchedules.groupBy { it.title.trim() }
            for ((title, list) in grouped) {
                val dates = list.map { it.eventDate }.distinct().sorted()
                if (dates.isEmpty()) continue

                // 연속된 날짜 구간 구하기
                var currentStart = dates[0]
                var currentPrev = dates[0]
                var currentLen = 1

                var bestStart = dates[0]
                var bestEnd = dates[0]
                var bestLen = 1

                for (i in 1 until dates.size) {
                    val d = dates[i]
                    if (d == currentPrev.plusDays(1)) {
                        currentLen++
                        currentPrev = d
                    } else {
                        if (currentLen > bestLen) {
                            bestLen = currentLen
                            bestStart = currentStart
                            bestEnd = currentPrev
                        }
                        currentStart = d
                        currentPrev = d
                        currentLen = 1
                    }
                }
                if (currentLen > bestLen) {
                    bestLen = currentLen
                    bestStart = currentStart
                    bestEnd = currentPrev
                }

                // 2일 이상 연속된 일정 중 최장 기간인 것 선택
                if (bestLen >= 2 && bestLen > maxDuration) {
                    maxDuration = bestLen
                    longestTitle = title
                    val formatter = DateTimeFormatter.ofPattern("M/d")
                    periodText = "${bestStart.format(formatter)} ~ ${bestEnd.format(formatter)}"
                }
            }

            MonthlyDashboardSummary(
                totalWorkoutCount = workoutCount,
                workoutMotivationMessage = motivation,
                annualLeaveCount = leaveCount,
                annualLeaveDates = leaveDates,
                longestScheduleTitle = longestTitle,
                longestSchedulePeriodText = periodText,
                longestScheduleDuration = maxDuration
            )
        }
    }
}
