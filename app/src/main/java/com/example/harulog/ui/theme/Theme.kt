package com.example.harulog.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = PinterestRed,
    onPrimaryContainer = Canvas,
    
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = SurfaceDark,
    onSecondaryContainer = Canvas,
    
    tertiary = DarkTertiary,
    onTertiary = Canvas,
    
    background = DarkBackground,
    onBackground = DarkOnBackground,
    
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = SecondaryBg,
    
    outline = HairlineSoft,
    outlineVariant = Hairline,
    
    error = Error,
    onError = Canvas,
    errorContainer = ErrorDeep,
    onErrorContainer = Canvas
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = PinterestRed,
    onPrimaryContainer = Canvas,
    
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = SecondaryBg,
    onSecondaryContainer = Ink,
    
    tertiary = LightTertiary,
    onTertiary = Canvas,
    
    background = LightBackground,
    onBackground = LightOnBackground,
    
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = SurfaceCard,       // M3 컴포넌트(Card 등)가 기본으로 사용하는 배경색
    onSurfaceVariant = Mute,             // 보조 텍스트 색상
    
    outline = Hairline,                  // 보더 및 경계선
    outlineVariant = HairlineSoft,
    
    error = Error,
    onError = Canvas,
    errorContainer = ErrorDeep,
    onErrorContainer = Canvas
)


// Pinterest Shapes: 16px (medium), 32px (large), 8px (small)
val PinterestShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun HarulogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // 커스텀 브랜딩 색상을 강제하기 위해 false로 설정
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = PinterestShapes,
        content = content
    )
}

