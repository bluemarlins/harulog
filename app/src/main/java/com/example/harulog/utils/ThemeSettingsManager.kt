package com.example.harulog.utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

@Singleton
class ThemeSettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    // ── 테마 모드 ──────────────────────────────────────────────────────────────
    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private fun loadThemeMode(): ThemeMode {
        val name = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return try {
            ThemeMode.valueOf(name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _themeMode.value = mode
    }

    // ── 월급날 설정 (1~31일, 기본 21일) ──────────────────────────────────────
    private val _salaryDay = MutableStateFlow(prefs.getInt("salary_day", 21))
    val salaryDay: StateFlow<Int> = _salaryDay.asStateFlow()

    fun setSalaryDay(day: Int) {
        val clamped = day.coerceIn(1, 31)
        prefs.edit().putInt("salary_day", clamped).apply()
        _salaryDay.value = clamped
    }
}
