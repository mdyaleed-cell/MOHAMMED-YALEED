package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class LedgerSemanticColors(
    val income: Color = IncomeGreen,
    val incomeContainer: Color = IncomeGreenSoft,
    val expense: Color = ExpenseRed,
    val expenseContainer: Color = ExpenseRedSoft,
    val warning: Color = WarningAmber,
    val warningContainer: Color = WarningAmberSoft,
)

val LocalLedgerColors = staticCompositionLocalOf { LedgerSemanticColors() }

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = AmberSecondary,
    onSecondary = Color.White,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = OnAmberContainer,
    tertiary = TealTertiary,
    onTertiary = Color.White,
    tertiaryContainer = TealContainer,
    background = WarmCreamBg,
    onBackground = InkDark,
    surface = SoftPaperSurface,
    onSurface = InkDark,
    surfaceVariant = SoftPaperVariant,
    onSurfaceVariant = InkMuted,
    outline = SubtleOutline,
    error = ExpenseRed,
    onError = Color.White,
    errorContainer = ExpenseRedSoft,
    onErrorContainer = ExpenseRed
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = Color(0xFF3E2800),
    secondaryContainer = Color(0xFF5A3D00),
    onSecondaryContainer = Color(0xFFFFECC7),
    tertiary = Color(0xFF81D2DC),
    background = DarkBg,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkInkMuted,
    outline = DarkOutline,
    error = Color(0xFFFF7A7A),
    onError = Color(0xFF5C0000),
    errorContainer = Color(0xFF4D1A1A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun HisabKhataTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (darkTheme) {
        LedgerSemanticColors(
            income = Color(0xFF52D69E),
            incomeContainer = Color(0xFF0E3D2B),
            expense = Color(0xFFFF7A7A),
            expenseContainer = Color(0xFF4A1B1B),
            warning = Color(0xFFFFC15E),
            warningContainer = Color(0xFF473000)
        )
    } else {
        LedgerSemanticColors()
    }

    CompositionLocalProvider(LocalLedgerColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
