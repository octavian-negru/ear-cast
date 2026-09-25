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
        primary = Color(0xFF245CC1),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE5EDFF),
        onPrimaryContainer = Color(0xFF193A72),
        secondary = Color(0xFF426179),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE5EDF4),
        onSecondaryContainer = Color(0xFF233E52),
        tertiary = Color(0xFF376C72),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFE0F0F1),
        onTertiaryContainer = Color(0xFF20494E),
        error = Color(0xFFB3261E),
        onError = Color.White,
        errorContainer = Color(0xFFFCE4E2),
        onErrorContainer = Color(0xFF601410),
        background = Color(0xFFF4F6F9),
        onBackground = Color(0xFF1B2432),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF1B2432),
        surfaceVariant = Color(0xFFE9EEF5),
        onSurfaceVariant = Color(0xFF505F73),
        outline = Color(0xFF78869A),
        outlineVariant = Color(0xFFD6DEE9),
        surfaceTint = Color(0xFF245CC1),
        surfaceBright = Color.White,
        surfaceDim = Color(0xFFD6DEE9),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF4F6F9),
        surfaceContainer = Color(0xFFEEF2F7),
        surfaceContainerHigh = Color(0xFFE9EEF5),
        surfaceContainerHighest = Color(0xFFE1E7F0),
    )

private val NightPalette =
    darkColorScheme(
        primary = Color(0xFFA9C7FF),
        onPrimary = Color(0xFF102F66),
        primaryContainer = Color(0xFF25477E),
        onPrimaryContainer = Color(0xFFE5EDFF),
        secondary = Color(0xFFB2CDDF),
        onSecondary = Color(0xFF173447),
        secondaryContainer = Color(0xFF304C60),
        onSecondaryContainer = Color(0xFFE5EDF4),
        tertiary = Color(0xFFA5D3D7),
        onTertiary = Color(0xFF20494E),
        tertiaryContainer = Color(0xFF305A60),
        onTertiaryContainer = Color(0xFFE0F0F1),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF8C1D18),
        onErrorContainer = Color(0xFFFCE4E2),
        background = Color(0xFF101720),
        onBackground = Color(0xFFE5EBF4),
        surface = Color(0xFF182230),
        onSurface = Color(0xFFE5EBF4),
        surfaceVariant = Color(0xFF2B3748),
        onSurfaceVariant = Color(0xFFB8C5D8),
        outline = Color(0xFF8798AE),
        outlineVariant = Color(0xFF2B3748),
        surfaceTint = Color(0xFFA9C7FF),
        surfaceBright = Color(0xFF344154),
        surfaceDim = Color(0xFF101720),
        surfaceContainerLowest = Color(0xFF0C121A),
        surfaceContainerLow = Color(0xFF141D29),
        surfaceContainer = Color(0xFF182230),
        surfaceContainerHigh = Color(0xFF222E3F),
        surfaceContainerHighest = Color(0xFF2B3748),
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
                small = RoundedCornerShape(6.dp),
                medium = RoundedCornerShape(8.dp),
                large = RoundedCornerShape(10.dp),
                extraLarge = RoundedCornerShape(12.dp),
            ),
        content = content,
    )
}
