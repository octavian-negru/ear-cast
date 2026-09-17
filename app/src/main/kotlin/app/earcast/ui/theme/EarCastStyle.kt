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
        primary = Color(0xFF2C5D63),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFCDE8E8),
        onPrimaryContainer = Color(0xFF06363A),
        secondary = Color(0xFF5E647B),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE1E2EF),
        onSecondaryContainer = Color(0xFF1A1B2E),
        tertiary = Color(0xFFA96F24),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFFE0AE),
        onTertiaryContainer = Color(0xFF352000),
        error = Color(0xFFBA4D45),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD5),
        onErrorContainer = Color(0xFF3E0805),
        background = Color(0xFFF7F5F0),
        onBackground = Color(0xFF1F292C),
        surface = Color(0xFFFFFCF7),
        onSurface = Color(0xFF1F292C),
        surfaceVariant = Color(0xFFE9E9E3),
        onSurfaceVariant = Color(0xFF596368),
        outline = Color(0xFF7B8787),
        outlineVariant = Color(0xFFD1D7D4),
    )

private val NightPalette =
    darkColorScheme(
        primary = Color(0xFF9FCDCC),
        onPrimary = Color(0xFF00363A),
        primaryContainer = Color(0xFF1C4C50),
        onPrimaryContainer = Color(0xFFCDE8E8),
        secondary = Color(0xFFC1C3D9),
        onSecondary = Color(0xFF2B2E43),
        secondaryContainer = Color(0xFF44465C),
        onSecondaryContainer = Color(0xFFE1E2EF),
        tertiary = Color(0xFFE9BA75),
        onTertiary = Color(0xFF422A00),
        tertiaryContainer = Color(0xFF604516),
        onTertiaryContainer = Color(0xFFFFE0AE),
        error = Color(0xFFFFB4A9),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF8C1D18),
        onErrorContainer = Color(0xFFFFDAD5),
        background = Color(0xFF121A1D),
        onBackground = Color(0xFFE0E8E8),
        surface = Color(0xFF192326),
        onSurface = Color(0xFFE0E8E8),
        surfaceVariant = Color(0xFF3E494B),
        onSurfaceVariant = Color(0xFFBEC9C9),
        outline = Color(0xFF899596),
        outlineVariant = Color(0xFF3E494B),
    )

private val EarCastTypography =
    Typography().let { base ->
        base.copy(
            headlineLarge =
                base.headlineLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontSize = 34.sp,
                    lineHeight = 39.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                ),
            headlineMedium =
                base.headlineMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                ),
            headlineSmall =
                base.headlineSmall.copy(
                    fontFamily = FontFamily.Serif,
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
            )
        } else {
            palette
        }
    MaterialTheme(
        colorScheme = colors,
        typography = EarCastTypography,
        shapes =
            Shapes(
                small = RoundedCornerShape(10.dp),
                medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(22.dp),
            ),
        content = content,
    )
}
