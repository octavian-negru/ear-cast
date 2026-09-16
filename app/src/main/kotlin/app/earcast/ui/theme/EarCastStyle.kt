@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DayPalette =
    lightColorScheme(
        primary = Color(0xFF145E52),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD7EEE4),
        onPrimaryContainer = Color(0xFF123D35),
        secondary = Color(0xFF49665F),
        secondaryContainer = Color(0xFFE5EEE9),
        onSecondaryContainer = Color(0xFF203D35),
        tertiary = Color(0xFF8C4F35),
        tertiaryContainer = Color(0xFFFFDBC9),
        onTertiaryContainer = Color(0xFF512D1D),
        background = Color(0xFFF6F7F2),
        onBackground = Color(0xFF182C28),
        surface = Color(0xFFFAFBF7),
        onSurface = Color(0xFF182C28),
        surfaceVariant = Color(0xFFE8EDE6),
        onSurfaceVariant = Color(0xFF4B5F57),
        outline = Color(0xFF708279),
        outlineVariant = Color(0xFFD0DAD2),
    )

private val NightPalette =
    darkColorScheme(
        primary = Color(0xFF9AD5BC),
        onPrimary = Color(0xFF11392E),
        primaryContainer = Color(0xFF254F41),
        onPrimaryContainer = Color(0xFFD1F1DF),
        secondary = Color(0xFFB3CBBB),
        secondaryContainer = Color(0xFF2E4238),
        onSecondaryContainer = Color(0xFFD8EBDD),
        tertiary = Color(0xFFF2B693),
        background = Color(0xFF101B17),
        onBackground = Color(0xFFE0EAE1),
        surface = Color(0xFF16221C),
        onSurface = Color(0xFFE0EAE1),
        surfaceVariant = Color(0xFF2E3B32),
        onSurfaceVariant = Color(0xFFB9C9BD),
        outline = Color(0xFF899E8F),
        outlineVariant = Color(0xFF405347),
    )

private val EarCastTypography =
    Typography().let { base ->
        base.copy(
            headlineLarge = base.headlineLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
            headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
            headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            bodyLarge = base.bodyLarge.copy(lineHeight = 25.sp),
            bodyMedium = base.bodyMedium.copy(lineHeight = 22.sp),
        )
    }

/** Ear Cast uses system font scaling, paired light/dark palettes and optional extra contrast. */
@Composable
fun EarCastTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrast: Boolean = false,
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) NightPalette else DayPalette
    val colors =
        if (highContrast) {
            val ink = if (darkTheme) Color.White else Color.Black
            val paper = if (darkTheme) Color.Black else Color.White
            palette.copy(
                primary = ink,
                onPrimary = paper,
                onBackground = ink,
                background = paper,
                surface = paper,
                onSurface = ink,
                onSurfaceVariant = ink,
                outline = ink,
            )
        } else {
            palette
        }
    MaterialTheme(
        colorScheme = colors,
        typography = EarCastTypography,
        shapes =
            Shapes(
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(20.dp),
                large = RoundedCornerShape(28.dp),
            ),
        content = content,
    )
}
