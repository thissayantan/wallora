package com.wallora.app.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import com.wallora.app.MainActivity
import com.wallora.app.R

/**
 * Home screen widget showing a "Next wallpaper" button that opens the app.
 * The widget is intentionally simple: full next-wallpaper triggering from a widget
 * requires a WorkManager one-shot job (added in Phase 6 if needed).
 */
class WalloraWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = context.getString(R.string.app_name))
                    Box(
                        modifier = GlanceModifier
                            .padding(top = 8.dp)
                            .clickable(actionStartActivity<MainActivity>()),
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
