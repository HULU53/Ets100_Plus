package com.hulu.etsplus

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.staticCompositionLocalOf

enum class UiStyle(val displayName: String) {
    Material("Material"),
    Miuix("Miuix")
}

val LocalUiStyle = staticCompositionLocalOf { UiStyle.Material }
val LocalBlurEnabled = staticCompositionLocalOf { true }

fun homeIconContentColor(backgroundColor: Int): Color {
    return if (Color(backgroundColor).luminance() > 0.55f) {
        Color(0xFF171717)
    } else {
        Color.White
    }
}
