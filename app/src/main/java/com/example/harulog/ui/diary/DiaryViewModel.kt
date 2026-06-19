package com.example.harulog.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.harulog.data.local.entity.DiaryEntity
import com.example.harulog.data.repository.DataRepository
import com.example.harulog.utils.SelectedDateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DiaryUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val currentDiary: DiaryEntity? = null,
    /** 다이어리가 입력된 날짜 목록 (최신 날짜 순 정렬) */
    val allDiaryDates: List<LocalDate> = emptyList()
)

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val repository: DataRepository,
    private val selectedDateManager: SelectedDateManager
) : ViewModel() {

    val uiState: StateFlow<DiaryUiState> = combine(
        selectedDateManager.selectedDate,
        repository.getAllDiaries()
    ) { selectedDate, diaries ->
        val diary = diaries.find { it.date == selectedDate }
        DiaryUiState(
            selectedDate   = selectedDate,
            currentDiary   = diary,
            allDiaryDates  = diaries.map { it.date }.sortedDescending()
        )
    }.stateIn(
        scope   = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DiaryUiState()
    )

    /** 드롭다운에서 날짜를 선택했을 때 해당 날짜로 이동 */
    fun selectDate(date: LocalDate) {
        selectedDateManager.selectDate(date)
    }

    fun saveDiary(content: String) {
        viewModelScope.launch {
            val date = uiState.value.selectedDate
            repository.insertDiary(
                DiaryEntity(
                    date      = date,
                    content   = content,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteDiary() {
        viewModelScope.launch {
            uiState.value.currentDiary?.let {
                repository.deleteDiary(it)
            }
        }
    }
}
