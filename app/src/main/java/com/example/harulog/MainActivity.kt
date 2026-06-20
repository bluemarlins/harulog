package com.example.harulog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.harulog.ui.calendar.CalendarViewModel
import com.example.harulog.ui.diary.DiaryViewModel
import com.example.harulog.ui.theme.HarulogTheme
import com.example.harulog.ui.todo.TodoViewModel
import com.example.harulog.utils.ThemeMode
import com.example.harulog.utils.ThemeSettingsManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  private val calendarViewModel: CalendarViewModel by viewModels()
  private val todoViewModel: TodoViewModel by viewModels()
  private val diaryViewModel: DiaryViewModel by viewModels()

  @Inject
  lateinit var themeSettingsManager: ThemeSettingsManager

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    if (android.os.Build.VERSION.SDK_INT >= 29) {
      window.isNavigationBarContrastEnforced = false
    }
    setContent {
      val themeMode by themeSettingsManager.themeMode.collectAsStateWithLifecycle()
      val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
      }

      HarulogTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MainNavigation(
            calendarViewModel = calendarViewModel,
            todoViewModel = todoViewModel,
            diaryViewModel = diaryViewModel,
            themeSettingsManager = themeSettingsManager
          )
        }
      }
    }
  }
}

