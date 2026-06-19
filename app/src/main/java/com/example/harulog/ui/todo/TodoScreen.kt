package com.example.harulog.ui.todo

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
        onUpdateTodoSchedule = viewModel::updateTodoSchedule,
        modifier = modifier
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardPane(
    state: TodoUiState,
    onSetCategoryFilter: (CategoryType?) -> Unit,
    onToggleTodo: (TodoScheduleEntity) -> Unit,
    onDeleteTodoSchedule: (TodoScheduleEntity) -> Unit,
    onAddTodoSchedule: (String, String?, Boolean, LocalDate, LocalTime?, LocalTime?, CategoryType, Boolean) -> Unit,
    onUpdateTodoSchedule: (TodoScheduleEntity, String, String?, Boolean, LocalDate, LocalTime?, LocalTime?, CategoryType, Boolean) -> Unit,
    modifier: Modifier = Modifier

) {
    var isAddDialogOpen by remember { mutableStateOf(false) }
    var selectedItemForOptions by remember { mutableStateOf<TodoScheduleEntity?>(null) }
    var isOptionsDialogOpen by remember { mutableStateOf(false) }
    var isEditDialogOpen by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<TodoScheduleEntity?>(null) }

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
                Column {
                    Text(
                        "오늘의 할 일 및 일정",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (state.dateRangeText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = state.dateRangeText,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

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
                            ScheduleCardItem(
                                schedule = schedule,
                                onLongClick = {
                                    selectedItemForOptions = schedule
                                    isOptionsDialogOpen = true
                                }
                            )
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
                            TodoCardItem(
                                todo = todo,
                                onToggle = onToggleTodo,
                                onLongClick = {
                                    selectedItemForOptions = todo
                                    isOptionsDialogOpen = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (isAddDialogOpen) {
        AddEditItemDialog(
            selectedDate = state.selectedDate,
            onDismiss = { isAddDialogOpen = false },
            onConfirm = { title, content, isTodo, eventDate, startTime, endTime, category, isMonthlyScope ->
                onAddTodoSchedule(title, content, isTodo, eventDate, startTime, endTime, category, isMonthlyScope)
            }
        )
    }

    if (isOptionsDialogOpen && selectedItemForOptions != null) {
        ItemOptionsDialog(
            item = selectedItemForOptions!!,
            onDismiss = { isOptionsDialogOpen = false },
            onEdit = {
                itemToEdit = selectedItemForOptions
                isOptionsDialogOpen = false
                isEditDialogOpen = true
            },
            onDelete = {
                onDeleteTodoSchedule(selectedItemForOptions!!)
                isOptionsDialogOpen = false
            }
        )
    }

    if (isEditDialogOpen && itemToEdit != null) {
        AddEditItemDialog(
            selectedDate = state.selectedDate,
            editingItem = itemToEdit,
            onDismiss = {
                isEditDialogOpen = false
                itemToEdit = null
            },
            onConfirm = { title, content, isTodo, eventDate, startTime, endTime, category, isMonthlyScope ->
                onUpdateTodoSchedule(itemToEdit!!, title, content, isTodo, eventDate, startTime, endTime, category, isMonthlyScope)
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScheduleCardItem(
    schedule: TodoScheduleEntity,
    onLongClick: () -> Unit
) {
    val themeColor = if (schedule.category == CategoryType.WORK) WorkPrimaryColor else PersonalPrimaryColor
    val bgColor = if (schedule.category == CategoryType.WORK) WorkBackgroundColor else PersonalBackgroundColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, GrayBorderColor, RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            )
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
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TodoCardItem(
    todo: TodoScheduleEntity,
    onToggle: (TodoScheduleEntity) -> Unit,
    onLongClick: () -> Unit
) {
    val themeColor = if (todo.category == CategoryType.WORK) WorkPrimaryColor else PersonalPrimaryColor
    val bgColor = if (todo.category == CategoryType.WORK) WorkBackgroundColor else PersonalBackgroundColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, GrayBorderColor, RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            )
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
    }
}

@Composable
fun AddEditItemDialog(
    selectedDate: LocalDate,
    editingItem: TodoScheduleEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        content: String?,
        isTodo: Boolean,
        eventDate: LocalDate,
        startTime: LocalTime?,
        endTime: LocalTime?,
        category: CategoryType,
        isMonthlyScope: Boolean
    ) -> Unit
) {
    var title by remember { mutableStateOf(editingItem?.title ?: "") }
    var itemType by remember { mutableStateOf(if (editingItem?.isTodo == false) "SCHEDULE" else "TODO") }
    var category by remember { mutableStateOf(editingItem?.category ?: CategoryType.WORK) }
    var todoType by remember { mutableStateOf(if (editingItem?.isMonthlyScope == true) "MONTHLY_SCOPE" else "TARGET_DATE") }
    var time by remember { mutableStateOf(editingItem?.startTime?.toString() ?: "") }

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
                    if (editingItem != null) "항목 수정" else "새 항목 생성",
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
                                val eventDate = editingItem?.eventDate ?: selectedDate
                                if (itemType == "TODO") {
                                    val isMonthly = todoType == "MONTHLY_SCOPE"
                                    onConfirm(
                                        title,
                                        editingItem?.content,
                                        true,
                                        eventDate,
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
                                    onConfirm(
                                        title,
                                        editingItem?.content,
                                        false,
                                        eventDate,
                                        parsedTime,
                                        null,
                                        category,
                                        false
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (editingItem != null) "저장" else "생성")
                    }
                }
            }
        }
    }
}

@Composable
fun ItemOptionsDialog(
    item: TodoScheduleEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, GrayBorderColor)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "\"${item.title}\" 항목 관리",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("수정")
                    }

                    Button(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("삭제")
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("취소", color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}
