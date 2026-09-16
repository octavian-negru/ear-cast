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
        primary = Color(0xFFA12A2A),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFDAD6),
        onPrimaryContainer = Color(0xFF410004),
        secondary = Color(0xFF355C85),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD4E4FA),
        onSecondaryContainer = Color(0xFF001D35),
        tertiary = Color(0xFF795900),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFFDF9A),
        onTertiaryContainer = Color(0xFF261A00),
        error = Color(0xFFBA1A1A),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFEF9F7),
        onBackground = Color(0xFF211A1A),
        surface = Color(0xFFFFFBFF),
        onSurface = Color(0xFF211A1A),
        surfaceVariant = Color(0xFFF4DDDA),
        onSurfaceVariant = Color(0xFF534342),
        outline = Color(0xFF857371),
        outlineVariant = Color(0xFFD8C2BF),
    )

private val NightPalette =
    darkColorScheme(
        primary = Color(0xFFFFB3AD),
        onPrimary = Color(0xFF680008),
        primaryContainer = Color(0xFF85171D),
        onPrimaryContainer = Color(0xFFFFDAD6),
        secondary = Color(0xFFA5C9F4),
        onSecondary = Color(0xFF003257),
        secondaryContainer = Color(0xFF174A73),
        onSecondaryContainer = Color(0xFFD5E4FF),
        tertiary = Color(0xFFE9C271),
        onTertiary = Color(0xFF3F2E00),
        tertiaryContainer = Color(0xFF5A4300),
        onTertiaryContainer = Color(0xFFFFDF9A),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF171212),
        onBackground = Color(0xFFEEE0DE),
        surface = Color(0xFF1E1818),
        onSurface = Color(0xFFEEE0DE),
        surfaceVariant = Color(0xFF534342),
        onSurfaceVariant = Color(0xFFD8C2BF),
        outline = Color(0xFFA38C89),
        outlineVariant = Color(0xFF53433F),
    )

private val EarCastTypography =
    Typography().let { base ->
        base.copy(
            headlineLarge =
                base.headlineLarge.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 30.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                ),
            headlineMedium =
                base.headlineMedium.copy(
                    fontSize = 24.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                ),
            headlineSmall =
                base.headlineSmall.copy(
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
            labelLarge = base.labelLarge.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
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
            )
        } else {
            palette
        }
    MaterialTheme(
        colorScheme = colors,
        typography = EarCastTypography,
        shapes =
            Shapes(
                small = RoundedCornerShape(4.dp),
                medium = RoundedCornerShape(8.dp),
                large = RoundedCornerShape(12.dp),
            ),
        content = content,
    )
}
