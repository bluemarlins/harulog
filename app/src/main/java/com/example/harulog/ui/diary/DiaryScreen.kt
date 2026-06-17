package com.example.harulog.ui.diary

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.harulog.data.local.entity.DiaryEntity
import com.example.harulog.ui.theme.GrayBorderColor
import java.time.format.DateTimeFormatter

@Composable
fun DiaryScreen(
    viewModel: DiaryViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DiaryPane(
        state = state,
        onSaveDiary = viewModel::saveDiary,
        onDeleteDiary = { viewModel.deleteDiary() },
        modifier = modifier
    )
}

@Composable
fun DiaryPane(
    state: DiaryUiState,
    onSaveDiary: (String) -> Unit,
    onDeleteDiary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDateStr = state.selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
    var text by remember(selectedDateStr, state.currentDiary) {
        mutableStateOf(state.currentDiary?.content ?: "")
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "오늘의 기록 (Diary)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    selectedDateStr,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium
                )
            }

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("오늘 일어났던 일이나 느낀 감정을 기록하세요...") },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = GrayBorderColor
                )
            )

            // Save / Delete Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
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
