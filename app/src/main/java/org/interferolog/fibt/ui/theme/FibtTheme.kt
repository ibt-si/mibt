package org.interferolog.fibt.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun FibtTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        dark -> expressiveDarkColorScheme()
        else -> expressiveLightColorScheme()
    }
    MaterialExpressiveTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}

@Composable
fun fallbackScheme(): ColorScheme =
    if (isSystemInDarkTheme()) expressiveDarkColorScheme() else expressiveLightColorScheme()

/**
 * Material 3 1.5 ships [expressiveLightColorScheme] as the baseline light scheme with on-container
 * tones shifted from 10 to 30. The matching dark shift moves on-container tones from 90 to 80.
 */
fun expressiveDarkColorScheme(): ColorScheme = darkColorScheme(
    onPrimaryContainer = Color(0xFFD0BCFF),
    onSecondaryContainer = Color(0xFFCCC2DC),
    onTertiaryContainer = Color(0xFFEFB8C8),
    onErrorContainer = Color(0xFFF2B8B5),
)
