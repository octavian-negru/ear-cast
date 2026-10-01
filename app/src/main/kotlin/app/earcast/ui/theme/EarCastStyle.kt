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
        primary = Color(0xFF006B5F),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFC5EEE3),
        onPrimaryContainer = Color(0xFF073D34),
        secondary = Color(0xFF69604F),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFEFE5D4),
        onSecondaryContainer = Color(0xFF483E2E),
        tertiary = Color(0xFF765B38),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFF4E7D3),
        onTertiaryContainer = Color(0xFF513B20),
        error = Color(0xFFB3261E),
        onError = Color.White,
        errorContainer = Color(0xFFFCE4E2),
        onErrorContainer = Color(0xFF601410),
        background = Color(0xFFF7F5EF),
        onBackground = Color(0xFF1F302B),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF1F302B),
        surfaceVariant = Color(0xFFECEDE5),
        onSurfaceVariant = Color(0xFF52635A),
        outline = Color(0xFF738077),
        outlineVariant = Color(0xFFD8DED4),
        surfaceTint = Color(0xFF006B5F),
        surfaceBright = Color.White,
        surfaceDim = Color(0xFFD8DED4),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF7F5EF),
        surfaceContainer = Color(0xFFF0F1E9),
        surfaceContainerHigh = Color(0xFFECEDE5),
        surfaceContainerHighest = Color(0xFFE4E8DE),
    )

private val NightPalette =
    darkColorScheme(
        primary = Color(0xFF88D5C2),
        onPrimary = Color(0xFF00382F),
        primaryContainer = Color(0xFF205347),
        onPrimaryContainer = Color(0xFFC5EEE3),
        secondary = Color(0xFFD4C5AB),
        onSecondary = Color(0xFF393022),
        secondaryContainer = Color(0xFF514735),
        onSecondaryContainer = Color(0xFFEFE5D4),
        tertiary = Color(0xFFE4C399),
        onTertiary = Color(0xFF513B20),
        tertiaryContainer = Color(0xFF614A2D),
        onTertiaryContainer = Color(0xFFF4E7D3),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF8C1D18),
        onErrorContainer = Color(0xFFFCE4E2),
        background = Color(0xFF111C18),
        onBackground = Color(0xFFE2EAE1),
        surface = Color(0xFF1B2822),
        onSurface = Color(0xFFE2EAE1),
        surfaceVariant = Color(0xFF334139),
        onSurfaceVariant = Color(0xFFBDCCC0),
        outline = Color(0xFF899B8F),
        outlineVariant = Color(0xFF334139),
        surfaceTint = Color(0xFF88D5C2),
        surfaceBright = Color(0xFF3B4A40),
        surfaceDim = Color(0xFF111C18),
        surfaceContainerLowest = Color(0xFF0B1510),
        surfaceContainerLow = Color(0xFF16221C),
        surfaceContainer = Color(0xFF1B2822),
        surfaceContainerHigh = Color(0xFF26352C),
        surfaceContainerHighest = Color(0xFF334139),
    )

private val EarCastTypography =
    Typography().let { base ->
        base.copy(
            headlineLarge =
                base.headlineLarge.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.5).sp,
                ),
            headlineMedium =
                base.headlineMedium.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 24.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                ),
            headlineSmall =
                base.headlineSmall.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp,
                ),
            titleLarge = base.titleLarge.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
            titleMedium = base.titleMedium.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
            titleSmall = base.titleSmall.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
            bodyLarge = base.bodyLarge.copy(fontSize = 16.sp, lineHeight = 23.sp, letterSpacing = 0.sp),
            bodyMedium = base.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
            bodySmall = base.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = 0.sp),
            labelLarge = base.labelLarge.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
            labelMedium = base.labelMedium.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold),
        )
    }

/** EarCast uses system font scaling, paired light/dark palettes and optional extra contrast. */
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
                primaryContainer = paper,
                onPrimaryContainer = ink,
                secondary = ink,
                onSecondary = paper,
                secondaryContainer = paper,
                onSecondaryContainer = ink,
                tertiary = ink,
                onTertiary = paper,
                tertiaryContainer = paper,
                onTertiaryContainer = ink,
                onBackground = ink,
                background = paper,
                surface = paper,
                onSurface = ink,
                surfaceVariant = paper,
                onSurfaceVariant = ink,
                outline = ink,
                outlineVariant = ink,
                surfaceTint = paper,
                surfaceBright = paper,
                surfaceDim = paper,
                surfaceContainerLowest = paper,
                surfaceContainerLow = paper,
                surfaceContainer = paper,
                surfaceContainerHigh = paper,
                surfaceContainerHighest = paper,
            )
        } else {
            palette
        }
    MaterialTheme(
        colorScheme = colors,
        typography = EarCastTypography,
        shapes =
            Shapes(
                extraSmall = RoundedCornerShape(4.dp),
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(20.dp),
                extraLarge = RoundedCornerShape(28.dp),
            ),
        content = content,
    )
}
