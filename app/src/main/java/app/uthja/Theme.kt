package app.uthja

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Brand = Color(0xFFFF7A1A)
val Bg = Color(0xFF14110F)

private val scheme = darkColorScheme(
    primary = Brand,
    onPrimary = Color.Black,
    secondary = Color(0xFFFFC857),
    background = Bg,
    surface = Bg,
    surfaceVariant = Color(0xFF2A2420),
    onBackground = Color(0xFFF5EDE6),
    onSurface = Color(0xFFF5EDE6),
    onSurfaceVariant = Color(0xFFCBBFB5),
)

@Composable
fun UthJaTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = scheme, content = content)
