package org.associations.project.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Custom Color Palette ───────────────────────────────────────────────────

private val DarkNavy = Color(0xFF0D1B2A)
private val DeepBlue = Color(0xFF1B2838)
private val MidBlue = Color(0xFF253B50)
private val SteelBlue = Color(0xFF3A5068)
private val TealAccent = Color(0xFF2EC4B6)
private val TealLight = Color(0xFF5EEADB)
private val TealDark = Color(0xFF009688)
private val Coral = Color(0xFFE63946)
private val CoralLight = Color(0xFFFF6B6B)
private val Amber = Color(0xFFFFB703)
private val AmberLight = Color(0xFFFFD166)
private val PureWhite = Color(0xFFFFFFFF)
private val OffWhite = Color(0xFFF0F4F8)
private val LightGray = Color(0xFFCBD5E1)
private val MediumGray = Color(0xFF8E99A4)
private val SurfaceCard = Color(0xFF162435)
private val SurfaceVariant = Color(0xFF1E3148)

private val DarkColorScheme =
        darkColorScheme(
                primary = TealAccent,
                onPrimary = DarkNavy,
                primaryContainer = MidBlue,
                onPrimaryContainer = TealLight,
                secondary = Amber,
                onSecondary = DarkNavy,
                secondaryContainer = SteelBlue,
                onSecondaryContainer = AmberLight,
                tertiary = TealLight,
                onTertiary = DarkNavy,
                tertiaryContainer = MidBlue,
                onTertiaryContainer = TealLight,
                error = Coral,
                onError = PureWhite,
                errorContainer = Color(0xFF4A1019),
                onErrorContainer = CoralLight,
                background = DarkNavy,
                onBackground = OffWhite,
                surface = DeepBlue,
                onSurface = OffWhite,
                surfaceVariant = SurfaceVariant,
                onSurfaceVariant = LightGray,
                outline = SteelBlue,
                outlineVariant = MidBlue,
                inverseSurface = OffWhite,
                inverseOnSurface = DarkNavy,
                inversePrimary = TealDark,
                surfaceContainerLowest = DarkNavy,
                surfaceContainerLow = Color(0xFF121F30),
                surfaceContainer = DeepBlue,
                surfaceContainerHigh = SurfaceCard,
                surfaceContainerHighest = SurfaceVariant,
        )

private val LightColorScheme =
        lightColorScheme(
                primary = TealDark,
                onPrimary = PureWhite,
                primaryContainer = Color(0xFFB2DFDB),
                onPrimaryContainer = Color(0xFF004D40),
                secondary = Color(0xFFE65100),
                onSecondary = PureWhite,
                secondaryContainer = Color(0xFFFFE0B2),
                onSecondaryContainer = Color(0xFFBF360C),
                tertiary = Color(0xFF00796B),
                onTertiary = PureWhite,
                tertiaryContainer = Color(0xFFA7FFEB),
                onTertiaryContainer = Color(0xFF00251A),
                error = Coral,
                onError = PureWhite,
                background = Color(0xFFF8FAFB),
                onBackground = Color(0xFF1A1C1E),
                surface = PureWhite,
                onSurface = Color(0xFF1A1C1E),
                surfaceVariant = Color(0xFFE7EFF6),
                onSurfaceVariant = Color(0xFF43474E),
                outline = Color(0xFF73777F),
                outlineVariant = Color(0xFFC3C7CE),
        )

@Composable
fun AppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
            colorScheme = colorScheme,
            typography = MaterialTheme.typography,
            content = content
    )
}
