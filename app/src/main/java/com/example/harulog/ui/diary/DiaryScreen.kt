package com.example.harulog.ui.diary

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.ArrowDropDown
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
import java.time.LocalDate
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

    // 드롭다운 상태
    var dropdownExpanded by remember { mutableStateOf(false) }

    // 날짜 표시 포맷 (예: 2026년 6월 17일)
    val displayFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)", Locale.KOREAN)

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
            // ── 헤더: 제목 + 날짜 드롭다운 ───────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "오늘의 기록 (Diary)",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onBackground
                )

                // 날짜 클릭 → 드롭다운
                Box {
                    Row(
                        modifier = Modifier
                            .clickable(enabled = state.allDiaryDates.isNotEmpty()) {
                                dropdownExpanded = true
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            selectedDateStr,
                            fontSize   = 12.sp,
                            color      = if (state.allDiaryDates.isNotEmpty())
                                            MaterialTheme.colorScheme.primary
                                         else
                                            MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                        if (state.allDiaryDates.isNotEmpty()) {
                            Icon(
                                Icons.Outlined.ArrowDropDown,
                                contentDescription = "날짜 목록 열기",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // ── 드롭다운 메뉴 ─────────────────────────────────────
                    DropdownMenu(
                        expanded         = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        // 헤더
                        Text(
                            "기록된 날짜",
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.secondary,
                            modifier   = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                        HorizontalDivider()

                        state.allDiaryDates.forEach { date ->
                            val isSelected = date == state.selectedDate
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            date.format(displayFormatter),
                                            fontSize   = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color      = if (isSelected)
                                                            MaterialTheme.colorScheme.primary
                                                         else
                                                            MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isSelected) {
                                            Surface(
                                                shape = RoundedCornerShape(20.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    "현재",
                                                    fontSize   = 10.sp,
                                                    color      = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    onSelectDate(date)
                                    dropdownExpanded = false
                                }
                            )
                        }
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

            // ── 저장 / 삭제 버튼 ─────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                if (state.currentDiary != null) {
                    IconButton(
                        onClick = {
                            onDeleteDiary()
                            text = ""
                        }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Diary",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (text.isNotBlank()) {
                            onSaveDiary(text)
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (state.currentDiary != null) "기록 수정" else "기록 저장")
                }
            }
        }
    }
}
