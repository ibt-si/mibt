// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.color.DynamicThemeColorProviders
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import org.interferolog.mibt.MainActivity
import org.interferolog.mibt.ui.theme.fallbackScheme

class PauseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PauseWidget()
}

class PauseWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                DynamicThemeColorProviders
            } else {
                ColorProviders(fallbackScheme())
            }
            GlanceTheme(colors = colors) {
                Column(
                    modifier = GlanceModifier.fillMaxSize().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Пауза 3 секунды",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    Button(
                        text = "Открыть",
                        onClick = actionStartActivity(
                            Intent(context, MainActivity::class.java).putExtra("open", "pause"),
                        ),
                    )
                }
            }
        }
    }
}
