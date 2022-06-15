package cn.fatalc.flyfish.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColors(
    primary = Color(0xFF00796B),
    primaryVariant = Color(0xFF00675b),
    secondary = Color(0xFFffb74d)
)

private val LightColorPalette = lightColors(
    primary = IconBackground,
    primaryVariant = Color(0xFFB28526),
    secondary = Color(0xFF00796B)
)

@Composable
fun FlyFishTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable() () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }
    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}