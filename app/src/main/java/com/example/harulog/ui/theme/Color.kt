package com.example.harulog.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// 1. Pinterest Core Color Tokens
// ==========================================

// Brand & Accent
val PinterestRed = Color(0xFFE60023)
val PinterestRedPressed = Color(0xFFCC001F)

// Surface
val Canvas = Color(0xFFFFFFFF)
val SoftSurface = Color(0xFFFBFBF9)
val SurfaceCard = Color(0xFFF6F6F3)
val SecondaryBg = Color(0xFFE5E5E0)
val SecondaryPressed = Color(0xFFC8C8C1)
val SurfaceDark = Color(0xFF262622)
val Hairline = Color(0xFFDADAD3)
val HairlineSoft = Color(0xFFE5E5E0)

// Text
val Ink = Color(0xFF000000)
val InkSoft = Color(0xFF211922)
val Body = Color(0xFF33332E)
val Charcoal = Color(0xFF262622)
val Mute = Color(0xFF62625B)
val Ash = Color(0xFF91918C)
val Stone = Color(0xFFC8C8C1)
val OnDark = Color(0xFFFFFFFF)

// Semantic
val Error = Color(0xFF9E0A0A)
val ErrorDeep = Color(0xFFCC001F)
val SuccessDeep = Color(0xFF103C25)
val SuccessPale = Color(0xFFC7F0DA)
val FocusOuter = Color(0xFF435EE5)
val FocusInner = Color(0xFFFFFFFF)

// Editorial Accents
val AccentPressedBlue = Color(0xFF617BFF)
val AccentPurple = Color(0xFF7E238B)
val AccentPurpleDeep = Color(0xFF6845AB)


// ==========================================
// 2. Mapping Harulog Colors to Pinterest Tokens
// (Ensures 100% Backwards Compatibility & No Build Errors)
// ==========================================

// Light Scheme Mapping
val LightPrimary = PinterestRed
val LightSecondary = Mute
val LightTertiary = AccentPurple
val LightBackground = SurfaceCard
val LightSurface = Canvas
val LightOnPrimary = Canvas
val LightOnSecondary = Ink
val LightOnBackground = Body
val LightOnSurface = Body

// Dark Scheme Mapping
val DarkPrimary = PinterestRed
val DarkSecondary = SecondaryBg
val DarkTertiary = AccentPurpleDeep
val DarkBackground = SurfaceDark
val DarkSurface = SurfaceDark
val DarkOnPrimary = Canvas
val DarkOnSecondary = Ink
val DarkOnBackground = Canvas
val DarkOnSurface = Canvas

// Category & Feature Mapping
val WorkPrimaryColor = AccentPurpleDeep
val WorkBackgroundColor = SoftSurface
val WorkDarkBackgroundColor = SurfaceDark
val WorkDarkPrimaryColor = AccentPressedBlue

val PersonalPrimaryColor = PinterestRed
val PersonalBackgroundColor = SoftSurface
val PersonalDarkBackgroundColor = SurfaceDark
val PersonalDarkPrimaryColor = PinterestRed

val SuccessWorkoutColor = SuccessDeep
val GrayBorderColor = Hairline
val DarkBorderColor = HairlineSoft

