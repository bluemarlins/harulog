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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.harulog.data.local.entity.CategoryType
import com.example.harulog.data.local.entity.TodoScheduleEntity
import com.example.harulog.ui.theme.*
import com.example.harulog.ui.todo.TodoViewModel
import com.example.harulog.ui.todo.MonthlyDashboardSummary
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Screen Entry
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AiDashboardScreen(
    todoViewModel: TodoViewModel,
    modifier: Modifier = Modifier
) {
    val state by todoViewModel.uiState.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val currentMonth = YearMonth.from(today)

    // 한달 요약 정보 수집
    val dashboardSummary by remember(today) {
        todoViewModel.getMonthlyDashboardSummary(currentMonth)
    }.collectAsStateWithLifecycle(initialValue = MonthlyDashboardSummary())

    // 이번 달 미완료 할 일 (월간 범위형)
    val overdueMonthlyTodos = todoViewModel.getAllTodosForDashboard()
        .filter { it.isTodo && it.isMonthlyScope && !it.isCompleted &&
                it.eventDate.year == today.year && it.eventDate.month == today.month }

    // 이번 주 일정 (일요일~토요일)
    val dow = today.dayOfWeek.value % 7
    val weekStart = today.minusDays(dow.toLong())
    val weekEnd   = weekStart.plusDays(6)
    val weekSchedules = todoViewModel.getAllTodosForDashboard()
        .filter { !it.isTodo && it.eventDate >= weekStart && it.eventDate <= weekEnd }
        .sortedBy { it.eventDate }

    AiDashboardContent(
        overdueMonthlyTodos = overdueMonthlyTodos,
        weekSchedules       = weekSchedules,
        today               = today,
        weekStart           = weekStart,
        weekEnd             = weekEnd,
        dashboardSummary    = dashboardSummary,
        modifier            = modifier
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Content (Stateless — testable)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AiDashboardContent(
    overdueMonthlyTodos: List<TodoScheduleEntity>,
    weekSchedules: List<TodoScheduleEntity>,
    today: LocalDate,
    weekStart: LocalDate,
    weekEnd: LocalDate,
    dashboardSummary: MonthlyDashboardSummary,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // ── 한달 요약 섹션 ──────────────────────────────────────────────
        item {
            DashboardSectionHeader(
                icon = Icons.Outlined.Info,
                title = "한달 요약 리포트",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // AI 요약 배너 (플레이스홀더)
        item {
            AiSummaryBanner()
        }

        // 장기 일정 하이라이트 카드 (최장 연속 일정 2일 이상 시 노출)
        if (dashboardSummary.longestScheduleDuration >= 2 && dashboardSummary.longestScheduleTitle != null) {
            item {
                LongestScheduleHighlightCard(
                    title = dashboardSummary.longestScheduleTitle,
                    duration = dashboardSummary.longestScheduleDuration,
                    periodText = dashboardSummary.longestSchedulePeriodText ?: ""
                )
            }
        }

        // 운동 & 연차 요약 카드 (가로 2열 배치)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    WorkoutMotivationCard(
                        count = dashboardSummary.totalWorkoutCount,
                        message = dashboardSummary.workoutMotivationMessage
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    AnnualLeaveListCard(
                        count = dashboardSummary.annualLeaveCount,
                        dates = dashboardSummary.annualLeaveDates
                    )
                }
            }
        }

        // ── 이번 달 미완료 할 일 경고 ─────────────────────────────────────
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

        // ── 이번 주 일정 요약 ─────────────────────────────────────────────
        item {
            DashboardSectionHeader(
                icon  = Icons.Outlined.Star,
                title = "이번 주 일정 요약",
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                    )
                )
            )
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
private fun UrgentTodoCard(todo: TodoScheduleEntity) {
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val themeColor = if (todo.category == CategoryType.WORK) {
        if (isDark) WorkDarkPrimaryColor else WorkPrimaryColor
    } else {
        if (isDark) PersonalDarkPrimaryColor else PersonalPrimaryColor
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.30f))
            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
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
            Text(
                "${if (todo.category == CategoryType.WORK) "업무" else "개인"} · 월간 범위형",
                fontSize = 11.sp,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
        }
        Icon(
            Icons.Outlined.Warning,
            contentDescription = null,
            tint   = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** 이번 주 일정 카드 */
@Composable
private fun WeekScheduleCard(schedule: TodoScheduleEntity, today: LocalDate) {
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
    val isToday    = schedule.eventDate == today
    val dayFormatter = DateTimeFormatter.ofPattern("M/d (E)", Locale.KOREAN)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
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
                Text(
                    schedule.eventDate.format(dayFormatter),
                    fontSize = 11.sp,
                    color    = if (isToday) themeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                )
                if (schedule.startTime != null) {
                    Text("·", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                    Text(schedule.startTime.toString(), fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                }
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

@Composable
private fun EmptyStateCard(message: String) {
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, if (isDark) DarkBorderColor else GrayBorderColor, RoundedCornerShape(14.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
    }
}



// ─────────────────────────────────────────────────────────────────────────────
// 추가된 한달 요약 리포트 카드 컴포넌트
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WorkoutMotivationCard(count: Int, message: String) {
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    
    val bgColor = if (isDark) Color(0xFF1B2E24) else Color(0xFFE8F5E9)
    val borderColor = if (isDark) Color(0xFF2E4D3E) else Color(0xFFC8E6C9)
    val tintColor = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .shadow(1.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Outlined.Star,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                "운동",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = onSurfaceColor
            )
        }
        
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "$count",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = tintColor
                )
                Text(
                    " 회 완료",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = onSurfaceColor.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Text(
                message,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                color = onSurfaceColor.copy(alpha = 0.7f),
                maxLines = 3
            )
        }
    }
}

@Composable
private fun AnnualLeaveListCard(count: Int, dates: List<LocalDate>) {
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    
    val bgColor = if (isDark) Color(0xFF1E2838) else Color(0xFFE3F2FD)
    val borderColor = if (isDark) Color(0xFF2C3E56) else Color(0xFFBBDEFB)
    val tintColor = if (isDark) Color(0xFF64B5F6) else Color(0xFF1565C0)
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    val datesText = if (dates.isEmpty()) {
        "사용 내역이 없습니다."
    } else {
        dates.joinToString(", ") { "${it.monthValue}/${it.dayOfMonth}" }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .shadow(1.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                "연차 사용",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = onSurfaceColor
            )
        }
        
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "$count",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = tintColor
                )
                Text(
                    " 회 사용",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = onSurfaceColor.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Text(
                datesText,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                color = onSurfaceColor.copy(alpha = 0.7f),
                maxLines = 3
            )
        }
    }
}

@Composable
private fun LongestScheduleHighlightCard(
    title: String,
    duration: Int,
    periodText: String
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    
    val bgColor = if (isDark) Color(0xFF2D1F3D) else Color(0xFFF3E5F5)
    val borderColor = if (isDark) Color(0xFF4C3069) else Color(0xFFE1BEE7)
    val tintColor = if (isDark) Color(0xFFBA68C8) else Color(0xFF8E24AA)
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tintColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Star,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "가장 긴 일정 하이라이트",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = tintColor
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = tintColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        "${duration}일 연속",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = tintColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = onSurfaceColor
            )
            Text(
                "$periodText ($duration 일간)",
                fontSize = 12.sp,
                color = onSurfaceColor.copy(alpha = 0.8f)
            )
        }
    }
}
