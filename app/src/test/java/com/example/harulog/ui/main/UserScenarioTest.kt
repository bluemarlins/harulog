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
