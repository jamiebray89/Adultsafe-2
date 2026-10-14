package uk.co.skills4all.adultsafe.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AdultSafeNavy = Color(0xFF123D71)
val AdultSafeDeepNavy = Color(0xFF071F49)
val AdultSafeTeal = Color(0xFF12BFA8)
val AdultSafeBackground = Color(0xFFEAF5FF)
val AdultSafeText = Color(0xFF2F343B)
val AdultSafeMuted = Color(0xFF667085)

@Composable
fun AdultSafeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = AdultSafeNavy,
            secondary = AdultSafeTeal,
            background = AdultSafeBackground,
            surface = Color.White,
            onPrimary = Color.White,
            onSecondary = Color.White
        ),
        content = content
    )
}
