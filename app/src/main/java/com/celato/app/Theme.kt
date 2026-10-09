package com.celato.app

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF7C4DFF),
    secondary = Color(0xFFFF4081)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFB39DFF),
    secondary = Color(0xFFFF80AB)
)

@Composable
fun CelatoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}

/*
 * ---- app/build.gradle.kts dependencies ----
 *
 * dependencies {
 *     val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
 *     implementation(composeBom)
 *     implementation("androidx.compose.material3:material3")
 *     implementation("androidx.compose.material:material-icons-extended")
 *     implementation("androidx.activity:activity-compose:1.9.3")
 *     implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
 * }
 *
 * Setup: Android Studio -> New Project -> "Empty Activity" (Compose),
 * package name: com.celato.app. Copy these 4 files into
 * app/src/main/java/com/celato/app/ (replace the generated MainActivity/theme).
 */
