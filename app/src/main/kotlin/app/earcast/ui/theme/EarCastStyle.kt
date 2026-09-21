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
        primary = Color(0xFFAF3D1B),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFF9E0D3),
        onPrimaryContainer = Color(0xFF572311),
        secondary = Color(0xFF566544),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE9EDDF),
        onSecondaryContainer = Color(0xFF27321E),
        tertiary = Color(0xFF6C5842),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFF0E7D8),
        onTertiaryContainer = Color(0xFF3D3021),
        error = Color(0xFFBA4D45),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD5),
        onErrorContainer = Color(0xFF3E0805),
        background = Color(0xFFF5F1E9),
        onBackground = Color(0xFF242620),
        surface = Color(0xFFFFFCF6),
        onSurface = Color(0xFF242620),
        surfaceVariant = Color(0xFFEAE5DA),
        onSurfaceVariant = Color(0xFF68685E),
        outline = Color(0xFF858477),
        outlineVariant = Color(0xFFDAD6CB),
    )

private val NightPalette =
    darkColorScheme(
        primary = Color(0xFFFFB598),
        onPrimary = Color(0xFF52210E),
        primaryContainer = Color(0xFF68331F),
        onPrimaryContainer = Color(0xFFF9E0D3),
        secondary = Color(0xFFC2D0AE),
        onSecondary = Color(0xFF2C3723),
        secondaryContainer = Color(0xFF424D37),
        onSecondaryContainer = Color(0xFFE9EDDF),
        tertiary = Color(0xFFDCC3A3),
        onTertiary = Color(0xFF3D3021),
        tertiaryContainer = Color(0xFF554632),
        onTertiaryContainer = Color(0xFFF0E7D8),
        error = Color(0xFFFFB4A9),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF8C1D18),
        onErrorContainer = Color(0xFFFFDAD5),
        background = Color(0xFF1C1E19),
        onBackground = Color(0xFFF3F0E6),
        surface = Color(0xFF272A23),
        onSurface = Color(0xFFF3F0E6),
        surfaceVariant = Color(0xFF3D4137),
        onSurfaceVariant = Color(0xFFC7C9BB),
        outline = Color(0xFF959888),
        outlineVariant = Color(0xFF3D4137),
    )

private val EarCastTypography =
    Typography().let { base ->
        base.copy(
            headlineLarge =
                base.headlineLarge.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 42.sp,
                    lineHeight = 46.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-1.5).sp,
                ),
            headlineMedium =
                base.headlineMedium.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 34.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                ),
            headlineSmall =
                base.headlineSmall.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
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
            )
        } else {
            palette
        }
    MaterialTheme(
        colorScheme = colors,
        typography = EarCastTypography,
        shapes =
            Shapes(
                small = RoundedCornerShape(8.dp),
                medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(32.dp),
            ),
        content = content,
    )
}
