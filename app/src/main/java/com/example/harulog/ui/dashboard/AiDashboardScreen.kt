package com.example.harulog.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.harulog.R
import com.example.harulog.data.local.entity.CategoryType
import com.example.harulog.data.local.entity.TodoScheduleEntity
import com.example.harulog.ui.theme.*
import com.example.harulog.ui.todo.TodoViewModel
import com.example.harulog.ui.todo.MonthlyDashboardSummary
import com.example.harulog.ui.todo.MergedTodoScheduleItem
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Google Fonts Provider — Fredoka (둥글둥글 볼드 버블 폰트)
// ─────────────────────────────────────────────────────────────────────────────

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage    = "com.google.android.gms",
    certificates       = R.array.com_google_android_gms_fonts_certs
)

private val fredokaFont = GoogleFont("Fredoka")

private val FredokaFamily = FontFamily(
    Font(
        googleFont    = fredokaFont,
        fontProvider  = provider,
        weight        = FontWeight.Bold
    )
)

// ─────────────────────────────────────────────────────────────────────────────
// Screen Entry
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AiDashboardScreen(
    todoViewModel: TodoViewModel,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 96.dp)
) {
    val state by todoViewModel.uiState.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val currentMonth = YearMonth.from(today)

    // 한달 요약 정보 수집
    val dashboardSummary by remember(today) {
        todoViewModel.getMonthlyDashboardSummary(currentMonth)
    }.collectAsStateWithLifecycle(initialValue = MonthlyDashboardSummary())

    // 이번 달 미완료 할 일 (월간 범위형) - 병합 적용
    val rawOverdueMonthlyTodos = todoViewModel.getAllTodosForDashboard()
        .filter { it.isTodo && it.isMonthlyScope && !it.isCompleted &&
                it.eventDate.year == today.year && it.eventDate.month == today.month }
    val overdueMonthlyTodos = remember(rawOverdueMonthlyTodos) {
        todoViewModel.mergeConsecutiveItems(rawOverdueMonthlyTodos, com.example.harulog.ui.calendar.CalendarViewMode.MONTH)
    }

    // 이번 주 일정 (일요일~토요일) - 병합 적용
    val dow = today.dayOfWeek.value % 7
    val weekStart = today.minusDays(dow.toLong())
    val weekEnd   = weekStart.plusDays(6)
    val rawWeekSchedules = todoViewModel.getAllTodosForDashboard()
        .filter { !it.isTodo && it.eventDate >= weekStart && it.eventDate <= weekEnd }
        .sortedBy { it.eventDate }
    val weekSchedules = remember(rawWeekSchedules) {
        todoViewModel.mergeConsecutiveItems(rawWeekSchedules, com.example.harulog.ui.calendar.CalendarViewMode.WEEK)
    }

    AiDashboardContent(
        overdueMonthlyTodos = overdueMonthlyTodos,
        weekSchedules       = weekSchedules,
        today               = today,
        weekStart           = weekStart,
        weekEnd             = weekEnd,
        dashboardSummary    = dashboardSummary,
        modifier            = modifier,
        contentPadding      = contentPadding
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Content (Stateless — testable)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AiDashboardContent(
    overdueMonthlyTodos: List<MergedTodoScheduleItem>,
    weekSchedules: List<MergedTodoScheduleItem>,
    today: LocalDate,
    weekStart: LocalDate,
    weekEnd: LocalDate,
    dashboardSummary: MonthlyDashboardSummary,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 96.dp)
) {
    val currentMonth = YearMonth.from(today)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = contentPadding
    ) {
        // ── ① 캘리그라피 월 헤더 ──────────────────────────────────────────
        item {
            MonthCalligraphyHeader(month = currentMonth)
        }

        // ── ② 핵심 지표 상단 2열 (월급 D-Day / 운동 횟수) ───────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SalaryDayCard(diff = dashboardSummary.salaryDayDiff)
                }
                Box(modifier = Modifier.weight(1f)) {
                    WorkoutCountCard(
                        count   = dashboardSummary.totalWorkoutCount,
                        message = dashboardSummary.workoutMotivationMessage
                    )
                }
            }
        }

        // ── ③ 연차 사용 현황 / 완료 할일 수 ──────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    AnnualLeaveListCard(
                        count = dashboardSummary.annualLeaveCount,
                        dates = dashboardSummary.annualLeaveDates
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    CompletedTodoCard(
                        completed = dashboardSummary.completedTodoCount,
                        total     = dashboardSummary.totalTodoCount
                    )
                }
            }
        }

        // ── ④ 이번 달 일정 요약 ───────────────────────────────────────────
        item {
            DashboardSectionHeader(
                icon  = Icons.Outlined.Star,
                title = "이번 달 일정 요약",
                tint  = MaterialTheme.colorScheme.primary
            )
        }
        item {
            MonthlyScheduleSummaryCard(
                mostFreqTitle    = dashboardSummary.mostFrequentScheduleTitle,
                mostFreqCount    = dashboardSummary.mostFrequentScheduleCount,
                longestTitle     = dashboardSummary.longestScheduleTitle,
                longestDuration  = dashboardSummary.longestScheduleDuration,
                longestPeriod    = dashboardSummary.longestSchedulePeriodText,
                totalScheduleCount = dashboardSummary.totalScheduleCount,
                meetingSchedules = dashboardSummary.meetingSchedules
            )
        }

        // ── ⑤ 이번 달 미완료 할 일 경고 ─────────────────────────────────────
        if (overdueMonthlyTodos.isNotEmpty()) {
            item {
                DashboardSectionHeader(
                    icon  = Icons.Outlined.Warning,
                    title = "이번 달 안에 해야 할 일 (미완료)",
                    tint  = MaterialTheme.colorScheme.error
                )
            }
            items(overdueMonthlyTodos, key = { it.id }) { todo ->
                UrgentTodoCard(todo = todo)
            }
        }

        // ── ⑥ 이번 주 일정 요약 ─────────────────────────────────────────────
        item {
            DashboardSectionHeader(
                icon  = Icons.Outlined.Star,
                title = "이번 주 일정",
                tint  = MaterialTheme.colorScheme.primary
            )
            Text(
                "${weekStart.format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN))}" +
                " ~ ${weekEnd.format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN))}",
                fontSize  = 12.sp,
                color     = MaterialTheme.colorScheme.secondary,
                modifier  = Modifier.padding(start = 28.dp, bottom = 4.dp)
            )
        }

        if (weekSchedules.isEmpty()) {
            item {
                EmptyStateCard(message = "이번 주 등록된 일정이 없습니다.")
            }
        } else {
            items(weekSchedules, key = { "sched_${it.id}" }) { schedule ->
                WeekScheduleCard(schedule = schedule, today = today)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-components
// ─────────────────────────────────────────────────────────────────────────────

/** AI 주간/월간 요약 영역 — 추후 실제 AI 응답으로 교체 */
@Composable
private fun AiSummaryBanner() {
    val auroraBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF4F46E5), // Indigo
            Color(0xFF7C3AED), // Violet
            Color(0xFFEC4899), // Pink
            Color(0xFF06B6D4)  // Cyan
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color(0xFF7C3AED),
                spotColor = Color(0xFFEC4899)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(auroraBrush)
            .border(0.8.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.Star,
                    contentDescription = null,
                    tint   = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "AI 일정 요약",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp,
                    color      = Color.White
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.20f)
                ) {
                    Text(
                        "준비 중",
                        fontSize = 10.sp,
                        color    = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Text(
                "AI가 이번 주·이번 달의 일정과 할 일을 분석하여\n우선순위와 핵심 요약을 제공할 예정입니다.",
                fontSize   = 13.sp,
                color      = Color.White.copy(alpha = 0.88f),
                lineHeight = 20.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Calligraphy Month Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MonthCalligraphyHeader(month: YearMonth) {
    val monthName = month.month.getDisplayName(
        java.time.format.TextStyle.FULL,
        Locale.ENGLISH
    ) // e.g. "June"
    val year = month.year.toString()

    val isDark      = MaterialTheme.colorScheme.background == DarkBackground
    // Design.md 컬러 스킴 적용
    val primaryColor    = if (isDark) DarkPrimary else LightPrimary
    val secondaryColor  = if (isDark) DarkSecondary else LightSecondary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text       = monthName,
            fontFamily = FredokaFamily,
            fontWeight = FontWeight.Bold,
            fontSize   = 68.sp,
            lineHeight = 72.sp,
            color      = primaryColor
        )
        Text(
            text       = year,
            fontFamily = FredokaFamily,
            fontWeight = FontWeight.Normal,
            fontSize   = 20.sp,
            lineHeight = 24.sp,
            color      = secondaryColor,
            modifier   = Modifier.padding(start = 4.dp, top = 2.dp)
        )
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// DashboardMetricCard — 공통 템플릿 카드 컴포넌트
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DashboardMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueSuffix: String? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    icon: ImageVector? = null,
    subtext: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val borderColor = if (isDark) DarkBorderColor else GrayBorderColor

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .border(1.dp, borderColor, MaterialTheme.shapes.medium),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = accentColor
                )
                if (valueSuffix != null) {
                    Text(
                        text = valueSuffix,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            if (content != null) {
                content()
            } else if (subtext != null) {
                Text(
                    text = subtext,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    maxLines = 2
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SalaryDayCard — 월급날 D-Day
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SalaryDayCard(diff: Int) {
    val isToday = diff == 0
    val isPast  = diff < 0

    val label = when {
        isToday -> "오늘 💰"
        isPast  -> "D+${-diff}"
        else    -> "D-${diff}"
    }
    val subLabel = when {
        isToday -> "월급날이에요!"
        isPast  -> "${-diff}일 전 지났어요"
        else    -> "${diff}일 후 월급날"
    }

    DashboardMetricCard(
        title = "월급날",
        value = label,
        accentColor = MaterialTheme.colorScheme.primary,
        subtext = subLabel
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// WorkoutCountCard — 이달 운동 횟수
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WorkoutCountCard(count: Int, message: String) {
    DashboardMetricCard(
        title = "이달 운동",
        value = "$count",
        valueSuffix = " 회",
        accentColor = SuccessWorkoutColor,
        subtext = message
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// CompletedTodoCard — 이달 완료한 할일 수
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CompletedTodoCard(completed: Int, total: Int) {
    val progress = if (total > 0) completed.toFloat() / total.toFloat() else 0f
    val accentColor = PersonalPrimaryColor

    DashboardMetricCard(
        title = "이달 완료",
        value = "$completed",
        valueSuffix = " / ${total}건",
        accentColor = accentColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AnnualLeaveListCard — 이달 연차 사용 현황
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AnnualLeaveListCard(count: Int, dates: List<LocalDate>) {
    val datesText = if (dates.isEmpty()) {
        "사용 내역이 없습니다."
    } else {
        dates.joinToString(", ") { "${it.monthValue}/${it.dayOfMonth}" }
    }

    DashboardMetricCard(
        title = "연차 사용",
        value = "$count",
        valueSuffix = " 회 사용",
        accentColor = WorkPrimaryColor,
        subtext = datesText
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// MonthlyScheduleSummaryCard — 이달 일정 요약 (가장 많은 / 가장 긴)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MonthlyScheduleSummaryCard(
    mostFreqTitle: String?,
    mostFreqCount: Int,
    longestTitle: String?,
    longestDuration: Int,
    longestPeriod: String?,
    totalScheduleCount: Int,
    meetingSchedules: List<TodoScheduleEntity>
) {
    val isDark      = MaterialTheme.colorScheme.background == DarkBackground
    val borderColor = if (isDark) DarkBorderColor else GrayBorderColor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, borderColor, MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 총 일정 일수
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "이번 달 일정",
                fontSize   = 12.sp,
                color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
            Spacer(Modifier.weight(1f))
            Text(
                "총 ${totalScheduleCount}일",
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary
            )
        }

        // 구분선
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.8.dp)
                .background(borderColor)
        )

        // 가장 많았던 일정
        SummaryRow(
            label = "가장 많았던 일정",
            value = if (mostFreqTitle != null) "$mostFreqTitle  (${mostFreqCount}일)" else "데이터 없음",
            tint  = MaterialTheme.colorScheme.primary
        )

        // 가장 길었던 일정
        SummaryRow(
            label = "가장 길었던 일정",
            value = if (longestTitle != null && longestDuration >= 2)
                        "$longestTitle  (${longestDuration}일 연속, $longestPeriod)"
                    else "연속 2일 이상 일정 없음",
            tint  = MaterialTheme.colorScheme.secondary
        )

        // 구분선
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.8.dp)
                .background(borderColor)
        )

        // 이번 달 주요 회의/논의/검토 일정
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "이번 달 주요 회의 및 논의",
                fontSize   = 11.sp,
                color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            if (meetingSchedules.isEmpty()) {
                Text(
                    "예정된 회의 일정이 없습니다.",
                    fontSize   = 13.sp,
                    color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Medium
                )
            } else {
                meetingSchedules.forEach { meeting ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val formatter = DateTimeFormatter.ofPattern("M/d (E)", Locale.KOREAN)
                        Text(
                            text = meeting.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = meeting.eventDate.format(formatter),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, tint: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            fontSize   = 11.sp,
            color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Text(
            value,
            fontSize   = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DashboardSectionHeader
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DashboardSectionHeader(
    icon: ImageVector,
    title: String,
    tint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground)
    }
}

/** 미완료 월간 할 일 — 경고 강조 카드 */
@Composable
private fun UrgentTodoCard(todo: MergedTodoScheduleItem) {
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val themeColor = if (todo.category == CategoryType.WORK) {
        if (isDark) WorkDarkPrimaryColor else WorkPrimaryColor
    } else {
        if (isDark) PersonalDarkPrimaryColor else PersonalPrimaryColor
    }
    val bgColor = if (todo.category == CategoryType.WORK) {
        if (isDark) WorkDarkBackgroundColor else WorkBackgroundColor
    } else {
        if (isDark) PersonalDarkBackgroundColor else PersonalBackgroundColor
    }
    val borderColor = if (isDark) DarkBorderColor else GrayBorderColor

    val isMerged = todo.originalItems.size > 1

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isMerged) {
            if (todo.originalItems.size > 2) {
                // Bottom stacked card background
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(top = 8.dp, start = 8.dp, end = 8.dp)
                        .background(bgColor.copy(alpha = 0.5f), MaterialTheme.shapes.medium)
                        .border(1.dp, borderColor.copy(alpha = 0.5f), MaterialTheme.shapes.medium)
                )
            }
            // Middle stacked card background
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(top = 4.dp, start = 4.dp, end = 4.dp)
                    .background(bgColor.copy(alpha = 0.8f), MaterialTheme.shapes.medium)
                    .border(1.dp, borderColor.copy(alpha = 0.8f), MaterialTheme.shapes.medium)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (isMerged) 8.dp else 0.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(bgColor)
                .border(1.dp, borderColor, MaterialTheme.shapes.medium)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 카테고리 컬러 바
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(32.dp)
                    .background(themeColor, RoundedCornerShape(2.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    todo.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 14.sp,
                    color      = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                
                val dateStr = if (todo.isMonthlyScope) {
                    todo.startDate.format(DateTimeFormatter.ofPattern("M월 범위", Locale.KOREAN))
                } else if (isMerged) {
                    val startStr = todo.startDate.format(DateTimeFormatter.ofPattern("M/d (E)", Locale.KOREAN))
                    val endStr = todo.endDate.format(DateTimeFormatter.ofPattern("M/d (E)", Locale.KOREAN))
                    "$startStr ~ $endStr"
                } else {
                    todo.startDate.format(DateTimeFormatter.ofPattern("M/d (E)", Locale.KOREAN))
                }

                Text(
                    "$dateStr · ${if (todo.category == CategoryType.WORK) "업무" else "개인"} · 월간 범위형",
                    fontSize = 11.sp,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = themeColor.copy(alpha = 0.15f)
            ) {
                Text(
                    "할 일",
                    fontSize   = 10.sp,
                    color      = themeColor,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

/** 이번 주 일정 카드 */
@Composable
private fun WeekScheduleCard(schedule: MergedTodoScheduleItem, today: LocalDate) {
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val themeColor = if (schedule.category == CategoryType.WORK) {
        if (isDark) WorkDarkPrimaryColor else WorkPrimaryColor
    } else {
        if (isDark) PersonalDarkPrimaryColor else PersonalPrimaryColor
    }
    val bgColor    = if (schedule.category == CategoryType.WORK) {
        if (isDark) WorkDarkBackgroundColor else WorkBackgroundColor
    } else {
        if (isDark) PersonalDarkBackgroundColor else PersonalBackgroundColor
    }
    val borderColor = if (isDark) DarkBorderColor else GrayBorderColor
    val isToday    = !schedule.isMonthlyScope && schedule.startDate == today
    val dayFormatter = DateTimeFormatter.ofPattern("M/d (E)", Locale.KOREAN)

    val isMerged = schedule.originalItems.size > 1

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isMerged) {
            if (schedule.originalItems.size > 2) {
                // Bottom stacked card background
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(top = 8.dp, start = 8.dp, end = 8.dp)
                        .background(bgColor.copy(alpha = 0.5f), MaterialTheme.shapes.medium)
                        .border(1.dp, borderColor.copy(alpha = 0.5f), MaterialTheme.shapes.medium)
                )
            }
            // Middle stacked card background
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(top = 4.dp, start = 4.dp, end = 4.dp)
                    .background(bgColor.copy(alpha = 0.8f), MaterialTheme.shapes.medium)
                    .border(1.dp, borderColor.copy(alpha = 0.8f), MaterialTheme.shapes.medium)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (isMerged) 8.dp else 0.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(bgColor)
                .border(1.dp, borderColor, MaterialTheme.shapes.medium)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(28.dp)
                    .background(themeColor, RoundedCornerShape(2.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    schedule.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 14.sp,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val dateStr = if (isMerged) {
                        val startStr = schedule.startDate.format(dayFormatter)
                        val endStr = schedule.endDate.format(dayFormatter)
                        "$startStr ~ $endStr"
                    } else {
                        schedule.startDate.format(dayFormatter)
                    }

                    Text(
                        dateStr,
                        fontSize = 11.sp,
                        color    = if (isToday) themeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                    )
                    
                    val timeStr = if (schedule.startTime != null) {
                        schedule.startTime.toString()
                    } else {
                        "하루 종일"
                    }
                    Text("·", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                    Text(
                        timeStr,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                }
            }
            if (isToday) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = themeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        "오늘",
                        fontSize   = 10.sp,
                        color      = themeColor,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(message: String) {
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, if (isDark) DarkBorderColor else GrayBorderColor, MaterialTheme.shapes.medium)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
    }
}
