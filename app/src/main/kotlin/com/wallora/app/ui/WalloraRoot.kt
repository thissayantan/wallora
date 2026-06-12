package com.wallora.app.ui

import androidx.compose.runtime.Composable
import com.wallora.app.ui.navigation.WalloraNavGraph

/** Top-level composable set in [com.wallora.app.MainActivity]. */
@Composable
fun WalloraRoot() {
    WalloraNavGraph()
}
