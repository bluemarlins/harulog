package com.example.harulog

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.harulog.ui.calendar.CalendarViewModel
import com.example.harulog.ui.diary.DiaryViewModel
import com.example.harulog.ui.main.MainScreen
import com.example.harulog.ui.todo.TodoViewModel

@Composable
fun MainNavigation(
    calendarViewModel: CalendarViewModel,
    todoViewModel: TodoViewModel,
    diaryViewModel: DiaryViewModel
) {
  val backStack = rememberNavBackStack(Main)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          MainScreen(
            calendarViewModel = calendarViewModel,
            todoViewModel = todoViewModel,
            diaryViewModel = diaryViewModel,
            onItemClick = { navKey -> backStack.add(navKey) },
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
          )
        }
      },
  )
}
