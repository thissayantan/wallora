package com.wallora.app.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import com.wallora.app.MainActivity
import com.wallora.app.R

/**
 * Home screen widget for Wallora.
 *
 * Layout:
 * - App name label (tapping it opens the app)
 * - "Next" button (tapping it applies the next wallpaper in-place via [NextWallpaperAction])
 *
 * C1 fix: primary tap calls [actionRunCallback]<[NextWallpaperAction]>() instead of
 * launching MainActivity. A secondary "Open" label still opens the app.
 */
class WalloraWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // App name — tap to open the app
                    Box(
                        modifier = GlanceModifier
                            .padding(bottom = 4.dp)
                            .clickable(actionStartActivity<MainActivity>()),
                    ) {
                        Text(text = context.getString(R.string.app_name))
                    }
                    // Next button — applies the next wallpaper in place (C1 fix)
                    Box(
                        modifier = GlanceModifier
                            .padding(top = 4.dp)
                            .clickable(actionRunCallback<NextWallpaperAction>()),
                    ) {
                        Text(text = context.getString(R.string.widget_next))
                    }
                }
            }
        }
    }
}

/** AppWidgetProvider bridge for [WalloraWidget]. */
class WalloraWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WalloraWidget()
}
