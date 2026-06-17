package com.example.harulog.ui.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.harulog.data.local.entity.CategoryType
import com.example.harulog.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CalendarPane(
        state = state,
        onSelectDate = viewModel::selectDate,
        onSelectMonth = viewModel::selectMonth,
        onSetViewMode = viewModel::setViewMode,
        onToggleWorkout = viewModel::toggleWorkout,
        modifier = modifier
    )
}

@Composable
fun CalendarPane(
    state: CalendarUiState,
    onSelectDate: (LocalDate) -> Unit,
    onSelectMonth: (YearMonth) -> Unit,
    onSetViewMode: (CalendarViewMode) -> Unit,
    onToggleWorkout: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .border(1.dp, GrayBorderColor, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // View Mode Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LightBackground, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val modes = listOf(
                    CalendarViewMode.MONTH to "월간",
                    CalendarViewMode.WEEK to "주간",
                    CalendarViewMode.DAY to "일간"
                )
                modes.forEach { (mode, title) ->
                    val isSelected = state.viewMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { onSetViewMode(mode) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            title,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val formattedHeader = when (state.viewMode) {
                    CalendarViewMode.MONTH -> state.currentMonth.format(DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN))
                    CalendarViewMode.WEEK -> state.selectedDate.format(DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN))
                    CalendarViewMode.DAY -> state.selectedDate.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)", Locale.KOREAN))
                }

                IconButton(
                    onClick = {
                        when (state.viewMode) {
                            CalendarViewMode.MONTH -> onSelectMonth(state.currentMonth.minusMonths(1))
                            CalendarViewMode.WEEK -> onSelectDate(state.selectedDate.minusWeeks(1))
                            CalendarViewMode.DAY -> onSelectDate(state.selectedDate.minusDays(1))
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev")
                }

                Text(
                    formattedHeader,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                IconButton(
                    onClick = {
                        when (state.viewMode) {
                            CalendarViewMode.MONTH -> onSelectMonth(state.currentMonth.plusMonths(1))
                            CalendarViewMode.WEEK -> onSelectDate(state.selectedDate.plusWeeks(1))
                            CalendarViewMode.DAY -> onSelectDate(state.selectedDate.plusDays(1))
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Grid Headers (Days of week)
            if (state.viewMode != CalendarViewMode.DAY) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    val days = listOf("일", "월", "화", "수", "목", "금", "토")
                    days.forEach {
                        Text(
                            text = it,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (it == "일") Color.Red else MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Calendar Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (state.viewMode) {
                    CalendarViewMode.MONTH -> MonthCalendarView(state, onSelectDate, onToggleWorkout)
                    CalendarViewMode.WEEK -> WeekCalendarView(state, onSelectDate, onToggleWorkout)
                    CalendarViewMode.DAY -> DayCalendarView(state, onToggleWorkout)
                }
            }
        }
    }
}

@Composable
fun MonthCalendarView(
    state: CalendarUiState,
    onSelectDate: (LocalDate) -> Unit,
    onToggleWorkout: (LocalDate) -> Unit
) {
    val firstDay = state.currentMonth.atDay(1)
    val dayOfWeek = firstDay.dayOfWeek.value % 7
    val daysInMonth = state.currentMonth.lengthOfMonth()

    val totalSlots = 42
    val dates = ArrayList<LocalDate?>()
    for (i in 0 until dayOfWeek) {
        dates.add(null)
    }
    for (i in 1..daysInMonth) {
        dates.add(state.currentMonth.atDay(i))
    }
    while (dates.size < totalSlots) {
        dates.add(null)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        for (week in 0 until 6) {
            Row(modifier = Modifier.weight(1f)) {
                for (day in 0 until 7) {
                    val date = dates[week * 7 + day]
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        if (date != null) {
                            DateCell(
                                date = date,
                                isSelected = date == state.selectedDate,
                                state = state,
                                onClick = { onSelectDate(date) },
                                onLongClick = { onToggleWorkout(date) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeekCalendarView(
    state: CalendarUiState,
    onSelectDate: (LocalDate) -> Unit,
    onToggleWorkout: (LocalDate) -> Unit
) {
    val dayOfWeek = state.selectedDate.dayOfWeek.value % 7
    val startOfWeek = state.selectedDate.minusDays(dayOfWeek.toLong())

    Row(modifier = Modifier.fillMaxWidth().height(80.dp)) {
        for (day in 0 until 7) {
            val date = startOfWeek.plusDays(day.toLong())
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                DateCell(
                    date = date,
                    isSelected = date == state.selectedDate,
                    state = state,
                    onClick = { onSelectDate(date) },
                    onLongClick = { onToggleWorkout(date) }
                )
            }
        }
    }
}

@Composable
fun DayCalendarView(
    state: CalendarUiState,
    onToggleWorkout: (LocalDate) -> Unit
) {
    val hasWorkout = state.allStickers.any { it.date == state.selectedDate && it.isExercised }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "오늘의 운동 성취 완료?",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(if (hasWorkout) SuccessWorkoutColor else LightBackground)
                .clickable { onToggleWorkout(state.selectedDate) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Workout toggle",
                modifier = Modifier.size(48.dp),
                tint = if (hasWorkout) Color.White else MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            if (hasWorkout) "완료한 운동 스티커 등록됨!" else "터치하여 운동 스티커를 완료하세요",
            fontSize = 12.sp,
            color = if (hasWorkout) SuccessWorkoutColor else MaterialTheme.colorScheme.secondary
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DateCell(
    date: LocalDate,
    isSelected: Boolean,
    state: CalendarUiState,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val hasWorkout = state.allStickers.any { it.date == date && it.isExercised }
    val dayItems = state.allTodoSchedules.filter { it.eventDate == date }
    val isToday = date == LocalDate.now()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 2.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Date Circle
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isToday -> LightBackground
                            else -> Color.Transparent
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isSelected -> Color.White
                        isToday -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onBackground
                    }
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Category Indicators (Dots: Work = Dark Blue, Personal = Coral / Purple)
            Row(
                modifier = Modifier.height(4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (dayItems.any { it.category == CategoryType.WORK }) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(WorkPrimaryColor)
                    )
                }
                if (dayItems.any { it.category == CategoryType.PERSONAL }) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(PersonalPrimaryColor)
                    )
                }
            }
        }

        // Workout Sticker (Emerald Green Circle badge / sticker overlay in TopEnd)
        if (hasWorkout) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 2.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(SuccessWorkoutColor)
            )
        }
    }
}
