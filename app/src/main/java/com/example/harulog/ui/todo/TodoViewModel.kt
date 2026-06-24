package com.example.harulog.ui.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.harulog.data.local.entity.CategoryType
import com.example.harulog.data.local.entity.TodoScheduleEntity
import com.example.harulog.data.local.entity.DiaryEntity
import com.example.harulog.data.local.entity.ExerciseStickerEntity
import com.example.harulog.data.repository.DataRepository
import com.example.harulog.data.repository.DataBackupDto
import com.example.harulog.utils.SelectedDateManager
import com.example.harulog.utils.ThemeSettingsManager
import com.example.harulog.ui.calendar.CalendarViewMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import android.content.Context

data class MergedTodoScheduleItem(
    val id: Long,
    val title: String,
    val content: String?,
    val isTodo: Boolean,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val category: CategoryType,
    val isCompleted: Boolean,
    val isMonthlyScope: Boolean,
    val originalItems: List<TodoScheduleEntity>
)

data class TodoUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedCategory: CategoryType? = null,
    val schedules: List<MergedTodoScheduleItem> = emptyList(),
    val todos: List<MergedTodoScheduleItem> = emptyList(),
    val dateRangeText: String = ""
)

data class MonthlyDashboardSummary(
    val totalWorkoutCount: Int = 0,
    val workoutMotivationMessage: String = "",
    val annualLeaveCount: Int = 0,
    val annualLeaveDates: List<LocalDate> = emptyList(),
    val longestScheduleTitle: String? = null,
    val longestSchedulePeriodText: String? = null,
    val longestScheduleDuration: Int = 0,
    // 신규 필드
    val completedTodoCount: Int = 0,
    val totalTodoCount: Int = 0,
    val mostFrequentScheduleTitle: String? = null,
    val mostFrequentScheduleCount: Int = 0,
    val totalScheduleCount: Int = 0,
    val salaryDayDiff: Int = 0,   // 월급날(21일)까지 남은 일수, 음수=지남
    val meetingSchedules: List<TodoScheduleEntity> = emptyList() // 회의 일정 목록
)

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val repository: DataRepository,
    private val selectedDateManager: SelectedDateManager,
    private val themeSettingsManager: ThemeSettingsManager,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context? = null
) : ViewModel() {

    private val prefs = context?.getSharedPreferences("debug_prefs", Context.MODE_PRIVATE)
    private val _isDebugDataEnabled = MutableStateFlow(prefs?.getBoolean("debug_data_enabled", false) ?: false)
    val isDebugDataEnabled: StateFlow<Boolean> = _isDebugDataEnabled.asStateFlow()

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

        val rawSchedules: List<TodoScheduleEntity>
        val rawTodos: List<TodoScheduleEntity>

        when (viewMode) {
            CalendarViewMode.DAY -> {
                rawSchedules = filteredItems.filter { !it.isTodo && it.eventDate == selectedDate }
                rawTodos = filteredItems.filter {
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

                rawSchedules = filteredItems.filter {
                    !it.isTodo && it.eventDate >= startOfWeek && it.eventDate <= endOfWeek
                }
                rawTodos = filteredItems.filter {
                    it.isTodo && (
                        (!it.isMonthlyScope && it.eventDate >= startOfWeek && it.eventDate <= endOfWeek) ||
                        (it.isMonthlyScope && it.eventDate.year == selectedDate.year && it.eventDate.month == selectedDate.month)
                    )
                }
            }
            CalendarViewMode.MONTH -> {
                rawSchedules = filteredItems.filter {
                    !it.isTodo && it.eventDate.year == selectedDate.year && it.eventDate.month == selectedDate.month
                }
                rawTodos = filteredItems.filter {
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
            schedules = mergeConsecutiveItems(rawSchedules, viewMode),
            todos = mergeConsecutiveItems(rawTodos, viewMode),
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

    fun toggleTodo(todo: MergedTodoScheduleItem) {
        viewModelScope.launch {
            val nextState = !todo.isCompleted
            todo.originalItems.forEach {
                repository.updateTodoSchedule(it.copy(isCompleted = nextState))
            }
        }
    }

    fun updateTodoSchedule(
        item: MergedTodoScheduleItem,
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
            val oldStartDate = item.startDate
            val offsetDays = java.time.temporal.ChronoUnit.DAYS.between(oldStartDate, eventDate)
            item.originalItems.forEach { original ->
                repository.updateTodoSchedule(
                    original.copy(
                        title = title,
                        content = content,
                        isTodo = isTodo,
                        eventDate = original.eventDate.plusDays(offsetDays),
                        startTime = startTime,
                        endTime = endTime,
                        category = category,
                        isMonthlyScope = isMonthlyScope
                    )
                )
            }
        }
    }

    fun deleteTodoSchedule(item: MergedTodoScheduleItem) {
        viewModelScope.launch {
            item.originalItems.forEach {
                repository.deleteTodoSchedule(it)
            }
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

            // 4. 이번 달 완료/전체 할일 수 계산
            val monthTodos = schedules.filter {
                it.isTodo && it.eventDate.year == yearMonth.year && it.eventDate.month == yearMonth.month
            }
            val completedTodoCount = monthTodos.count { it.isCompleted }
            val totalTodoCount = monthTodos.size

            // 5. 가장 많이 반복된 일정 타이틀
            val freqMap = monthSchedules.groupBy { it.title.trim() }
                .mapValues { (_, list) -> list.map { it.eventDate }.distinct().size }
            val mostFrequent = freqMap.maxByOrNull { it.value }
            val mostFreqTitle = mostFrequent?.key
            val mostFreqCount = mostFrequent?.value ?: 0

            // 6. 회의로 추정되는 일정 추출 ("회의", "논의", "검토")
            val meetingSchedules = monthSchedules.filter {
                !it.isTodo && (it.title.contains("회의") || it.title.contains("논의") || it.title.contains("검토"))
            }.sortedBy { it.eventDate }

            Pair(
                Triple(workoutCount, motivation, Pair(leaveCount, leaveDates)),
                Triple(
                    Triple(longestTitle, periodText, maxDuration),
                    Triple(completedTodoCount, totalTodoCount, Pair(mostFreqTitle, mostFreqCount)),
                    Pair(monthSchedules.map { it.eventDate }.distinct().size, meetingSchedules)
                )
            )
        }.combine(themeSettingsManager.salaryDay) { data, salaryDayOfMonth ->
            val (part1, part2) = data
            val (workoutCount, motivation, leavePair) = part1
            val (leaveCount, leaveDates) = leavePair
            val (longestPart, todoPart, scheduleCountAndMeetings) = part2
            val (longestTitle, periodText, maxDuration) = longestPart
            val (completedTodoCount, totalTodoCount, freqPair) = todoPart
            val (mostFreqTitle, mostFreqCount) = freqPair
            val (totalScheduleCount, meetingSchedules) = scheduleCountAndMeetings

            // 6. 월급날 D-Day (설정된 일수 기준, 주말인 경우 직전 금요일로 보정)
            val today = LocalDate.now()
            val clampedDay = salaryDayOfMonth.coerceIn(1, yearMonth.lengthOfMonth())
            val originalSalaryDate = java.time.LocalDate.of(yearMonth.year, yearMonth.month, clampedDay)
            val adjustedSalaryDate = when (originalSalaryDate.dayOfWeek) {
                java.time.DayOfWeek.SATURDAY -> originalSalaryDate.minusDays(1)
                java.time.DayOfWeek.SUNDAY -> originalSalaryDate.minusDays(2)
                else -> originalSalaryDate
            }
            val salaryDiff = java.time.temporal.ChronoUnit.DAYS.between(today, adjustedSalaryDate).toInt()


            MonthlyDashboardSummary(
                totalWorkoutCount = workoutCount,
                workoutMotivationMessage = motivation,
                annualLeaveCount = leaveCount,
                annualLeaveDates = leaveDates,
                longestScheduleTitle = longestTitle,
                longestSchedulePeriodText = periodText,
                longestScheduleDuration = maxDuration,
                completedTodoCount = completedTodoCount,
                totalTodoCount = totalTodoCount,
                mostFrequentScheduleTitle = mostFreqTitle,
                mostFrequentScheduleCount = mostFreqCount,
                totalScheduleCount = totalScheduleCount,
                salaryDayDiff = salaryDiff,
                meetingSchedules = meetingSchedules
            )
        }
    }

    fun setDebugDataEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean("debug_data_enabled", enabled)?.apply()
        _isDebugDataEnabled.value = enabled
        viewModelScope.launch {
            if (enabled) {
                // 1. 더미 데이터를 주입하기 전에 현재 사용자의 실제 데이터를 임시 JSON 스냅샷으로 백업합니다.
                val snapshot = exportToJson()
                if (!snapshot.isNullOrBlank()) {
                    prefs?.edit()?.putString("temp_user_backup_json", snapshot)?.apply()
                }
                
                repository.deleteAllTodoSchedules()
                repository.deleteAllDiaries()
                repository.deleteAllExerciseStickers()
                insertDummyData()
            } else {
                repository.deleteAllTodoSchedules()
                repository.deleteAllDiaries()
                repository.deleteAllExerciseStickers()
                
                // 2. 더미 데이터를 비활성화할 때, 저장된 스냅샷이 있다면 자동으로 사용자 실제 데이터를 복원합니다.
                val tempBackup = prefs?.getString("temp_user_backup_json", null)
                if (!tempBackup.isNullOrBlank()) {
                    importFromJson(tempBackup)
                    prefs?.edit()?.remove("temp_user_backup_json")?.apply()
                }
            }
        }
    }


    private suspend fun insertDummyData() {
        val today = LocalDate.now()
        for (i in -5..4) {
            repository.insertTodoSchedule(
                TodoScheduleEntity(
                    title = "크로아티아 여행",
                    content = "가족들과 함께하는 유럽 여행",
                    isTodo = false,
                    eventDate = today.plusDays(i.toLong()),
                    startTime = null,
                    endTime = null,
                    category = CategoryType.PERSONAL
                )
            )
        }
        repository.insertTodoSchedule(
            TodoScheduleEntity(
                title = "연차",
                content = "개인 휴가 및 휴식",
                isTodo = false,
                eventDate = today.minusDays(2),
                startTime = null,
                endTime = null,
                category = CategoryType.WORK
            )
        )
        repository.insertTodoSchedule(
            TodoScheduleEntity(
                title = "연차",
                content = "개인 휴가 및 휴식",
                isTodo = false,
                eventDate = today.minusDays(1),
                startTime = null,
                endTime = null,
                category = CategoryType.WORK
            )
        )
        repository.insertTodoSchedule(
            TodoScheduleEntity(
                title = "주간 전략 회의",
                content = "상반기 실적 공유 및 하반기 계획 수립 회의",
                isTodo = false,
                eventDate = today,
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(11, 30),
                category = CategoryType.WORK
            )
        )
        repository.insertTodoSchedule(
            TodoScheduleEntity(
                title = "퇴근 후 저녁 약속",
                content = "친구와 맛집 방문 및 수다",
                isTodo = false,
                eventDate = today,
                startTime = LocalTime.of(19, 0),
                endTime = LocalTime.of(21, 0),
                category = CategoryType.PERSONAL
            )
        )
        repository.insertTodoSchedule(
            TodoScheduleEntity(
                title = "가족 식사 모임",
                content = "부모님 결혼기념일 축하 저녁 식사",
                isTodo = false,
                eventDate = today.plusDays(3),
                startTime = LocalTime.of(18, 0),
                endTime = LocalTime.of(20, 0),
                category = CategoryType.PERSONAL
            )
        )
        repository.insertTodoSchedule(
            TodoScheduleEntity(
                title = "아침 조깅 5km",
                content = "상쾌한 아침 공기 마시며 달리기",
                isTodo = true,
                eventDate = today,
                startTime = null,
                endTime = null,
                category = CategoryType.PERSONAL,
                isCompleted = true
            )
        )
        repository.insertTodoSchedule(
            TodoScheduleEntity(
                title = "프로젝트 기획안 작성 완료",
                content = "기획 요약서 및 아키텍처 다이어그램 정리",
                isTodo = true,
                eventDate = today,
                startTime = null,
                endTime = null,
                category = CategoryType.WORK,
                isCompleted = false
            )
        )
        repository.insertTodoSchedule(
            TodoScheduleEntity(
                title = "매일 물 2L 마시기",
                content = "건강 관리 프로젝트 첫 번째 실천 항목",
                isTodo = true,
                eventDate = today,
                startTime = null,
                endTime = null,
                category = CategoryType.PERSONAL,
                isCompleted = false,
                isMonthlyScope = true
            )
        )
        repository.insertTodoSchedule(
            TodoScheduleEntity(
                title = "마트 장보기",
                content = "우유, 계란, 바나나, 닭가슴살 구매",
                isTodo = true,
                eventDate = today.plusDays(1),
                startTime = null,
                endTime = null,
                category = CategoryType.PERSONAL,
                isCompleted = false
            )
        )
        repository.insertDiary(
            DiaryEntity(
                date = today.minusDays(2),
                content = "드디어 기다리던 연차 휴가 첫날! 집에서 하루 종일 밀린 잠을 자고 넷플릭스를 정주행했다. 아무것도 하지 않는 자유가 이렇게 행복할 줄이야."
            )
        )
        repository.insertDiary(
            DiaryEntity(
                date = today.minusDays(1),
                content = "연차 둘째 날. 맛있는 브런치를 해 먹고 가벼운 산책을 다녀왔다. 마음속의 스트레스가 다 날아가는 듯한 기분이다. 내일 출근도 화이팅하자."
            )
        )
        repository.insertDiary(
            DiaryEntity(
                date = today,
                content = "오늘은 주간 전략 회의가 길어져 조금 지쳤지만, 퇴근 후에 친구와 맛있는 저녁을 먹고 스트레스를 풀 수 있어서 좋았다. 하루로그에 기록을 남기며 하루를 마무리한다."
            )
        )
        repository.insertExerciseSticker(ExerciseStickerEntity(date = today.minusDays(1), isExercised = true))
        repository.insertExerciseSticker(ExerciseStickerEntity(date = today.minusDays(3), isExercised = true))
        repository.insertExerciseSticker(ExerciseStickerEntity(date = today.minusDays(6), isExercised = true))
        repository.insertExerciseSticker(ExerciseStickerEntity(date = today.minusDays(8), isExercised = true))
        repository.insertExerciseSticker(ExerciseStickerEntity(date = today.minusDays(10), isExercised = true))
    }

    fun mergeConsecutiveItems(items: List<TodoScheduleEntity>, viewMode: CalendarViewMode): List<MergedTodoScheduleItem> {
        if (viewMode == CalendarViewMode.DAY) {
            return items.map { it.toMergedItem() }
        }

        val grouped = items.groupBy { 
            GroupKey(
                title = it.title.trim(),
                content = it.content?.trim(),
                isTodo = it.isTodo,
                category = it.category,
                isMonthlyScope = it.isMonthlyScope
            )
        }

        val result = mutableListOf<MergedTodoScheduleItem>()

        for ((key, groupItems) in grouped) {
            if (key.isMonthlyScope) {
                result.addAll(groupItems.map { it.toMergedItem() })
                continue
            }

            val sorted = groupItems.sortedBy { it.eventDate }
            var currentGroup = mutableListOf<TodoScheduleEntity>()
            for (item in sorted) {
                if (currentGroup.isEmpty()) {
                    currentGroup.add(item)
                } else {
                    val lastItem = currentGroup.last()
                    if (item.eventDate == lastItem.eventDate.plusDays(1)) {
                        currentGroup.add(item)
                    } else if (item.eventDate == lastItem.eventDate) {
                        currentGroup.add(item)
                    } else {
                        result.add(createMergedItem(currentGroup))
                        currentGroup = mutableListOf(item)
                    }
                }
            }
            if (currentGroup.isNotEmpty()) {
                result.add(createMergedItem(currentGroup))
            }
        }

        return result.sortedWith(
            compareBy<MergedTodoScheduleItem> { it.startDate }
                .thenBy { it.startTime ?: LocalTime.MIN }
                .thenBy { it.title }
        )
    }

    private fun TodoScheduleEntity.toMergedItem(): MergedTodoScheduleItem {
        return MergedTodoScheduleItem(
            id = this.id,
            title = this.title,
            content = this.content,
            isTodo = this.isTodo,
            startDate = this.eventDate,
            endDate = this.eventDate,
            startTime = this.startTime,
            endTime = this.endTime,
            category = this.category,
            isCompleted = this.isCompleted,
            isMonthlyScope = this.isMonthlyScope,
            originalItems = listOf(this)
        )
    }

    private fun createMergedItem(group: List<TodoScheduleEntity>): MergedTodoScheduleItem {
        val first = group.first()
        return MergedTodoScheduleItem(
            id = first.id,
            title = first.title,
            content = first.content,
            isTodo = first.isTodo,
            startDate = group.minOf { it.eventDate },
            endDate = group.maxOf { it.eventDate },
            startTime = first.startTime,
            endTime = first.endTime,
            category = first.category,
            isCompleted = group.all { it.isCompleted },
            isMonthlyScope = first.isMonthlyScope,
            originalItems = group
        )
    }
}

data class GroupKey(
    val title: String,
    val content: String?,
    val isTodo: Boolean,
    val category: CategoryType,
    val isMonthlyScope: Boolean
)
