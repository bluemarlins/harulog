package com.example.harulog.ui.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.res.painterResource
import com.example.harulog.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import com.example.harulog.ui.theme.GrayBorderColor
import com.example.harulog.ui.theme.LightPrimary
import com.example.harulog.ui.theme.DarkBackground
import com.example.harulog.ui.theme.DarkBorderColor
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DiaryScreen(
    viewModel: DiaryViewModel,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 96.dp)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DiaryPane(
        state         = state,
        onSaveDiary   = viewModel::saveDiary,
        onDeleteDiary = { viewModel.deleteDiary() },
        onSelectDate  = viewModel::selectDate,
        modifier      = modifier,
        contentPadding = contentPadding
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryPane(
    state: DiaryUiState,
    onSaveDiary: (String) -> Unit,
    onDeleteDiary: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 96.dp)
) {
    val selectedDateStr = state.selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
    var text by remember(selectedDateStr, state.currentDiary) {
        mutableStateOf(state.currentDiary?.content ?: "")
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    var isFocused by remember { mutableStateOf(false) }

    val displayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd (E)", Locale.KOREAN)

    if (showDatePicker) {
        DiaryDatePickerDialog(
            initialDate = state.selectedDate,
            allDiaryDates = state.allDiaryDates,
            onDismiss = { showDatePicker = false },
            onConfirm = onSelectDate
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("기록 삭제") },
            text = { Text("오늘의 기록을 정말 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteDiary()
                        text = ""
                        showDeleteDialog = false
                    }
                ) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    Card(
        modifier = modifier.imePadding()
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .border(1.dp, GrayBorderColor, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── 헤더: 제목 + 삭제 아이콘 + 날짜 퀵 네비게이션 ───────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "오늘의 기록 (Diary)",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onBackground
                    )

                    if (state.currentDiary != null) {
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_trash_regular),
                                contentDescription = "기록 삭제",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 날짜 퀵 네비게이터
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { onSelectDate(state.selectedDate.minusDays(1)) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "이전 날짜",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        state.selectedDate.format(displayFormatter),
                        fontSize   = 12.sp,
                        color      = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )

                    IconButton(
                        onClick = { onSelectDate(state.selectedDate.plusDays(1)) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "다음 날짜",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ── 텍스트 입력 영역 ─────────────────────────────────────────
            val textBorderModifier = if (isFocused) {
                Modifier.border(
                    width = 1.5.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(LightPrimary, Color(0xFF6366F1))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            } else {
                Modifier.border(
                    width = 1.5.dp,
                    color = if (isDark) DarkBorderColor else GrayBorderColor,
                    shape = RoundedCornerShape(16.dp)
                )
            }
            OutlinedTextField(
                value          = text,
                onValueChange  = { text = it },
                placeholder    = { Text("오늘 일어났던 일이나 느낀 감정을 기록하세요...") },
                modifier       = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused }
                    .then(textBorderModifier),
                shape  = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            // ── 저장 / 수정 완료 버튼 (동적 렌더링) ────────────────────────
            val isTextChanged = text != (state.currentDiary?.content ?: "")
            val isButtonVisible = isTextChanged && text.isNotBlank()

            if (isButtonVisible) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (text.isNotBlank()) {
                                onSaveDiary(text)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(LightPrimary, Color(0xFF6366F1))
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 24.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (state.currentDiary != null) "수정 완료" else "기록 저장",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
            // 모바일 Floating Bottom Bar 및 시스템 내비게이션 바 영역만큼 하단 여백 추가
            Spacer(modifier = Modifier.height(contentPadding.calculateBottomPadding()))
        }
    }
}

// ── Custom DatePickerDialog (Shows dots for diary entry dates) ─────────
@Composable
fun DiaryDatePickerDialog(
    initialDate: LocalDate,
    allDiaryDates: List<LocalDate>,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "이전 달"
                    )
                }
                Text(
                    text = currentMonth.format(DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN)),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "다음 달"
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 요일 헤더
                Row(modifier = Modifier.fillMaxWidth()) {
                    val days = listOf("일", "월", "화", "수", "목", "금", "토")
                    days.forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (day == "일") Color.Red else MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                // 날짜 그리드
                val firstDay = currentMonth.atDay(1)
                val dayOfWeek = firstDay.dayOfWeek.value % 7 // 0 = Sunday, 1 = Monday, ...
                val daysInMonth = currentMonth.lengthOfMonth()

                val totalCells = ((dayOfWeek + daysInMonth + 6) / 7) * 7
                val dates = ArrayList<LocalDate?>(totalCells)
                for (i in 0 until dayOfWeek) dates.add(null)
                for (i in 1..daysInMonth) dates.add(currentMonth.atDay(i))
                while (dates.size < totalCells) dates.add(null)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val rows = totalCells / 7
                    for (row in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (col in 0 until 7) {
                                val date = dates[row * 7 + col]
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (date != null) {
                                        val isSelected = date == initialDate
                                        val hasDiary = allDiaryDates.contains(date)

                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary
                                                    else Color.Transparent
                                                )
                                                .clickable {
                                                    onConfirm(date)
                                                    onDismiss()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = date.dayOfMonth.toString(),
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) Color.White
                                                    else if (col == 0) Color.Red
                                                    else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (hasDiary) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (isSelected) Color.White
                                                                else MaterialTheme.colorScheme.primary
                                                            )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
