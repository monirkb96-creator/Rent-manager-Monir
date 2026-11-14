package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = PureWhite,
    primaryContainer = EmeraldSoft,
    onPrimaryContainer = EmeraldPrimary,
    secondary = TealAccent,
    onSecondary = PureWhite,
    secondaryContainer = TealSoft,
    onSecondaryContainer = TealAccent,
    tertiary = EmeraldLight,
    onTertiary = PureWhite,
    background = SoftGrayBg,
    onBackground = SlateText,
    surface = PureWhite,
    onSurface = SlateText,
    surfaceVariant = SoftGrayBg,
    onSurfaceVariant = SlateMuted,
    outline = CardBorderGray,
    outlineVariant = EmeraldBorder,
    error = StatusUnpaidText,
    onError = PureWhite,
    errorContainer = StatusUnpaidBg,
    onErrorContainer = StatusUnpaidText
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkEmeraldPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = DarkEmeraldPrimary,
    secondary = DarkEmeraldPrimary,
    onSecondary = DarkOnPrimary,
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextMuted,
    outline = DarkSurfaceVariant,
    error = StatusUnpaidText,
    onError = PureWhite
)

val SmartRentShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SmartRentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = SmartRentTypography,
        shapes = SmartRentShapes,
        content = content
    )
}
