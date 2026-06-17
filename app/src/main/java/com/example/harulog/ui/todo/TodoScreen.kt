package com.example.harulog.ui.todo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.harulog.data.local.entity.CategoryType
import com.example.harulog.data.local.entity.TodoScheduleEntity
import com.example.harulog.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun TodoScreen(
    viewModel: TodoViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardPane(
        state = state,
        onSetCategoryFilter = viewModel::setCategoryFilter,
        onToggleTodo = viewModel::toggleTodo,
        onDeleteTodoSchedule = viewModel::deleteTodoSchedule,
        onAddTodoSchedule = viewModel::addTodoSchedule,
        modifier = modifier
    )
}

@Composable
fun DashboardPane(
    state: TodoUiState,
    onSetCategoryFilter: (CategoryType?) -> Unit,
    onToggleTodo: (TodoScheduleEntity) -> Unit,
    onDeleteTodoSchedule: (TodoScheduleEntity) -> Unit,
    onAddTodoSchedule: (String, String?, Boolean, LocalDate, LocalTime?, LocalTime?, CategoryType, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isAddDialogOpen by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .border(1.dp, GrayBorderColor, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "오늘의 할 일 및 일정",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                IconButton(
                    onClick = { isAddDialogOpen = true },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Item",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category Filter Controls (All, Work, Personal)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    null to "전체",
                    CategoryType.WORK to "업무",
                    CategoryType.PERSONAL to "개인"
                )
                filters.forEach { (cat, label) ->
                    val isSelected = state.selectedCategory == cat
                    val chipColor = when (cat) {
                        CategoryType.WORK -> WorkPrimaryColor
                        CategoryType.PERSONAL -> PersonalPrimaryColor
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Button(
                        onClick = { onSetCategoryFilter(cat) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) chipColor else LightBackground,
                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (state.todos.isEmpty() && state.schedules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "등록된 일정 및 할 일이 없습니다.",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.schedules.isNotEmpty()) {
                        item {
                            Text(
                                "일정 (Schedules)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(state.schedules) { schedule ->
                            ScheduleCardItem(schedule, onDeleteTodoSchedule)
                        }
                    }

                    if (state.todos.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "할 일 (To-Do)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(state.todos) { todo ->
                            TodoCardItem(todo, onToggleTodo, onDeleteTodoSchedule)
                        }
                    }
                }
            }
        }
    }

    if (isAddDialogOpen) {
        AddItemDialog(
            selectedDate = state.selectedDate,
            onDismiss = { isAddDialogOpen = false },
            onAddTodoSchedule = onAddTodoSchedule
        )
    }
}

@Composable
fun ScheduleCardItem(
    schedule: TodoScheduleEntity,
    onDelete: (TodoScheduleEntity) -> Unit
) {
    val themeColor = if (schedule.category == CategoryType.WORK) WorkPrimaryColor else PersonalPrimaryColor
    val bgColor = if (schedule.category == CategoryType.WORK) WorkBackgroundColor else PersonalBackgroundColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, GrayBorderColor, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Edge Accent Bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(24.dp)
                .background(themeColor, RoundedCornerShape(2.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                schedule.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (schedule.startTime != null) {
                val timeStr = if (schedule.endTime != null) {
                    "${schedule.startTime} - ${schedule.endTime}"
                } else {
                    "${schedule.startTime}"
                }
                Text(
                    timeStr,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        IconButton(onClick = { onDelete(schedule) }) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun TodoCardItem(
    todo: TodoScheduleEntity,
    onToggle: (TodoScheduleEntity) -> Unit,
    onDelete: (TodoScheduleEntity) -> Unit
) {
    val themeColor = if (todo.category == CategoryType.WORK) WorkPrimaryColor else PersonalPrimaryColor
    val bgColor = if (todo.category == CategoryType.WORK) WorkBackgroundColor else PersonalBackgroundColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, GrayBorderColor, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = todo.isCompleted,
            onCheckedChange = { onToggle(todo) },
            colors = CheckboxDefaults.colors(checkedColor = themeColor)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                todo.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                color = if (todo.isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    if (!todo.isMonthlyScope) "D-Day형" else "월간 범위형",
                    fontSize = 10.sp,
                    color = themeColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "| ${todo.eventDate.format(DateTimeFormatter.ofPattern(if (todo.isMonthlyScope) "yyyy-MM" else "yyyy-MM-dd"))}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        IconButton(onClick = { onDelete(todo) }) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun AddItemDialog(
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onAddTodoSchedule: (String, String?, Boolean, LocalDate, LocalTime?, LocalTime?, CategoryType, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var itemType by remember { mutableStateOf("TODO") }
    var category by remember { mutableStateOf(CategoryType.WORK) }
    var todoType by remember { mutableStateOf("TARGET_DATE") }
    var time by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "새 항목 생성",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Item Type Tab (Todo vs Schedule)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LightBackground, RoundedCornerShape(8.dp))
                        .padding(2.dp)
                ) {
                    listOf("TODO" to "할 일", "SCHEDULE" to "일정").forEach { (type, label) ->
                        val isSel = itemType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { itemType = type }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("제목을 입력하세요") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Category Selection (WORK or PERSONAL)
                Text("카테고리 선택", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(CategoryType.WORK to "업무", CategoryType.PERSONAL to "개인").forEach { (cat, label) ->
                        val isSel = category == cat
                        val color = if (cat == CategoryType.WORK) WorkPrimaryColor else PersonalPrimaryColor
                        Button(
                            onClick = { category = cat },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) color else LightBackground,
                                contentColor = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(label, fontSize = 12.sp)
                        }
                    }
                }

                // Conditional Inputs based on itemType
                if (itemType == "TODO") {
                    Text("할 일 범위 설정", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("TARGET_DATE" to "D-Day형", "MONTHLY_SCOPE" to "월간 범위형").forEach { (type, label) ->
                            val isSel = todoType == type
                            Button(
                                onClick = { todoType = type },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) MaterialTheme.colorScheme.primary else LightBackground,
                                    contentColor = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // Add button text
                                Text(label, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("시간 정보 (예: 14:00, 선택 사항)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("취소")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                if (itemType == "TODO") {
                                    val isMonthly = todoType == "MONTHLY_SCOPE"
                                    onAddTodoSchedule(
                                        title,
                                        null,
                                        true,
                                        selectedDate,
                                        null,
                                        null,
                                        category,
                                        isMonthly
                                    )
                                } else {
                                    val parsedTime = try {
                                        if (time.isNotBlank()) LocalTime.parse(time.trim()) else null
                                    } catch (e: Exception) {
                                        null
                                    }
                                    onAddTodoSchedule(
                                        title,
                                        null,
                                        false,
                                        selectedDate,
                                        parsedTime,
                                        null,
                                        category,
                                        false
                                    )
                                }
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("생성")
                    }
                }
            }
        }
    }
}
