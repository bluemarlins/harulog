package com.example.harulog.ui.main

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
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

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (!isTablet) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected    = currentTab == 0,
                        onClick     = { currentTab = 0 },
                        icon        = { Icon(Icons.Outlined.DateRange, contentDescription = "Calendar") },
                        label       = { Text("캘린더") }
                    )
                    NavigationBarItem(
                        selected    = currentTab == 1,
                        onClick     = { currentTab = 1 },
                        icon        = { Icon(Icons.Outlined.Edit, contentDescription = "Diary") },
                        label       = { Text("다이어리") }
                    )
                    NavigationBarItem(
                        selected    = currentTab == 2,
                        onClick     = { currentTab = 2 },
                        icon        = { Icon(Icons.Outlined.Analytics, contentDescription = "Dashboard") },
                        label       = { Text("대시보드") }
                    )
                    NavigationBarItem(
                        selected    = currentTab == 3,
                        onClick     = { currentTab = 3 },
                        icon        = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") },
                        label       = { Text("설정") }
                    )
                }
            }
        }
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
                            icon     = { Icon(Icons.Outlined.DateRange, contentDescription = "기록") },
                            label    = { Text("기록") }
                        )
                        NavigationRailItem(
                            selected = currentTab == 1,
                            onClick  = { currentTab = 1 },
                            icon     = { Icon(Icons.Outlined.Analytics, contentDescription = "대시보드") },
                            label    = { Text("대시보드") }
                        )
                        NavigationRailItem(
                            selected = currentTab == 2,
                            onClick  = { currentTab = 2 },
                            icon     = { Icon(Icons.Outlined.Settings, contentDescription = "설정") },
                            label    = { Text("설정") }
                        )
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp)) {
                        when (currentTab) {
                            0 -> {
                                // Master-Detail Split View
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
                                    Column(
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        DiaryScreen(
                                            viewModel = diaryViewModel,
                                            modifier  = Modifier.weight(0.8f)
                                        )
                                    }
                                }
                            }
                            1 -> {
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
                                    BackupRestoreCard(
                                        onExportBackup = onExportBackup,
                                        onImportBackup = onImportBackup
                                    )
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
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    when (currentTab) {
                        // 0: 캘린더 — 상단 캘린더 그리드 + 하단 할 일/일정 리스트
                        0 -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CalendarScreen(
                                    viewModel = calendarViewModel,
                                    modifier  = Modifier.weight(1.2f)
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

