package com.example.harulog.ui.diary

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.harulog.ui.theme.GrayBorderColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DiaryScreen(
    viewModel: DiaryViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DiaryPane(
        state         = state,
        onSaveDiary   = viewModel::saveDiary,
        onDeleteDiary = { viewModel.deleteDiary() },
        onSelectDate  = viewModel::selectDate,
        modifier      = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryPane(
    state: DiaryUiState,
    onSaveDiary: (String) -> Unit,
    onDeleteDiary: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDateStr = state.selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
    var text by remember(selectedDateStr, state.currentDiary) {
        mutableStateOf(state.currentDiary?.content ?: "")
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val displayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd (E)", Locale.KOREAN)

    if (showDatePicker) {
        DiaryDatePickerDialog(
            initialDate = state.selectedDate,
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
        modifier = modifier
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
                                imageVector = Icons.Default.Delete,
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
            OutlinedTextField(
                value          = text,
                onValueChange  = { text = it },
                placeholder    = { Text("오늘 일어났던 일이나 느낀 감정을 기록하세요...") },
                modifier       = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape  = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = GrayBorderColor
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
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (state.currentDiary != null) "수정 완료" else "기록 저장")
                    }
                }
            }
            // 모바일 Floating Bottom Bar 영역만큼 하단 여백 추가
            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}

// ── M3 DatePickerDialog ───────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    val initialMillis = remember(initialDate) {
        initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        onConfirm(selectedDate)
                    }
                    onDismiss()
                }
            ) {
                Text("선택")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
