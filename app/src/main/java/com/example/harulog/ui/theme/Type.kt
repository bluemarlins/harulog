package com.example.harulog.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// We fall back through sans-serif (system-ui equivalent in Android)
private val PinterestFontFamily = FontFamily.SansSerif

val Typography = Typography(
    // {typography.display-xl} - Marketing hero headline
    displayLarge = TextStyle(
        fontFamily = PinterestFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 70.sp,
        lineHeight = 77.sp,
        letterSpacing = (-1.2).sp
    ),
    // {typography.display-lg} - Creator hero
    displayMedium = TextStyle(
        fontFamily = PinterestFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 50.6.sp,
        letterSpacing = (-0.8).sp
    ),
    // {typography.heading-xl} - Section heading
    headlineLarge = TextStyle(
        fontFamily = PinterestFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 33.6.sp,
        letterSpacing = (-1.2).sp
    ),
    // {typography.heading-lg} - Sub-section / Modal Title (Mapped to Harulog's titleLarge)
    titleLarge = TextStyle(
        fontFamily = PinterestFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 27.5.sp,
        letterSpacing = 0.sp
    ),
    // {typography.heading-md} - Card title / In-grid pin label
    titleMedium = TextStyle(
        fontFamily = PinterestFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 23.4.sp,
        letterSpacing = 0.sp
    ),
    // {typography.body-md} - Default body copy (Mapped to Harulog's bodyLarge)
    bodyLarge = TextStyle(
        fontFamily = PinterestFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.4.sp,
        letterSpacing = 0.sp
    ),
    // {typography.body-strong} - Inline emphasis
    bodyMedium = TextStyle(
        fontFamily = PinterestFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.4.sp,
        letterSpacing = 0.sp
    ),
    // {typography.body-sm} - Helper text / grid pin metadata
    bodySmall = TextStyle(
        fontFamily = PinterestFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 19.6.sp,
        letterSpacing = 0.sp
    ),
    // {typography.body-sm-strong} - Table header / Search result count
    labelLarge = TextStyle(
        fontFamily = PinterestFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 19.6.sp,
        letterSpacing = 0.sp
    ),
    // {typography.caption-md} - Default captions (Mapped to Harulog's labelMedium)
    labelMedium = TextStyle(
        fontFamily = PinterestFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    // {typography.caption-sm} - Smallest utility
    labelSmall = TextStyle(
        fontFamily = PinterestFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.8.sp,
        letterSpacing = 0.sp
    )
)

