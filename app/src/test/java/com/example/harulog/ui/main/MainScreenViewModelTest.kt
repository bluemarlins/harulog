package com.example.harulog.ui.main

import com.example.harulog.data.local.entity.CategoryType
import com.example.harulog.data.local.entity.DiaryEntity
import com.example.harulog.data.local.entity.ExerciseStickerEntity
import com.example.harulog.data.local.entity.TodoScheduleEntity
import com.example.harulog.data.repository.DataRepository
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
class MainScreenViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dateManager = SelectedDateManager()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_initiallyLoads() = runTest {
        val fakeRepository = FakeDataRepository()
        val themeSettingsManager = org.mockito.Mockito.mock(com.example.harulog.utils.ThemeSettingsManager::class.java)
        org.mockito.Mockito.`when`(themeSettingsManager.themeMode).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(com.example.harulog.utils.ThemeMode.SYSTEM))
        org.mockito.Mockito.`when`(themeSettingsManager.salaryDay).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(21))
        val viewModel = TodoViewModel(fakeRepository, dateManager, themeSettingsManager)
        
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        
        testScheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertNotNull(state)
    }

    @Test
    fun uiState_addingTodo_updatesTodos() = runTest {
        val fakeRepository = FakeDataRepository()
        val themeSettingsManager = org.mockito.Mockito.mock(com.example.harulog.utils.ThemeSettingsManager::class.java)
        org.mockito.Mockito.`when`(themeSettingsManager.themeMode).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(com.example.harulog.utils.ThemeMode.SYSTEM))
        org.mockito.Mockito.`when`(themeSettingsManager.salaryDay).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(21))
        val viewModel = TodoViewModel(fakeRepository, dateManager, themeSettingsManager)
        
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        
        val today = LocalDate.now()
        viewModel.addTodoSchedule(
            title = "Test Todo",
            content = null,
            isTodo = true,
            eventDate = today,
            startTime = null,
            endTime = null,
            category = CategoryType.WORK,
            isMonthlyScope = false
        )
        
        testScheduler.advanceUntilIdle()
        
        val schedules = fakeRepository.getAllTodoSchedules().first()
        assertEquals(1, schedules.size)
        assertEquals("Test Todo", schedules[0].title)
        assertEquals(CategoryType.WORK, schedules[0].category)
        assertTrue(schedules[0].isTodo)
    }

    @Test
    fun test_jsonBackupAndRestore() = runTest {
        val fakeRepository = FakeDataRepository()
        val themeSettingsManager = org.mockito.Mockito.mock(com.example.harulog.utils.ThemeSettingsManager::class.java)
        org.mockito.Mockito.`when`(themeSettingsManager.themeMode).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(com.example.harulog.utils.ThemeMode.SYSTEM))
        org.mockito.Mockito.`when`(themeSettingsManager.salaryDay).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(21))
        val viewModel = TodoViewModel(fakeRepository, dateManager, themeSettingsManager)

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val today = LocalDate.now()
        fakeRepository.insertTodoSchedule(
            TodoScheduleEntity(
                id = 1,
                title = "Task 1",
                content = null,
                isTodo = true,
                eventDate = today,
                startTime = null,
                endTime = null,
                category = CategoryType.WORK
            )
        )
        fakeRepository.insertDiary(DiaryEntity(date = today, content = "Good day", createdAt = 12345L))
        fakeRepository.insertExerciseSticker(ExerciseStickerEntity(date = today, isExercised = true))

        testScheduler.advanceUntilIdle()

        val exportedJson = viewModel.exportToJson()
        assertNotNull(exportedJson)
        assertTrue(exportedJson!!.contains("Task 1"))
        assertTrue(exportedJson.contains("Good day"))

        fakeRepository.deleteAllTodoSchedules()
        fakeRepository.deleteAllDiaries()
        fakeRepository.deleteAllExerciseStickers()
        
        testScheduler.advanceUntilIdle()
        assertEquals(0, fakeRepository.getAllTodoSchedules().first().size)

        val importSuccess = viewModel.importFromJson(exportedJson)
        assertTrue(importSuccess)
        
        testScheduler.advanceUntilIdle()

        val imported = fakeRepository.getAllTodoSchedules().first()
        assertEquals(1, imported.size)
        assertEquals("Task 1", imported[0].title)
    }
}

private class FakeDataRepository : DataRepository {
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
