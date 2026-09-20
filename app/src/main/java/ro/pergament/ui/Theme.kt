package ro.pergament.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Noapte = Color(0xFF0B0907)
val Lemn = Color(0xFF191410)
val LemnCald = Color(0xFF241C16)
val Aur = Color(0xFFC9A227)
val AurStins = Color(0xFF7C6428)
val Vin = Color(0xFF6B2230)
val Pergam = Color(0xFFEDE0C8)
val PergamStins = Color(0xFF9C8F7A)

val CuloriCoperti = listOf(
    Color(0xFF6B2230),
    Color(0xFF1F3A34),
    Color(0xFF243A56),
    Color(0xFF4A2B14),
    Color(0xFF3A2647),
    Color(0xFF2B2B2B),
    Color(0xFF5A3410),
    Color(0xFF143B3B)
)

private val Schema = darkColorScheme(
    primary = Aur,
    onPrimary = Noapte,
    secondary = Vin,
    onSecondary = Pergam,
    background = Noapte,
    onBackground = Pergam,
    surface = Lemn,
    onSurface = Pergam,
    surfaceVariant = LemnCald,
    onSurfaceVariant = PergamStins,
    outline = AurStins
)

private val Serif = FontFamily.Serif
private val Sans = FontFamily.SansSerif

private val Tipar = Typography(
    displayLarge = TextStyle(
        fontFamily = Serif, fontSize = 38.sp, fontWeight = FontWeight.Normal,
        letterSpacing = 6.sp, lineHeight = 46.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Serif, fontSize = 24.sp, fontWeight = FontWeight.Normal,
        letterSpacing = 1.sp, lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Serif, fontSize = 19.sp, fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp, lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Serif, fontSize = 15.sp, fontWeight = FontWeight.Medium,
        letterSpacing = 0.4.sp, lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Serif, fontSize = 17.sp, lineHeight = 28.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Sans, fontSize = 13.sp, lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Sans, fontSize = 11.sp, letterSpacing = 1.2.sp
    )
)

@Composable
fun PergamentTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Schema, typography = Tipar, content = content)
}

data class TemaLectura(val nume: String, val hartie: Color, val cerneala: Color, val accent: Color)

val TemeLectura = listOf(
    TemaLectura("Pergament", Color(0xFFF2E7D0), Color(0xFF241C12), Color(0xFF8A6F2E)),
    TemaLectura("Hârtie", Color(0xFFFBFAF7), Color(0xFF1A1A1A), Color(0xFF7A7A7A)),
    TemaLectura("Seară", Color(0xFF2A2521), Color(0xFFDCCFB8), Color(0xFFC9A227)),
    TemaLectura("Noapte", Color(0xFF0C0C0D), Color(0xFF9E9E9E), Color(0xFF6B6B6B))
)
