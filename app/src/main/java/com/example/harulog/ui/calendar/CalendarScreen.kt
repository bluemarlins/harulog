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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.harulog.data.local.entity.CategoryType
import com.example.harulog.ui.common.SegmentedControl
import com.example.harulog.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 날짜 셀이 범위 하이라이트 내에서 차지하는 위치 */
enum class RangePosition { NONE, SINGLE, START, MIDDLE, END }

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    isExpanded: Boolean = true,
    onToggleExpand: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CalendarPane(
        state = state,
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
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
    isExpanded: Boolean = true,
    onToggleExpand: () -> Unit = {},
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
            modifier = if (isExpanded) Modifier.fillMaxSize().padding(8.dp)
                       else Modifier.wrapContentHeight().padding(8.dp)
        ) {
            // View Mode Selector + Expand/Collapse Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SegmentedControl(
                    items = listOf(CalendarViewMode.DAY, CalendarViewMode.WEEK, CalendarViewMode.MONTH),
                    selectedItem = state.viewMode,
                    onItemSelect = onSetViewMode,
                    modifier = Modifier.weight(1f),
                    labelProvider = { mode ->
                        when (mode) {
                            CalendarViewMode.DAY -> "일간"
                            CalendarViewMode.WEEK -> "주간"
                            CalendarViewMode.MONTH -> "월간"
                        }
                    }
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(4.dp))

                // Calendar Navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val formattedHeader = when (state.viewMode) {
                        CalendarViewMode.MONTH -> state.currentMonth.format(DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN))
                        CalendarViewMode.WEEK  -> state.selectedDate.format(DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN))
                        CalendarViewMode.DAY   -> state.selectedDate.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)", Locale.KOREAN))
                    }

                    IconButton(
                        onClick = {
                            when (state.viewMode) {
                                CalendarViewMode.MONTH -> onSelectMonth(state.currentMonth.minusMonths(1))
                                CalendarViewMode.WEEK  -> onSelectDate(state.selectedDate.minusWeeks(1))
                                CalendarViewMode.DAY   -> onSelectDate(state.selectedDate.minusDays(1))
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            formattedHeader,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }


                    IconButton(
                        onClick = {
                            when (state.viewMode) {
                                CalendarViewMode.MONTH -> onSelectMonth(state.currentMonth.plusMonths(1))
                                CalendarViewMode.WEEK  -> onSelectDate(state.selectedDate.plusWeeks(1))
                                CalendarViewMode.DAY   -> onSelectDate(state.selectedDate.plusDays(1))
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Grid Headers (Days of week)
                Row(modifier = Modifier.fillMaxWidth()) {
                    val days = listOf("일", "월", "화", "수", "목", "금", "토")
                    days.forEach {
                        Text(
                            text = it,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (it == "일") Color.Red else MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    MonthCalendarView(state, onSelectDate, onToggleWorkout)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 범위 위치 계산 헬퍼
// ─────────────────────────────────────────────────────────────────────────────

private fun resolveRangePosition(
    date: LocalDate,
    selectedDate: LocalDate,
    viewMode: CalendarViewMode
): RangePosition {
    return when (viewMode) {
        CalendarViewMode.DAY -> RangePosition.NONE

        CalendarViewMode.WEEK -> {
            val dow = selectedDate.dayOfWeek.value % 7  // 0 = Sunday
            val start = selectedDate.minusDays(dow.toLong())
            val end = start.plusDays(6)
            when {
                date < start || date > end -> RangePosition.NONE
                start == end               -> RangePosition.SINGLE
                date == start              -> RangePosition.START
                date == end                -> RangePosition.END
                else                       -> RangePosition.MIDDLE
            }
        }

        CalendarViewMode.MONTH -> {
            if (date.year != selectedDate.year || date.month != selectedDate.month) {
                RangePosition.NONE
            } else {
                val start = selectedDate.withDayOfMonth(1)
                val end   = selectedDate.withDayOfMonth(selectedDate.month.length(selectedDate.isLeapYear))
                when {
                    start == end -> RangePosition.SINGLE
                    date == start -> RangePosition.START
                    date == end   -> RangePosition.END
                    else          -> RangePosition.MIDDLE
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Views
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MonthCalendarView(
    state: CalendarUiState,
    onSelectDate: (LocalDate) -> Unit,
    onToggleWorkout: (LocalDate) -> Unit
) {
    val firstDay    = state.currentMonth.atDay(1)
    val dayOfWeek   = firstDay.dayOfWeek.value % 7
    val daysInMonth = state.currentMonth.lengthOfMonth()

    val dates = ArrayList<LocalDate?>(42)
    for (i in 0 until dayOfWeek) dates.add(null)
    for (i in 1..daysInMonth)    dates.add(state.currentMonth.atDay(i))
    while (dates.size < 42)      dates.add(null)

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
                            val rangePos = resolveRangePosition(date, state.selectedDate, state.viewMode)
                            DateCell(
                                date          = date,
                                isSelected    = date == state.selectedDate,
                                rangePosition = rangePos,
                                state         = state,
                                onClick       = { onSelectDate(date) },
                                onLongClick   = { onToggleWorkout(date) }
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
    val dayOfWeek   = state.selectedDate.dayOfWeek.value % 7
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
                    date          = date,
                    isSelected    = date == state.selectedDate,
                    rangePosition = RangePosition.NONE,
                    state         = state,
                    onClick       = { onSelectDate(date) },
                    onLongClick   = { onToggleWorkout(date) }
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
                .background(if (hasWorkout) SuccessWorkoutColor else MaterialTheme.colorScheme.surfaceVariant)
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

// ─────────────────────────────────────────────────────────────────────────────
// DateCell
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DateCell(
    date: LocalDate,
    isSelected: Boolean,
    rangePosition: RangePosition,
    state: CalendarUiState,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val primary    = MaterialTheme.colorScheme.primary
    val hasWorkout = state.allStickers.any { it.date == date && it.isExercised }
    val dayItems   = state.allTodoSchedules.filter { it.eventDate == date }
    val isToday    = date == LocalDate.now()

    // 범위 하이라이트 형태: 양끝은 반원, 중간은 직사각형
    val rangeColor = primary.copy(alpha = 0.10f)
    val rangeShape: RoundedCornerShape = when (rangePosition) {
        RangePosition.START  -> RoundedCornerShape(topStart = 50f, bottomStart = 50f, topEnd = 0f, bottomEnd = 0f)
        RangePosition.END    -> RoundedCornerShape(topStart = 0f, bottomStart = 0f, topEnd = 50f, bottomEnd = 50f)
        RangePosition.SINGLE -> RoundedCornerShape(50f)
        else                 -> RoundedCornerShape(0f) // MIDDLE or NONE (unused)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center
    ) {
        // ── 범위 하이라이트 배경 스트립 ──────────────────────────────────
        if (rangePosition != RangePosition.NONE) {
            if (rangePosition == RangePosition.MIDDLE) {
                // 중간: 모서리 없는 전체 폭 띠
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.55f)
                        .background(rangeColor)
                )
            } else {
                // 시작/끝/단독: 반원 클리핑
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.55f)
                        .clip(rangeShape)
                        .background(rangeColor)
                )
            }
        }

        // ── 날짜 콘텐츠 ──────────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 1.dp, horizontal = 1.dp)
        ) {
            // Date Circle or Capsule based on viewMode
            val isWeekMode = state.viewMode == CalendarViewMode.WEEK
            val cellSizeModifier = if (isWeekMode) Modifier.size(width = 32.dp, height = 48.dp) else Modifier.size(26.dp)
            val cellShape = if (isWeekMode) RoundedCornerShape(16.dp) else CircleShape

            Box(
                modifier = Modifier
                    .then(cellSizeModifier)
                    .clip(cellShape)
                    .let {
                        if (isSelected) {
                            it.background(
                                Brush.horizontalGradient(
                                    colors = listOf(LightPrimary, Color(0xFF6366F1))
                                )
                            )
                        } else if (isToday) {
                            it.background(MaterialTheme.colorScheme.surfaceVariant)
                        } else {
                            it
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    fontSize = 11.sp,
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isSelected -> Color.White
                        isToday    -> primary
                        else       -> MaterialTheme.colorScheme.onBackground
                    }
                )
            }

            Spacer(modifier = Modifier.height(1.dp))

            // Category Indicator Bars
            Row(
                modifier = Modifier.height(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (dayItems.any { it.category == CategoryType.WORK }) {
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(WorkPrimaryColor)
                    )
                }
                if (dayItems.any { it.category == CategoryType.PERSONAL }) {
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(PersonalPrimaryColor)
                    )
                }
            }
        }

        // Workout Sticker (TopEnd overlay badge)
        if (hasWorkout) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 2.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(SuccessWorkoutColor)
            )
        }

        // Salary Day Sticker (TopStart overlay badge with Money Emoji)
        val yearMonth = YearMonth.from(date)
        val clampedDay = state.salaryDay.coerceIn(1, yearMonth.lengthOfMonth())
        val originalSalaryDate = LocalDate.of(yearMonth.year, yearMonth.month, clampedDay)
        val adjustedSalaryDate = when (originalSalaryDate.dayOfWeek) {
            java.time.DayOfWeek.SATURDAY -> originalSalaryDate.minusDays(1)
            java.time.DayOfWeek.SUNDAY -> originalSalaryDate.minusDays(2)
            else -> originalSalaryDate
        }
        val isSalaryDay = date == adjustedSalaryDate
        if (isSalaryDay) {
            Text(
                text = "💸",
                fontSize = 9.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 2.dp, start = 2.dp)
            )
        }
    }
}


