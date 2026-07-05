package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = HDPrimaryDark,
    secondary = HDSecondaryDark,
    tertiary = HDTertiaryDark,
    background = HDBackgroundDark,
    surface = HDSurfaceDark,
    onPrimary = HDOnPrimaryDark,
    onSecondary = HDOnSecondaryDark,
    onTertiary = HDOnTertiaryDark,
    onBackground = HDOnBackgroundDark,
    onSurface = HDOnSurfaceDark,
    error = HDErrorDark,
    errorContainer = HDErrorContainerDark,
    onErrorContainer = HDOnErrorContainerDark,
    primaryContainer = HDPrimaryContainerDark,
    onPrimaryContainer = HDOnPrimaryContainerDark,
    secondaryContainer = HDSecondaryContainerDark,
    onSecondaryContainer = HDOnSecondaryContainerDark,
    tertiaryContainer = HDTertiaryContainerDark,
    onTertiaryContainer = HDOnTertiaryContainerDark,
    outline = HDOutlineDark,
    outlineVariant = HDOutlineVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = HDPrimary,
    secondary = HDSecondary,
    tertiary = HDTertiary,
    background = HDBackground,
    surface = HDSurface,
    onPrimary = HDOnPrimary,
    onSecondary = HDOnSecondary,
    onTertiary = HDOnTertiary,
    onBackground = HDOnBackground,
    onSurface = HDOnSurface,
    error = HDError,
    errorContainer = HDTertiaryContainer,
    onErrorContainer = HDOnTertiaryContainer,
    primaryContainer = HDPrimaryContainer,
    onPrimaryContainer = HDOnPrimaryContainer,
    secondaryContainer = HDSecondaryContainer,
    onSecondaryContainer = HDOnSecondaryContainer,
    tertiaryContainer = HDTertiaryContainer,
    onTertiaryContainer = HDOnTertiaryContainer,
    outline = HDOutline,
    outlineVariant = HDOutlineVariant
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to ensure our premium custom theme is active
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
