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
import java.time.LocalDate
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
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // ── AI 요약 배너 (플레이스홀더) ────────────────────────────────────
        item {
            AiSummaryBanner()
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

        // ── AI 기능 예정 안내 카드 ─────────────────────────────────────────
        item {
            AiComingSoonCard()
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
    val themeColor = if (todo.category == CategoryType.WORK) WorkPrimaryColor else PersonalPrimaryColor

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
                color    = MaterialTheme.colorScheme.secondary
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
    val themeColor = if (schedule.category == CategoryType.WORK) WorkPrimaryColor else PersonalPrimaryColor
    val bgColor    = if (schedule.category == CategoryType.WORK) WorkBackgroundColor else PersonalBackgroundColor
    val isToday    = schedule.eventDate == today
    val dayFormatter = DateTimeFormatter.ofPattern("M/d (E)", Locale.KOREAN)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, GrayBorderColor, RoundedCornerShape(14.dp))
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
                    color    = if (isToday) themeColor else MaterialTheme.colorScheme.secondary,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                )
                if (schedule.startTime != null) {
                    Text("·", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    Text(schedule.startTime.toString(), fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary)
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, GrayBorderColor, RoundedCornerShape(14.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
    }
}

/** AI 기능 예정 안내 카드 */
@Composable
private fun AiComingSoonCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, GrayBorderColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = null,
            tint     = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Column {
            Text(
                "AI 기능 준비 중",
                fontWeight = FontWeight.Bold,
                fontSize   = 14.sp,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "향후 업데이트에서 주간·월간 AI 요약 및\n맞춤형 우선순위 추천 기능이 제공될 예정입니다.",
                fontSize   = 12.sp,
                color      = MaterialTheme.colorScheme.secondary,
                lineHeight = 18.sp
            )
        }
    }
}
