package app.uthja

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** "Sunrise" palette: night sky turning into dawn. */
object UJ {
    val Night = Color(0xFF0F0B24)
    val Card = Color(0xFF1B1540)
    val CardHi = Color(0xFF261E55)
    val Border = Color(0xFF342A6E)
    val Indigo = Color(0xFF1E1650)
    val Plum = Color(0xFF5A2370)
    val Rose = Color(0xFFB8336A)
    val Orange = Color(0xFFFF8A3D)
    val Pink = Color(0xFFFF4D7E)
    val Gold = Color(0xFFFFC857)
    val SunLight = Color(0xFFFFF1A8)
    val Text = Color(0xFFF7F1FF)
    val Muted = Color(0xFFB3A9D6)
    val Good = Color(0xFF4FD1A5)
    val Bad = Color(0xFFFF6B86)

    val sunrise = Brush.horizontalGradient(listOf(Orange, Pink))
    val sky = Brush.verticalGradient(listOf(Indigo, Plum, Rose, Orange))
}

val Baloo = FontFamily(
    Font(R.font.baloo_regular, FontWeight.Normal),
    Font(R.font.baloo_semibold, FontWeight.Medium),
    Font(R.font.baloo_semibold, FontWeight.SemiBold),
    Font(R.font.baloo_extrabold, FontWeight.Bold),
    Font(R.font.baloo_extrabold, FontWeight.ExtraBold),
    Font(R.font.baloo_extrabold, FontWeight.Black),
)

private fun TextStyle.b() = copy(fontFamily = Baloo)
private val base = Typography()
private val type = Typography(
    displayLarge = base.displayLarge.b(), displayMedium = base.displayMedium.b(), displaySmall = base.displaySmall.b(),
    headlineLarge = base.headlineLarge.b(), headlineMedium = base.headlineMedium.b(), headlineSmall = base.headlineSmall.b(),
    titleLarge = base.titleLarge.b(), titleMedium = base.titleMedium.b(), titleSmall = base.titleSmall.b(),
    bodyLarge = base.bodyLarge.b(), bodyMedium = base.bodyMedium.b(), bodySmall = base.bodySmall.b(),
    labelLarge = base.labelLarge.b(), labelMedium = base.labelMedium.b(), labelSmall = base.labelSmall.b(),
)

private val scheme = darkColorScheme(
    primary = UJ.Orange,
    onPrimary = Color(0xFF1A0E08),
    primaryContainer = Color(0xFF5A2E1C),
    onPrimaryContainer = UJ.Gold,
    secondary = UJ.Gold,
    onSecondary = Color(0xFF1A0E08),
    secondaryContainer = UJ.CardHi,
    onSecondaryContainer = UJ.Text,
    tertiary = UJ.Pink,
    tertiaryContainer = Color(0xFF5C1F38),
    onTertiaryContainer = UJ.Text,
    background = UJ.Night,
    onBackground = UJ.Text,
    surface = UJ.Card,
    onSurface = UJ.Text,
    surfaceVariant = UJ.CardHi,
    onSurfaceVariant = UJ.Muted,
    surfaceContainerHigh = UJ.Card,
    surfaceContainerHighest = UJ.CardHi,
    outline = UJ.Border,
    outlineVariant = UJ.Border,
    error = UJ.Bad,
)

@Composable
fun UthJaTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = scheme,
    typography = type,
    shapes = Shapes(
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(28.dp),
        extraLarge = RoundedCornerShape(32.dp),
    ),
    content = content,
)

// ------------------------------------------------------------ shared pieces

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    color: Color = UJ.Card,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .clip(shape)
            .background(color)
            .border(1.dp, UJ.Border, shape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(18.dp),
        content = content,
    )
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(50))
            .background(UJ.sunrise)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color(0xFF1A0E08), fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .clip(shape)
            .border(1.5.dp, UJ.Orange.copy(alpha = 0.7f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = UJ.Orange, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, bg: Color = UJ.CardHi, fg: Color = UJ.Text) {
    Box(
        modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 14.dp, vertical = 6.dp),
    ) { Text(text, color = fg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun Feedback(msg: String) {
    if (msg.isEmpty()) return
    Text(
        msg,
        color = if (msg.startsWith("✅")) UJ.Good else UJ.Bad,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
    )
}
