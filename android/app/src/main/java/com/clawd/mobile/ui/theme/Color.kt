package com.clawd.mobile.ui.theme

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color

// Clawd brand colors — dashboard aligned
val ClawdAccent = Color(0xFFB45309)          // amber
val ClawdAccentLight = Color(0xFFD97706)
val ClawdAccentDark = Color(0xFF92400E)

// Light mode
val ClawdBackground = Color(0xFFF5F5F7)
val ClawdSurface = Color(0xFFFFFFFF)
val ClawdSurfaceAlt = Color(0xFFECECEF)
val ClawdText = Color(0xFF18181B)
val ClawdMuted = Color(0xFF6B6B70)
val ClawdBorder = Color(0x14000000)          // rgba(0,0,0,0.08)

// Dark mode
val ClawdBackgroundDark = Color(0xFF111318)    // mockup page bg
val ClawdSurfaceDark = Color(0xFF1A1D26)      // mockup card bg
val ClawdSurfaceAltDark = Color(0xFF18181B)
val ClawdTextDark = Color(0xFFF2F2F2)        // mockup primary text
val ClawdMutedDark = Color(0xFFA1A1AA)
val ClawdSubtleDark = Color(0xFF71717A)
val ClawdBorderDark = Color(0x12FFFFFF)      // rgba(255,255,255,0.07) mockup

// Status colors — dashboard aligned
val ClawdError = Color(0xFFEF4444)           // red
val ClawdBlue = Color(0xFF6366F1)            // thinking indigo

// Legacy aliases for compatibility
val ClawdTextPrimary = ClawdTextDark
val ClawdTextSecondary = ClawdMutedDark
val ClawdTextTertiary = ClawdSubtleDark

// Mockup dark theme — from clawd_mobile_ui_redesign.html
val ClawdDividerDark = Color(0xFF2E2E35)      // divider
val ClawdFaintDark = Color(0xFF52525B)        // meta text, event label
val ClawdGreenBright = Color(0xFF16A34A)      // connected dot, working badge
val ClawdGreenBorder = Color(0x4D168060)      // rgba(22,128,60,0.3) connection badge border

// Floating pet bubble colors (android.graphics.Color int values for traditional View usage)
val BUBBLE_BG = 0xFF1E1E2E.toInt()            // card background
val BUBBLE_TEXT = 0xFFE0E0E0.toInt()          // primary text
val BUBBLE_MUTED = 0xFF888888.toInt()         // secondary/muted text
val BUBBLE_BUTTON_BG = 0xFF2A2A3E.toInt()     // button background
val BUBBLE_DIVIDER = 0x33FFFFFF.toInt()       // 20% white divider

data class BubblePalette(
    val background: Int,
    val text: Int,
    val muted: Int,
    val buttonBackground: Int,
    val divider: Int,
    val hintBackground: Int,
    val accent: Int,
    val hintText: Int,
    val suggestionBackground: Int,
    val swipeNeutral: Int,
    val swipeAllow: Int,
    val swipeDeny: Int,
)

private val DarkBubblePalette = BubblePalette(
    background = 0xFF1E1E2E.toInt(),
    text = 0xFFE0E0E0.toInt(),
    muted = 0xFF888888.toInt(),
    buttonBackground = 0xFF2A2A3E.toInt(),
    divider = 0x33FFFFFF.toInt(),
    hintBackground = 0xCC333333.toInt(),
    accent = 0xFF4FC3F7.toInt(),
    hintText = 0x88FFFFFF.toInt(),
    suggestionBackground = 0x664FC3F7.toInt(),
    swipeNeutral = 0x44444444.toInt(),
    swipeAllow = 0x444CAF50.toInt(),
    swipeDeny = 0x44F44336.toInt(),
)

private val LightBubblePalette = BubblePalette(
    background = 0xFFFFFFFF.toInt(),
    text = 0xFF18181B.toInt(),
    muted = 0xFF6B6B70.toInt(),
    buttonBackground = 0xFFECECEF.toInt(),
    divider = 0x22000000.toInt(),
    hintBackground = 0xFFF1F3F5.toInt(),
    accent = 0xFF0B6E99.toInt(),
    hintText = 0xFF5E6470.toInt(),
    suggestionBackground = 0xFFD9EEF8.toInt(),
    swipeNeutral = 0xFFE5E7EB.toInt(),
    swipeAllow = 0xFFC8E6C9.toInt(),
    swipeDeny = 0xFFFFCDD2.toInt(),
)

fun bubblePalette(context: Context): BubblePalette {
    val mode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return if (mode == Configuration.UI_MODE_NIGHT_YES) DarkBubblePalette else LightBubblePalette
}
