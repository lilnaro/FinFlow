package ru.lilnaro.finflow.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FinFlowDarkColorScheme = darkColorScheme(
    primary = FinFlowPrimary,
    onPrimary = FinFlowTextPrimary,

    primaryContainer = FinFlowSurfaceElevated,
    onPrimaryContainer = FinFlowPrimaryLight,

    secondary = FinFlowSecondary,
    onSecondary = FinFlowBackground,

    secondaryContainer = FinFlowSurfaceSoft,
    onSecondaryContainer = FinFlowSecondary,

    tertiary = FinFlowTertiary,
    onTertiary = FinFlowTextPrimary,

    background = FinFlowBackground,
    onBackground = FinFlowTextPrimary,

    surface = FinFlowSurface,
    onSurface = FinFlowTextPrimary,

    surfaceVariant = FinFlowSurfaceElevated,
    onSurfaceVariant = FinFlowTextSecondary,

    outline = FinFlowBorder,

    error = FinFlowExpense,
    onError = FinFlowTextPrimary,
)

@Composable
fun FinFlowTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = FinFlowDarkColorScheme,
        typography = FinFlowTypography,
        shapes = FinFlowShapes,
        content = content,
    )
}