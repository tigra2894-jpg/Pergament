package ro.pergament.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import ro.pergament.data.Carte
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Cum se leaga cartile de pe fiecare raft. */
private data class Legatura(
    val piele: Color,
    val aur: Color,
    val ornament: Ornament,
    val titluSus: Boolean = false
)

private enum class Ornament {
    CHENAR_INFLORAT,   // religie: chenar dublu cu colturi lucrate
    SOBRU,             // filosofie: nimic, doar titlul
    CHENAR_DUBLU,      // istorie
    COLTURI_FINE,      // poezie
    LINIE_SIMPLA,      // literatura
    LINII_DREPTE,      // stiinte, tehnica
    VESEL,             // copii
    RUSTIC,            // gatit
    NATURAL            // limbi straine
}

private val LEGATURI = mapOf(
    "Religie" to Legatura(Color(0xFF241014), Color(0xFFC9A227), Ornament.CHENAR_INFLORAT),
    "Filosofie" to Legatura(Color(0xFF2E2118), Color(0xFFA98C4A), Ornament.SOBRU),
    "Istorie" to Legatura(Color(0xFF22301F), Color(0xFFBE9F3F), Ornament.CHENAR_DUBLU),
    "Poezie" to Legatura(Color(0xFF4A1824), Color(0xFFD3AE4E), Ornament.COLTURI_FINE),
    "Literatură" to Legatura(Color(0xFF1B2A3E), Color(0xFFBFA45C), Ornament.LINIE_SIMPLA),
    "Științe" to Legatura(Color(0xFF26292C), Color(0xFF9EA7AD), Ornament.LINII_DREPTE),
    "Tehnică & Construcții" to Legatura(Color(0xFF2B2622), Color(0xFFA08B5F), Ornament.LINII_DREPTE),
    "Psihologie" to Legatura(Color(0xFF2B2438), Color(0xFFB79CC4), Ornament.LINIE_SIMPLA),
    "Afaceri & Bani" to Legatura(Color(0xFF1E2A26), Color(0xFFC0A85E), Ornament.CHENAR_DUBLU),
    "Sănătate" to Legatura(Color(0xFF1F3330), Color(0xFFA9C0A0), Ornament.LINIE_SIMPLA),
    "Artă & Muzică" to Legatura(Color(0xFF3A2436), Color(0xFFCBA36A), Ornament.COLTURI_FINE),
    "Gătit" to Legatura(Color(0xFF4A2A18), Color(0xFFD6A263), Ornament.RUSTIC),
    "Călătorii" to Legatura(Color(0xFF1D3540), Color(0xFFC3A868), Ornament.CHENAR_DUBLU),
    "Limbi străine" to Legatura(Color(0xFF4A3520), Color(0xFFD8BE8A), Ornament.NATURAL),
    "Copii" to Legatura(Color(0xFF6B2419), Color(0xFFEFC75E), Ornament.VESEL),
    "Benzi desenate" to Legatura(Color(0xFF2A2430), Color(0xFFD0A84E), Ornament.LINII_DREPTE),
    "Manuale & Ghiduri" to Legatura(Color(0xFF2C2C2A), Color(0xFFAFA070), Ornament.LINII_DREPTE),
    "Diverse" to Legatura(Color(0xFF302A24), Color(0xFFB39864), Ornament.LINIE_SIMPLA)
)

private val IMPLICITA = Legatura(Color(0xFF302A24), Color(0xFFB39864), Ornament.LINIE_SIMPLA)

/** Fiecare carte primeste o nuanta proprie, calculata din titlul ei. */
private fun Color.nuanta(seed: Int): Color {
    val a = ((seed / 7) % 13 - 6) / 100f      // -0.06 .. +0.06
    val b = ((seed / 3) % 11 - 5) / 130f
    return Color(
        (red + a).coerceIn(0.04f, 0.92f),
        (green + b).coerceIn(0.03f, 0.88f),
        (blue + a * 0.5f).coerceIn(0.03f, 0.88f),
        alpha
    )
}

private fun Color.inchis(f: Float) = Color(red * f, green * f, blue * f, alpha)

@Composable
fun CopertaPiele(carte: Carte) {
    val panza = texturaPiele()
    val seed = remember(carte.titlu) { abs(carte.titlu.hashCode()) }
    val leg = remember(carte.raft) { LEGATURI[carte.raft] ?: IMPLICITA }
    val piele = remember(leg, seed) { leg.piele.nuanta(seed) }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    0f to piele.inchis(1.18f),
                    0.42f to piele,
                    1f to piele.inchis(0.70f)
                )
            )
            .texturaPeste(panza, 0.62f)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val aur = leg.aur

            // cotorul: banda mai intunecata cu nervuri
            val cotor = w * 0.13f
            drawRect(
                Brush.horizontalGradient(
                    0f to Color.Black.copy(alpha = 0.34f),
                    0.55f to Color.Black.copy(alpha = 0.14f),
                    1f to Color.Transparent
                ),
                size = Size(cotor * 1.6f, h)
            )
            for (i in 1..4) {
                val y = h * (0.16f + i * 0.17f)
                drawRect(
                    aur.copy(alpha = 0.22f),
                    topLeft = Offset(0f, y),
                    size = Size(cotor, 1.4f)
                )
                drawRect(
                    Color.Black.copy(alpha = 0.22f),
                    topLeft = Offset(0f, y + 1.4f),
                    size = Size(cotor, 1.2f)
                )
            }
            // muchia dintre cotor si fata
            drawRect(
                Color.Black.copy(alpha = 0.26f),
                topLeft = Offset(cotor, 0f),
                size = Size(1.2f, h)
            )

            when (leg.ornament) {
                Ornament.CHENAR_INFLORAT -> chenarInflorat(w, h, cotor, aur)
                Ornament.CHENAR_DUBLU -> chenarDublu(w, h, cotor, aur)
                Ornament.COLTURI_FINE -> colturiFine(w, h, cotor, aur)
                Ornament.LINIE_SIMPLA -> chenarSimplu(w, h, cotor, aur, 0.55f)
                Ornament.LINII_DREPTE -> liniiDrepte(w, h, cotor, aur)
                Ornament.VESEL -> vesel(w, h, cotor, aur)
                Ornament.RUSTIC -> rustic(w, h, cotor, aur)
                Ornament.NATURAL -> chenarSimplu(w, h, cotor, aur, 0.40f)
                Ornament.SOBRU -> {}
            }

            // lumina care cade de sus, umbra jos
            drawRect(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.09f),
                    0.28f to Color.Transparent,
                    0.82f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.26f)
                )
            )
        }

        // titlul si autorul, presate in aur
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 22.dp, end = 13.dp, top = 22.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TextAurit(
                text = carte.titlu,
                aur = leg.aur,
                marime = if (carte.titlu.length > 34) 11.sp else 13.sp,
                inaltimeRand = if (carte.titlu.length > 34) 15.sp else 17.sp,
                greutate = FontWeight.SemiBold,
                randuri = 5
            )
            if (carte.autor.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.30f)
                        .height(0.8.dp)
                        .background(leg.aur.copy(alpha = 0.55f))
                )
                Spacer(Modifier.height(10.dp))
                TextAurit(
                    text = carte.autor,
                    aur = leg.aur,
                    marime = 9.5.sp,
                    inaltimeRand = 12.sp,
                    greutate = FontWeight.Normal,
                    randuri = 2,
                    transparenta = 0.78f
                )
            }
        }
    }
}

/** Literele presate: umbra adanca sub ele, apoi aurul deasupra. */
@Composable
private fun TextAurit(
    text: String,
    aur: Color,
    marime: androidx.compose.ui.unit.TextUnit,
    inaltimeRand: androidx.compose.ui.unit.TextUnit,
    greutate: FontWeight,
    randuri: Int,
    transparenta: Float = 1f
) {
    Box(contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = Color.Black.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
            maxLines = randuri,
            overflow = TextOverflow.Ellipsis,
            fontFamily = FontFamily.Serif,
            fontWeight = greutate,
            fontSize = marime,
            lineHeight = inaltimeRand,
            modifier = Modifier.padding(top = 1.2.dp)
        )
        Text(
            text = text,
            color = aur.copy(alpha = transparenta),
            textAlign = TextAlign.Center,
            maxLines = randuri,
            overflow = TextOverflow.Ellipsis,
            fontFamily = FontFamily.Serif,
            fontWeight = greutate,
            fontSize = marime,
            lineHeight = inaltimeRand
        )
    }
}

// ---------- ornamentele ----------

private fun DrawScope.presat(cale: Path, aur: Color, grosime: Float) {
    translate(cale, 0f, 1f) { p ->
        drawPath(p, Color.Black.copy(alpha = 0.42f), style = Stroke(width = grosime))
    }
    drawPath(cale, aur.copy(alpha = 0.82f), style = Stroke(width = grosime))
}

private fun DrawScope.translate(cale: Path, dx: Float, dy: Float, bloc: (Path) -> Unit) {
    val p = Path()
    p.addPath(cale, Offset(dx, dy))
    bloc(p)
}

private fun dreptunghi(x0: Float, y0: Float, x1: Float, y1: Float): Path {
    val p = Path()
    p.moveTo(x0, y0)
    p.lineTo(x1, y0)
    p.lineTo(x1, y1)
    p.lineTo(x0, y1)
    p.close()
    return p
}

private fun DrawScope.chenarSimplu(w: Float, h: Float, cotor: Float, aur: Color, tarie: Float) {
    val m = w * 0.10f
    presat(dreptunghi(cotor + m, m, w - m, h - m), aur.copy(alpha = tarie), 1.1f)
}

private fun DrawScope.chenarDublu(w: Float, h: Float, cotor: Float, aur: Color) {
    val m = w * 0.085f
    presat(dreptunghi(cotor + m, m, w - m, h - m), aur, 1.4f)
    val m2 = m + w * 0.045f
    presat(dreptunghi(cotor + m2, m2, w - m2, h - m2), aur.copy(alpha = 0.6f), 0.8f)
}

private fun DrawScope.chenarInflorat(w: Float, h: Float, cotor: Float, aur: Color) {
    val m = w * 0.085f
    presat(dreptunghi(cotor + m, m, w - m, h - m), aur, 1.5f)
    val m2 = m + w * 0.05f
    presat(dreptunghi(cotor + m2, m2, w - m2, h - m2), aur.copy(alpha = 0.55f), 0.8f)

    // colturi: o floare stilizata din trei petale
    val r = w * 0.075f
    val colturi = listOf(
        Triple(cotor + m, m, 0),
        Triple(w - m, m, 1),
        Triple(w - m, h - m, 2),
        Triple(cotor + m, h - m, 3)
    )
    for ((cx, cy, k) in colturi) {
        val baza = (k * 90f + 45f) * Math.PI.toFloat() / 180f
        val p = Path()
        for (i in 0..2) {
            val unghi = baza + (i - 1) * 0.55f
            val vx = cx + cos(unghi) * r
            val vy = cy + sin(unghi) * r
            p.moveTo(cx, cy)
            p.quadraticBezierTo(
                cx + cos(unghi - 0.4f) * r * 0.7f,
                cy + sin(unghi - 0.4f) * r * 0.7f,
                vx, vy
            )
            p.quadraticBezierTo(
                cx + cos(unghi + 0.4f) * r * 0.7f,
                cy + sin(unghi + 0.4f) * r * 0.7f,
                cx, cy
            )
        }
        presat(p, aur.copy(alpha = 0.75f), 1f)
    }
}

private fun DrawScope.colturiFine(w: Float, h: Float, cotor: Float, aur: Color) {
    val m = w * 0.09f
    val l = w * 0.16f
    val colturi = listOf(
        listOf(Offset(cotor + m, m + l), Offset(cotor + m, m), Offset(cotor + m + l, m)),
        listOf(Offset(w - m - l, m), Offset(w - m, m), Offset(w - m, m + l)),
        listOf(Offset(w - m, h - m - l), Offset(w - m, h - m), Offset(w - m - l, h - m)),
        listOf(Offset(cotor + m + l, h - m), Offset(cotor + m, h - m), Offset(cotor + m, h - m - l))
    )
    for (c in colturi) {
        val p = Path()
        p.moveTo(c[0].x, c[0].y)
        p.lineTo(c[1].x, c[1].y)
        p.lineTo(c[2].x, c[2].y)
        translate(p, 0f, 1f) { q ->
            drawPath(q, Color.Black.copy(alpha = 0.40f), style = Stroke(width = 1.3f))
        }
        drawPath(p, aur.copy(alpha = 0.8f), style = Stroke(width = 1.3f))
    }
}

private fun DrawScope.liniiDrepte(w: Float, h: Float, cotor: Float, aur: Color) {
    val m = w * 0.11f
    for (y in listOf(h * 0.13f, h * 0.87f)) {
        drawRect(
            Color.Black.copy(alpha = 0.35f),
            topLeft = Offset(cotor + m, y + 1f),
            size = Size(w - m - cotor - m, 1.3f)
        )
        drawRect(
            aur.copy(alpha = 0.7f),
            topLeft = Offset(cotor + m, y),
            size = Size(w - m - cotor - m, 1.3f)
        )
    }
}

private fun DrawScope.vesel(w: Float, h: Float, cotor: Float, aur: Color) {
    val m = w * 0.09f
    val p = Path()
    val x0 = cotor + m
    val x1 = w - m
    val y0 = m
    val y1 = h - m
    val r = w * 0.12f
    p.moveTo(x0 + r, y0)
    p.lineTo(x1 - r, y0)
    p.quadraticBezierTo(x1, y0, x1, y0 + r)
    p.lineTo(x1, y1 - r)
    p.quadraticBezierTo(x1, y1, x1 - r, y1)
    p.lineTo(x0 + r, y1)
    p.quadraticBezierTo(x0, y1, x0, y1 - r)
    p.lineTo(x0, y0 + r)
    p.quadraticBezierTo(x0, y0, x0 + r, y0)
    presat(p, aur, 1.6f)
}

private fun DrawScope.rustic(w: Float, h: Float, cotor: Float, aur: Color) {
    val m = w * 0.10f
    presat(dreptunghi(cotor + m, m, w - m, h - m), aur.copy(alpha = 0.65f), 2.2f)
}
