package com.example.harulog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.harulog.ui.calendar.CalendarViewModel
import com.example.harulog.ui.diary.DiaryViewModel
import com.example.harulog.ui.theme.HarulogTheme
import com.example.harulog.ui.todo.TodoViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  private val calendarViewModel: CalendarViewModel by viewModels()
  private val todoViewModel: TodoViewModel by viewModels()
  private val diaryViewModel: DiaryViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      HarulogTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MainNavigation(
            calendarViewModel = calendarViewModel,
            todoViewModel = todoViewModel,
            diaryViewModel = diaryViewModel
          )
        }
      }
    }
  }
}
