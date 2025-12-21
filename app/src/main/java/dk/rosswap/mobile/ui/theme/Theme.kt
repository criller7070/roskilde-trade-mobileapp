package dk.rosswap.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Purple500,
    onPrimary = White,
    secondary = Teal200,
    onSecondary = Black
)

private val DarkColors = darkColorScheme(
    primary = Purple200,
    onPrimary = Black,
    secondary = Teal200,
    onSecondary = Black
)

@Composable
fun RosSwapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = RosSwapTypography,
        shapes = RosSwapShapes,
        content = content
    )
}

