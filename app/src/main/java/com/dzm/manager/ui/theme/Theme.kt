package com.dzm.manager.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val TacticalDarkColorScheme = darkColorScheme(
    primary = AmberSignal,
    onPrimary = AmberOnSignal,
    primaryContainer = AmberContainer,
    onPrimaryContainer = AmberText,
    secondary = EmeraldHud,
    onSecondary = ObsidianBg,
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = EmeraldText,
    tertiary = TextSecondary,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateElevated,
    onSurfaceVariant = TextSecondary,
    outline = HairlineBorder,
    outlineVariant = HairlineBorder,
    error = CrimsonAlert,
    onError = TextPrimary,
    errorContainer = CrimsonContainer,
    onErrorContainer = CrimsonText
)

private val TacticalShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp)
)

@Composable
fun DropZoneTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TacticalDarkColorScheme,
        typography = DropZoneTypography,
        shapes = TacticalShapes,
        content = content
    )
}
