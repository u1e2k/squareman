package com.retro.squareman.ui.theme

import androidx.compose.ui.graphics.Color

// Retro Device Backgrounds & Chassis
val RetroDarkBg = Color(0xFF0D0E12)
val RetroPanelBg = Color(0xFF16181F)
val RetroSurface = Color(0xFF1E212B)
val RetroBorder = Color(0xFF2E3440)
val RetroHighlightBorder = Color(0xFF4C566A)

// Text Colors
val RetroTextPrimary = Color(0xFFECEFF4)
val RetroTextSecondary = Color(0xFF9EA7B8)
val RetroTextDim = Color(0xFF5E6573)

// Focus & Accent Colors (PSP / Retro Audio inspired)
val PSPAccentBlue = Color(0xFF00A2FF)
val RetroFocusAmber = Color(0xFFFFB300)
val RetroFocusGreen = Color(0xFF00E676)

// VFD / Spectrum Analyzer Palettes
object SpectrumPalettes {
    // VFD Cyan / Blue (蛍光表示管風)
    val VfdCyan = listOf(
        Color(0xFF00F0FF), // Top / Peak
        Color(0xFF00C8E5),
        Color(0xFF0097A7),
        Color(0xFF006064)  // Base
    )

    // Retro Amber (アンバー単色モニター風)
    val Amber = listOf(
        Color(0xFFFFD54F), // Peak
        Color(0xFFFFB300),
        Color(0xFFFF8F00),
        Color(0xFFFF6F00)  // Base
    )

    // Emerald Green (オールドLCD / オーディオイコライザー風)
    val EmeraldGreen = listOf(
        Color(0xFF69F0AE),
        Color(0xFF00E676),
        Color(0xFF00C853),
        Color(0xFF1B5E20)
    )

    // Multicolor LED (Classic Hi-Fi: Green -> Amber -> Red Peak)
    val Multicolor = listOf(
        Color(0xFFFF1744), // Peak (Red)
        Color(0xFFFF9100), // High (Amber)
        Color(0xFFFFEA00), // Mid (Yellow)
        Color(0xFF00E676)  // Low (Green)
    )
}
