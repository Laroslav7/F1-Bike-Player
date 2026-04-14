package com.example.minus81.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryViolet,
    secondary = PrimaryViolet,
    background = BackgroundBlack,
    surface = SurfaceDark,
    onPrimary = OnPrimary,
    onBackground = OnPrimary,
    onSurface = OnPrimary
)

@Composable
fun Minus81Theme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    // Игнорируем динамические цвета для сохранения стиля приложения
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = context.findActivity()
            activity?.window?.let { window ->
                // Эта строка скрывает предупреждения о том, что метод устарел
                @Suppress("DEPRECATION")
                run {
                    window.statusBarColor = colorScheme.background.toArgb()
                    window.navigationBarColor = colorScheme.background.toArgb()
                }
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}

// Вспомогательная функция для поиска Activity (чтобы не вылетало)
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}