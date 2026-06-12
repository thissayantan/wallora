package com.wallora.app.ui.components.adaptive

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

val LocalWindowSizeClass = compositionLocalOf<WindowSizeClass?> { null }

/**
 * Returns the grid column count based on the current window width.
 * Compact → 2, Medium → 3, Expanded → 4.
 */
@Composable
fun gridColumns(): Int {
    val windowSizeClass = LocalWindowSizeClass.current
    return when (windowSizeClass?.widthSizeClass) {
        WindowWidthSizeClass.Medium -> 3
        WindowWidthSizeClass.Expanded -> 4
        else -> 2
    }
}

@Composable
fun isExpandedLayout(): Boolean {
    val windowSizeClass = LocalWindowSizeClass.current
    return windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Expanded
}
