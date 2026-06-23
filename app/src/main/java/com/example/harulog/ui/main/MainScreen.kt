package com.example.harulog.ui.main

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.res.painterResource
import com.example.harulog.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.harulog.utils.ThemeMode
import com.example.harulog.utils.ThemeSettingsManager

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.harulog.ui.calendar.CalendarScreen
import com.example.harulog.ui.calendar.CalendarViewModel
import com.example.harulog.ui.common.BackupRestoreCard
import com.example.harulog.ui.dashboard.AiDashboardScreen
import com.example.harulog.ui.diary.DiaryScreen
import com.example.harulog.ui.diary.DiaryViewModel
import com.example.harulog.ui.theme.GrayBorderColor
import com.example.harulog.ui.theme.DarkBorderColor
import com.example.harulog.ui.theme.DarkBackground
import com.example.harulog.ui.common.SegmentedControl
import com.example.harulog.ui.todo.TodoViewModel
import java.io.BufferedReader
import java.io.InputStreamReader

private data class TabItem(
    val index: Int,
    val filledIcon: Int,
    val outlinedIcon: Int,
    val label: String
)

@Composable
fun MainScreen(
    calendarViewModel: CalendarViewModel,
    todoViewModel: TodoViewModel,
    diaryViewModel: DiaryViewModel,
    themeSettingsManager: ThemeSettingsManager,
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // JSON export / import launchers
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            val json = todoViewModel.exportToJson()
            if (json != null) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(json.toByteArray())
                    }
                    Toast.makeText(context, "데이터 백업 성공!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "백업 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val json = reader.readText()
                    val success = todoViewModel.importFromJson(json)
                    if (success) {
                        Toast.makeText(context, "데이터 복원 성공!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "복원 실패: 올바르지 않은 백업 파일입니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "복원 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    MainContent(
        calendarViewModel = calendarViewModel,
        todoViewModel     = todoViewModel,
        diaryViewModel    = diaryViewModel,
        themeSettingsManager = themeSettingsManager,
        onExportBackup    = { exportLauncher.launch("harulog_backup.json") },
        onImportBackup    = { importLauncher.launch(arrayOf("application/json")) },
        modifier          = modifier
    )
}

@Composable
internal fun MainContent(
    calendarViewModel: CalendarViewModel,
    todoViewModel: TodoViewModel,
    diaryViewModel: DiaryViewModel,
    themeSettingsManager: ThemeSettingsManager,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isTablet      = configuration.screenWidthDp > 600

    // 탭 순서: 0=캘린더, 1=다이어리, 2=대시보드, 3=설정
    var currentTab by remember { mutableStateOf(0) }
    var isCalendarExpanded by remember { mutableStateOf(true) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isTablet) {
                // ── Tablet Layout: 좌측 Navigation Rail + Split View ──────────
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier       = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        NavigationRailItem(
                            selected = currentTab == 0,
                            onClick  = { currentTab = 0 },
                            icon     = { Icon(painterResource(if (currentTab == 0) R.drawable.ic_calendar_blank_fill else R.drawable.ic_calendar_blank_regular), contentDescription = "캘린더") },
                            label    = { Text("캘린더") }
                        )
                        NavigationRailItem(
                            selected = currentTab == 1,
                            onClick  = { currentTab = 1 },
                            icon     = { Icon(painterResource(if (currentTab == 1) R.drawable.ic_book_open_fill else R.drawable.ic_book_open_regular), contentDescription = "다이어리") },
                            label    = { Text("다이어리") }
                        )
                        NavigationRailItem(
                            selected = currentTab == 2,
                            onClick  = { currentTab = 2 },
                            icon     = { Icon(painterResource(if (currentTab == 2) R.drawable.ic_chart_bar_fill else R.drawable.ic_chart_bar_regular), contentDescription = "대시보드") },
                            label    = { Text("대시보드") }
                        )
                        NavigationRailItem(
                            selected = currentTab == 3,
                            onClick  = { currentTab = 3 },
                            icon     = { Icon(painterResource(if (currentTab == 3) R.drawable.ic_gear_fill else R.drawable.ic_gear_regular), contentDescription = "설정") },
                            label    = { Text("설정") }
                        )
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp)) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(300))) togetherWith
                                            (slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(300)))
                                } else {
                                    (slideInHorizontally { width -> -width } + fadeIn(animationSpec = tween(300))) togetherWith
                                            (slideOutHorizontally { width -> width } + fadeOut(animationSpec = tween(300)))
                                }
                            },
                            label = "TabletTabTransition"
                        ) { targetTab ->
                            when (targetTab) {
                                0 -> {
                                    // 캘린더 + 할일/일정 목록 가로 분할 뷰
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        CalendarScreen(
                                            viewModel = calendarViewModel,
                                            modifier  = Modifier.weight(1.2f).fillMaxHeight()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .width(1.dp)
                                                .background(GrayBorderColor)
                                        )
                                        com.example.harulog.ui.todo.TodoScreen(
                                            viewModel = todoViewModel,
                                            modifier  = Modifier.weight(1f).fillMaxHeight()
                                        )
                                    }
                                }
                                1 -> {
                                    // 다이어리 화면 단독 전체 화면 노출
                                    DiaryScreen(
                                        viewModel = diaryViewModel,
                                        modifier  = Modifier.fillMaxSize()
                                    )
                                }
                                2 -> {
                                    AiDashboardScreen(
                                        todoViewModel = todoViewModel,
                                        modifier      = Modifier.fillMaxSize()
                                    )
                                }
                                else -> {
                                    Column(
                                        modifier = Modifier
                                            .widthIn(max = 600.dp)
                                            .fillMaxHeight()
                                            .padding(vertical = 16.dp),
                                        verticalArrangement   = Arrangement.spacedBy(16.dp),
                                        horizontalAlignment   = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "설정 및 백업",
                                            fontSize   = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color      = MaterialTheme.colorScheme.onBackground,
                                            modifier   = Modifier.padding(bottom = 8.dp)
                                        )
                                        ThemeSettingsCard(themeSettingsManager = themeSettingsManager)
                                        SalaryDaySettingsCard(themeSettingsManager = themeSettingsManager)
                                        DebugDataSettingsCard(todoViewModel = todoViewModel)
                                        BackupRestoreCard(
                                            onExportBackup = onExportBackup,
                                            onImportBackup = onImportBackup
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ── Mobile Layout ─────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 0.dp)
                ) {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(300))) togetherWith
                                        (slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(300)))
                            } else {
                                (slideInHorizontally { width -> -width } + fadeIn(animationSpec = tween(300))) togetherWith
                                        (slideOutHorizontally { width -> width } + fadeOut(animationSpec = tween(300)))
                            }
                        },
                        label = "MobileTabTransition",
                        modifier = Modifier.weight(1f)
                    ) { targetTab ->
                        when (targetTab) {
                            // 0: 캘린더 — 상단 캘린더 그리드 + 하단 할 일/일정 리스트
                            0 -> {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CalendarScreen(
                                        viewModel = calendarViewModel,
                                        isExpanded = isCalendarExpanded,
                                        onToggleExpand = { isCalendarExpanded = !isCalendarExpanded },
                                        modifier  = if (isCalendarExpanded) Modifier.aspectRatio(1f) else Modifier.wrapContentHeight()
                                    )
                                    com.example.harulog.ui.todo.TodoScreen(
                                        viewModel = todoViewModel,
                                        modifier  = Modifier.weight(1f)
                                    )
                                }
                            }
                            // 1: 다이어리
                            1 -> {
                                DiaryScreen(
                                    viewModel = diaryViewModel,
                                    modifier  = Modifier.fillMaxSize()
                                )
                            }
                            // 2: 대시보드 (AI 요약)
                            2 -> {
                                AiDashboardScreen(
                                    todoViewModel = todoViewModel,
                                    modifier      = Modifier.fillMaxSize()
                                )
                            }
                            // 3: 설정
                            3 -> {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement   = Arrangement.spacedBy(16.dp),
                                    horizontalAlignment   = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "설정 및 백업",
                                        fontSize   = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = MaterialTheme.colorScheme.onBackground,
                                        modifier   = Modifier.padding(bottom = 8.dp)
                                    )
                                    ThemeSettingsCard(themeSettingsManager = themeSettingsManager)
                                    SalaryDaySettingsCard(themeSettingsManager = themeSettingsManager)
                                    DebugDataSettingsCard(todoViewModel = todoViewModel)
                                    BackupRestoreCard(
                                        onExportBackup = onExportBackup,
                                        onImportBackup = onImportBackup
                                    )
                                }
                            }
                        }
                    }
                }

                // 모바일용 Floating Bottom Navigation Bar Overlay
                val isDark = MaterialTheme.colorScheme.background == DarkBackground
                val borderColor = if (isDark) DarkBorderColor else GrayBorderColor
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(24.dp))
                        .border(0.8.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tabItems = listOf(
                            TabItem(0, R.drawable.ic_calendar_blank_fill, R.drawable.ic_calendar_blank_regular, "캘린더"),
                            TabItem(1, R.drawable.ic_book_open_fill, R.drawable.ic_book_open_regular, "다이어리"),
                            TabItem(2, R.drawable.ic_chart_bar_fill, R.drawable.ic_chart_bar_regular, "대시보드"),
                            TabItem(3, R.drawable.ic_gear_fill, R.drawable.ic_gear_regular, "설정")
                        )
                        tabItems.forEach { item ->
                            val isSelected = currentTab == item.index
                            val tintColor = if (isSelected) MaterialTheme.colorScheme.onBackground
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            val iconRes = if (isSelected) item.filledIcon else item.outlinedIcon

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { currentTab = item.index },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .height(32.dp)
                                        .width(64.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.10f)
                                            else Color.Transparent
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = iconRes),
                                        contentDescription = item.label,
                                        tint = tintColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.label,
                                    color = tintColor,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ThemeSettingsCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ThemeSettingsCard(
    themeSettingsManager: ThemeSettingsManager,
    modifier: Modifier = Modifier
) {
    val themeMode by themeSettingsManager.themeMode.collectAsStateWithLifecycle()

    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, if (isDark) DarkBorderColor else GrayBorderColor, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "디자인 테마 설정",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            SegmentedControl(
                items = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK),
                selectedItem = themeMode,
                onItemSelect = { themeSettingsManager.setThemeMode(it) },
                labelProvider = { mode ->
                    when (mode) {
                        ThemeMode.SYSTEM -> "시스템 기본"
                        ThemeMode.LIGHT -> "라이트"
                        ThemeMode.DARK -> "다크"
                    }
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ─────────────────────────────────────────────────────────────────────────────
// SalaryDaySettingsCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SalaryDaySettingsCard(themeSettingsManager: ThemeSettingsManager) {
    val isDark       = MaterialTheme.colorScheme.background == DarkBackground
    val borderColor  = if (isDark) DarkBorderColor else GrayBorderColor
    val currentDay   by themeSettingsManager.salaryDay.collectAsStateWithLifecycle()

    val lazyListState = rememberLazyListState()
    val configuration = LocalConfiguration.current
    val density       = LocalDensity.current

    val screenWidthDp = configuration.screenWidthDp
    val cardWidthDp   = if (screenWidthDp > 600) 600 else screenWidthDp
    val lazyRowWidthDp = cardWidthDp - 32 // 16.dp horizontal padding * 2

    // 가로 스크롤 칩들의 중앙 정렬을 위한 offset 계산 (px)
    // (LazyRow 전체 가용 가로 폭의 절반) - (아이템의 크기 36dp의 절반)
    val scrollOffsetPx = with(density) {
        -((lazyRowWidthDp / 2) - 18).dp.toPx().toInt()
    }

    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(currentDay) {
        val targetIndex = currentDay - 1
        if (targetIndex in 0..30) {
            if (!isInitialized) {
                lazyListState.scrollToItem(targetIndex, scrollOffsetPx)
                isInitialized = true
            } else {
                lazyListState.animateScrollToItem(targetIndex, scrollOffsetPx)
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border   = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 헤더
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        "월급날 설정",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "매달 ${currentDay}일에 월급을 받아요",
                        fontSize = 12.sp,
                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // 날짜 선택 — 가로 스크롤 칩
            LazyRow(
                state                 = lazyListState,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding        = PaddingValues(horizontal = 2.dp)
            ) {
                items((1..31).toList()) { day ->
                    val selected = day == currentDay
                    Surface(
                        shape   = RoundedCornerShape(50),
                        color   = if (selected)
                                      MaterialTheme.colorScheme.primary
                                  else
                                      MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { themeSettingsManager.setSalaryDay(day) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text       = day.toString(),
                                fontSize   = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color      = if (selected)
                                                 MaterialTheme.colorScheme.onPrimary
                                             else
                                                 MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DebugDataSettingsCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DebugDataSettingsCard(
    todoViewModel: TodoViewModel,
    modifier: Modifier = Modifier
) {
    val isDebugEnabled by todoViewModel.isDebugDataEnabled.collectAsStateWithLifecycle()
    val isDark = MaterialTheme.colorScheme.background == DarkBackground

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, if (isDark) DarkBorderColor else GrayBorderColor, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "디버그 더미 데이터 활성화",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "활성화 시 테스트용 더미 일정 및 할 일이 생성됩니다.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                Switch(
                    checked = isDebugEnabled,
                    onCheckedChange = { todoViewModel.setDebugDataEnabled(it) }
                )
            }
        }
    }
}

