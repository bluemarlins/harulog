package com.example.harulog.ui.main

import com.example.harulog.data.local.entity.CategoryType
import com.example.harulog.data.local.entity.DiaryEntity
import com.example.harulog.data.local.entity.ExerciseStickerEntity
import com.example.harulog.data.local.entity.TodoScheduleEntity
import com.example.harulog.data.repository.DataRepository
import com.example.harulog.ui.calendar.CalendarViewMode
import com.example.harulog.ui.calendar.CalendarViewModel
import com.example.harulog.ui.todo.TodoViewModel
import com.example.harulog.utils.SelectedDateManager
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class UserScenarioTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dateManager = SelectedDateManager()
    private lateinit var fakeRepository: UserScenarioFakeDataRepository
    private lateinit var calendarViewModel: CalendarViewModel
    private lateinit var todoViewModel: TodoViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = UserScenarioFakeDataRepository()
        calendarViewModel = CalendarViewModel(fakeRepository, dateManager)
        todoViewModel = TodoViewModel(fakeRepository, dateManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun scenario1_appEntry_defaultState() = runTest {
        val collectJobCalendar = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            calendarViewModel.uiState.collect {}
        }
        val collectJobTodo = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            todoViewModel.uiState.collect {}
        }

        testScheduler.advanceUntilIdle()

        val calendarState = calendarViewModel.uiState.value
        val todoState = todoViewModel.uiState.value

        assertEquals(CalendarViewMode.DAY, calendarState.viewMode)
        assertEquals(LocalDate.now(), calendarState.selectedDate)
        assertEquals(LocalDate.now(), todoState.selectedDate)
    }

    @Test
    fun scenario2_changingViewMode_updatesFilterScopes() = runTest {
        val collectJobTodo = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            todoViewModel.uiState.collect {}
        }

        val today = LocalDate.now()
        fakeRepository.insertTodoSchedule(
            TodoScheduleEntity(
                id = 1,
                title = "Today Schedule",
                content = null,
                isTodo = false,
                eventDate = today,
                startTime = null,
                endTime = null,
                category = CategoryType.WORK
            )
        )

        testScheduler.advanceUntilIdle()

        assertEquals(CalendarViewMode.DAY, dateManager.viewMode.value)
        assertEquals(1, todoViewModel.uiState.value.schedules.size)
        assertEquals("Today Schedule", todoViewModel.uiState.value.schedules[0].title)
    }

    @Test
    fun scenario3_deterministicDateAndScopeFiltering() = runTest {
        val collectJobTodo = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            todoViewModel.uiState.collect {}
        }

        // We choose Feb 1, 2026 (Sunday) as selected date.
        val selectedDate = LocalDate.of(2026, 2, 1)
        dateManager.selectDate(selectedDate)

        // Insert schedules:
        // A: Feb 1, 2026 (Sunday) - day of selected
        val sA = TodoScheduleEntity(id = 1, title = "A", content = null, isTodo = false, eventDate = LocalDate.of(2026, 2, 1), startTime = null, endTime = null, category = CategoryType.WORK)
        // B: Feb 3, 2026 (Tuesday) - same week as selected
        val sB = TodoScheduleEntity(id = 2, title = "B", content = null, isTodo = false, eventDate = LocalDate.of(2026, 2, 3), startTime = null, endTime = null, category = CategoryType.WORK)
        // C: Feb 15, 2026 (Sunday) - same month, different week
        val sC = TodoScheduleEntity(id = 3, title = "C", content = null, isTodo = false, eventDate = LocalDate.of(2026, 2, 15), startTime = null, endTime = null, category = CategoryType.WORK)
        // D: Mar 1, 2026 (Sunday) - different month
        val sD = TodoScheduleEntity(id = 4, title = "D", content = null, isTodo = false, eventDate = LocalDate.of(2026, 3, 1), startTime = null, endTime = null, category = CategoryType.WORK)

        fakeRepository.insertTodoSchedule(sA)
        fakeRepository.insertTodoSchedule(sB)
        fakeRepository.insertTodoSchedule(sC)
        fakeRepository.insertTodoSchedule(sD)

        testScheduler.advanceUntilIdle()

        // Test 1: DAY view mode
        dateManager.setViewMode(CalendarViewMode.DAY)
        testScheduler.advanceUntilIdle()
        var currentSchedules = todoViewModel.uiState.value.schedules
        assertEquals(1, currentSchedules.size)
        assertEquals("A", currentSchedules[0].title)

        // Test 2: WEEK view mode (week contains Feb 1 to Feb 7)
        dateManager.setViewMode(CalendarViewMode.WEEK)
        testScheduler.advanceUntilIdle()
        currentSchedules = todoViewModel.uiState.value.schedules
        assertEquals(2, currentSchedules.size)
        assertTrue(currentSchedules.any { it.title == "A" })
        assertTrue(currentSchedules.any { it.title == "B" })

        // Test 3: MONTH view mode (month contains all Feb events)
        dateManager.setViewMode(CalendarViewMode.MONTH)
        testScheduler.advanceUntilIdle()
        currentSchedules = todoViewModel.uiState.value.schedules
        assertEquals(3, currentSchedules.size)
        assertTrue(currentSchedules.any { it.title == "A" })
        assertTrue(currentSchedules.any { it.title == "B" })
        assertTrue(currentSchedules.any { it.title == "C" })
    }

    @Test
    fun scenario4_dateRangeTextFormatting() = runTest {
        val collectJobTodo = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            todoViewModel.uiState.collect {}
        }

        // Select June 17, 2026
        val targetDate = LocalDate.of(2026, 6, 17)
        dateManager.selectDate(targetDate)

        // Day view
        dateManager.setViewMode(CalendarViewMode.DAY)
        testScheduler.advanceUntilIdle()
        assertEquals("6월 17일", todoViewModel.uiState.value.dateRangeText)

        // Week view
        dateManager.setViewMode(CalendarViewMode.WEEK)
        testScheduler.advanceUntilIdle()
        assertEquals("6월 14일 ~ 6월 20일", todoViewModel.uiState.value.dateRangeText)

        // Month view
        dateManager.setViewMode(CalendarViewMode.MONTH)
        testScheduler.advanceUntilIdle()
        assertEquals("6월 1일 ~ 6월 30일", todoViewModel.uiState.value.dateRangeText)
    }

    @Test
    fun scenario5_updateTodoScheduleAndReflectUI() = runTest {
        val collectJobTodo = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            todoViewModel.uiState.collect {}
        }

        // Insert initial schedule
        val initialSchedule = TodoScheduleEntity(
            id = 10,
            title = "연차",
            content = null,
            isTodo = false,
            eventDate = LocalDate.of(2026, 6, 17),
            startTime = null,
            endTime = null,
            category = CategoryType.WORK
        )
        fakeRepository.insertTodoSchedule(initialSchedule)
        testScheduler.advanceUntilIdle()

        // Set selected date and view mode
        dateManager.selectDate(LocalDate.of(2026, 6, 17))
        dateManager.setViewMode(CalendarViewMode.DAY)
        testScheduler.advanceUntilIdle()

        // Verify initial state
        assertEquals(1, todoViewModel.uiState.value.schedules.size)
        assertEquals("연차", todoViewModel.uiState.value.schedules[0].title)
        assertEquals(CategoryType.WORK, todoViewModel.uiState.value.schedules[0].category)

        // Update item to "가족 행사", Category PERSONAL
        todoViewModel.updateTodoSchedule(
            item = todoViewModel.uiState.value.schedules[0],
            title = "가족 행사",
            content = null,
            isTodo = false,
            eventDate = LocalDate.of(2026, 6, 17),
            startTime = null,
            endTime = null,
            category = CategoryType.PERSONAL,
            isMonthlyScope = false
        )
        testScheduler.advanceUntilIdle()

        // Verify updated state
        assertEquals(1, todoViewModel.uiState.value.schedules.size)
        assertEquals("가족 행사", todoViewModel.uiState.value.schedules[0].title)
        assertEquals(CategoryType.PERSONAL, todoViewModel.uiState.value.schedules[0].category)

        // Verify in fake repository too
        val repoSchedules = fakeRepository.getAllTodoSchedules().first()
        assertEquals(1, repoSchedules.size)
        assertEquals("가족 행사", repoSchedules[0].title)
        assertEquals(CategoryType.PERSONAL, repoSchedules[0].category)
    }

    @Test
    fun scenario6_monthlyDashboardSummary() = runTest {
        val collectJobTodo = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            todoViewModel.uiState.collect {}
        }

        val targetMonth = java.time.YearMonth.of(2026, 6)

        // 1. 운동 스티커 등록 (Exercised 완료 2개, 미완료 1개)
        fakeRepository.insertExerciseSticker(ExerciseStickerEntity(date = LocalDate.of(2026, 6, 15), isExercised = true))
        fakeRepository.insertExerciseSticker(ExerciseStickerEntity(date = LocalDate.of(2026, 6, 17), isExercised = false))
        fakeRepository.insertExerciseSticker(ExerciseStickerEntity(date = LocalDate.of(2026, 6, 19), isExercised = true))

        // 2. 연차 등록 (3회 등록)
        fakeRepository.insertTodoSchedule(TodoScheduleEntity(id = 101, title = "연차", content = null, isTodo = false, eventDate = LocalDate.of(2026, 6, 15), startTime = null, endTime = null, category = CategoryType.WORK))
        fakeRepository.insertTodoSchedule(TodoScheduleEntity(id = 102, title = "연차", content = null, isTodo = false, eventDate = LocalDate.of(2026, 6, 17), startTime = null, endTime = null, category = CategoryType.WORK))
        fakeRepository.insertTodoSchedule(TodoScheduleEntity(id = 103, title = "연차", content = null, isTodo = false, eventDate = LocalDate.of(2026, 6, 20), startTime = null, endTime = null, category = CategoryType.WORK))

        // 3. 장기 일정 등록
        // 6/12 ~ 6/15 크로아티아 여행 (연속 4일)
        // 6/1 ~ 6/2 워크숍 (연속 2일)
        // 6/25 회의 (연속 아님)
        fakeRepository.insertTodoSchedule(TodoScheduleEntity(id = 201, title = "크로아티아 여행", content = null, isTodo = false, eventDate = LocalDate.of(2026, 6, 12), startTime = null, endTime = null, category = CategoryType.PERSONAL))
        fakeRepository.insertTodoSchedule(TodoScheduleEntity(id = 202, title = "크로아티아 여행", content = null, isTodo = false, eventDate = LocalDate.of(2026, 6, 13), startTime = null, endTime = null, category = CategoryType.PERSONAL))
        fakeRepository.insertTodoSchedule(TodoScheduleEntity(id = 203, title = "크로아티아 여행", content = null, isTodo = false, eventDate = LocalDate.of(2026, 6, 14), startTime = null, endTime = null, category = CategoryType.PERSONAL))
        fakeRepository.insertTodoSchedule(TodoScheduleEntity(id = 204, title = "크로아티아 여행", content = null, isTodo = false, eventDate = LocalDate.of(2026, 6, 15), startTime = null, endTime = null, category = CategoryType.PERSONAL))

        fakeRepository.insertTodoSchedule(TodoScheduleEntity(id = 205, title = "워크숍", content = null, isTodo = false, eventDate = LocalDate.of(2026, 6, 1), startTime = null, endTime = null, category = CategoryType.WORK))
        fakeRepository.insertTodoSchedule(TodoScheduleEntity(id = 206, title = "워크숍", content = null, isTodo = false, eventDate = LocalDate.of(2026, 6, 2), startTime = null, endTime = null, category = CategoryType.WORK))

        fakeRepository.insertTodoSchedule(TodoScheduleEntity(id = 207, title = "회의", content = null, isTodo = false, eventDate = LocalDate.of(2026, 6, 25), startTime = null, endTime = null, category = CategoryType.WORK))

        testScheduler.advanceUntilIdle()

        // 4. 요약 데이터 조회 및 검증
        val summary = todoViewModel.getMonthlyDashboardSummary(targetMonth).first()

        assertEquals(2, summary.totalWorkoutCount)
        assertTrue(summary.workoutMotivationMessage.isNotEmpty())

        assertEquals(3, summary.annualLeaveCount)
        assertEquals(3, summary.annualLeaveDates.size)
        assertTrue(summary.annualLeaveDates.contains(LocalDate.of(2026, 6, 15)))

        assertEquals("크로아티아 여행", summary.longestScheduleTitle)
        assertEquals(4, summary.longestScheduleDuration)
        assertEquals("6/12 ~ 6/15", summary.longestSchedulePeriodText)
    }
}

private class UserScenarioFakeDataRepository : DataRepository {
    private val schedulesFlow = MutableStateFlow<List<TodoScheduleEntity>>(emptyList())
    private val diariesFlow = MutableStateFlow<List<DiaryEntity>>(emptyList())
    private val stickersFlow = MutableStateFlow<List<ExerciseStickerEntity>>(emptyList())

    override fun getAllTodoSchedules(): Flow<List<TodoScheduleEntity>> = schedulesFlow

    override fun getTodoSchedulesByDate(date: LocalDate): Flow<List<TodoScheduleEntity>> =
        schedulesFlow.map { list -> list.filter { it.eventDate == date } }

    override fun getTodoSchedulesInRange(start: LocalDate, end: LocalDate): Flow<List<TodoScheduleEntity>> =
        schedulesFlow.map { list -> list.filter { it.eventDate >= start && it.eventDate <= end } }

    override suspend fun insertTodoSchedule(todoSchedule: TodoScheduleEntity) {
        val current = schedulesFlow.value.toMutableList()
        current.add(todoSchedule)
        schedulesFlow.value = current
    }

    override suspend fun updateTodoSchedule(todoSchedule: TodoScheduleEntity) {
        val current = schedulesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == todoSchedule.id }
        if (index != -1) {
            current[index] = todoSchedule
            schedulesFlow.value = current
        }
    }

    override suspend fun deleteTodoSchedule(todoSchedule: TodoScheduleEntity) {
        schedulesFlow.value = schedulesFlow.value.filterNot { it.id == todoSchedule.id }
    }

    override suspend fun deleteAllTodoSchedules() {
        schedulesFlow.value = emptyList()
    }

    override fun getAllDiaries(): Flow<List<DiaryEntity>> = diariesFlow

    override fun getDiaryByDate(date: LocalDate): Flow<DiaryEntity?> =
        diariesFlow.map { list -> list.find { it.date == date } }

    override suspend fun insertDiary(diary: DiaryEntity) {
        val current = diariesFlow.value.toMutableList()
        current.removeAll { it.date == diary.date }
        current.add(diary)
        diariesFlow.value = current
    }

    override suspend fun updateDiary(diary: DiaryEntity) {
        insertDiary(diary)
    }

    override suspend fun deleteDiary(diary: DiaryEntity) {
        diariesFlow.value = diariesFlow.value.filterNot { it.date == diary.date }
    }

    override suspend fun deleteAllDiaries() {
        diariesFlow.value = emptyList()
    }

    override fun getAllExerciseStickers(): Flow<List<ExerciseStickerEntity>> = stickersFlow

    override fun getExerciseStickerByDate(date: LocalDate): Flow<ExerciseStickerEntity?> =
        stickersFlow.map { list -> list.find { it.date == date } }

    override suspend fun insertExerciseSticker(sticker: ExerciseStickerEntity) {
        val current = stickersFlow.value.toMutableList()
        current.removeAll { it.date == sticker.date }
        current.add(sticker)
        stickersFlow.value = current
    }

    override suspend fun deleteExerciseSticker(sticker: ExerciseStickerEntity) {
        stickersFlow.value = stickersFlow.value.filterNot { it.date == sticker.date }
    }

    override suspend fun deleteAllExerciseStickers() {
        stickersFlow.value = emptyList()
    }
}
