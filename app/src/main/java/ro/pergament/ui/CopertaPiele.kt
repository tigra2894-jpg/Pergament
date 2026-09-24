package ro.pergament.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
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
import ro.pergament.data.Carte
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Cum arata legatura cartilor de pe fiecare raft. */
private enum class Legatura { MARO, VISINIE, VERDE, ALBASTRA, CARAMEL }

private enum class Ornament {
    CHENAR_INFLORAT,   // religie
    SOBRU,             // filosofie
    CHENAR_DUBLU,      // istorie, afaceri, calatorii
    COLTURI_FINE,      // poezie, arta
    LINIE_SIMPLA,      // literatura, psihologie, sanatate
    LINII_DREPTE,      // stiinte, tehnica, manuale
    VESEL,             // copii
    RUSTIC             // gatit
}

private data class Stil(val legatura: Legatura, val ornament: Ornament, val aur: Color)

private val AUR_CALD = Color(0xFFD9B44A)
private val AUR_STINS = Color(0xFFB09256)
private val AUR_ALB = Color(0xFFDCC9A0)

private val STILURI = mapOf(
    "Religie" to Stil(Legatura.VISINIE, Ornament.CHENAR_INFLORAT, AUR_CALD),
    "Filosofie" to Stil(Legatura.MARO, Ornament.SOBRU, AUR_STINS),
    "Istorie" to Stil(Legatura.VERDE, Ornament.CHENAR_DUBLU, AUR_CALD),
    "Poezie" to Stil(Legatura.VISINIE, Ornament.COLTURI_FINE, AUR_CALD),
    "Literatură" to Stil(Legatura.ALBASTRA, Ornament.LINIE_SIMPLA, AUR_STINS),
    "Științe" to Stil(Legatura.ALBASTRA, Ornament.LINII_DREPTE, AUR_ALB),
    "Tehnică & Construcții" to Stil(Legatura.MARO, Ornament.LINII_DREPTE, AUR_STINS),
    "Psihologie" to Stil(Legatura.VERDE, Ornament.LINIE_SIMPLA, AUR_STINS),
    "Afaceri & Bani" to Stil(Legatura.VERDE, Ornament.CHENAR_DUBLU, AUR_CALD),
    "Sănătate" to Stil(Legatura.VERDE, Ornament.LINIE_SIMPLA, AUR_ALB),
    "Artă & Muzică" to Stil(Legatura.VISINIE, Ornament.COLTURI_FINE, AUR_CALD),
    "Gătit" to Stil(Legatura.CARAMEL, Ornament.RUSTIC, AUR_STINS),
    "Călătorii" to Stil(Legatura.ALBASTRA, Ornament.CHENAR_DUBLU, AUR_CALD),
    "Limbi străine" to Stil(Legatura.CARAMEL, Ornament.LINIE_SIMPLA, AUR_STINS),
    "Copii" to Stil(Legatura.VISINIE, Ornament.VESEL, AUR_CALD),
    "Benzi desenate" to Stil(Legatura.ALBASTRA, Ornament.LINII_DREPTE, AUR_ALB),
    "Manuale & Ghiduri" to Stil(Legatura.MARO, Ornament.LINII_DREPTE, AUR_STINS),
    "Diverse" to Stil(Legatura.MARO, Ornament.LINIE_SIMPLA, AUR_STINS)
)

private val IMPLICIT = Stil(Legatura.MARO, Ornament.LINIE_SIMPLA, AUR_STINS)

@Composable
fun CopertaPiele(carte: Carte) {
    val stil = remember(carte.raft) { STILURI[carte.raft] ?: IMPLICIT }
    val seed = remember(carte.titlu) { abs(carte.titlu.hashCode()) }

    // fiecare carte primeste pielea putin altfel asezata, ca sa nu semene doua la fel
    val scara = 0.46f + (seed % 7) * 0.035f
    val piele = when (stil.legatura) {
        Legatura.MARO -> pieleMaro(scara)
        Legatura.VISINIE -> pieleVisinie(scara)
        Legatura.VERDE -> pieleVerde(scara)
        Legatura.ALBASTRA -> pieleAlbastra(scara)
        Legatura.CARAMEL -> pieleCaramel(scara)
    }
    val foitaAur = aurFoita(0.22f)

    Box(
        Modifier
            .fillMaxSize()
            .material(piele)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val aur = stil.aur

            // ---------- cotorul ----------
            val cotor = w * 0.135f
            drawRect(
                Brush.horizontalGradient(
                    0f to Color.Black.copy(alpha = 0.40f),
                    0.50f to Color.Black.copy(alpha = 0.16f),
                    1f to Color.Transparent
                ),
                size = Size(cotor * 1.5f, h)
            )
            // nervurile cotorului, ca la cartile cusute de mana
            for (i in 1..4) {
                val y = h * (0.15f + i * 0.175f)
                drawRect(
                    Color.Black.copy(alpha = 0.30f),
                    topLeft = Offset(0f, y - 2.2f),
                    size = Size(cotor, 2.2f)
                )
                drawRect(
                    Color.White.copy(alpha = 0.10f),
                    topLeft = Offset(0f, y),
                    size = Size(cotor, 1.4f)
                )
                drawRect(
                    aur.copy(alpha = 0.16f),
                    topLeft = Offset(0f, y + 1.4f),
                    size = Size(cotor, 1f)
                )
            }
            drawRect(
                Color.Black.copy(alpha = 0.28f),
                topLeft = Offset(cotor, 0f),
                size = Size(1.3f, h)
            )

            // ---------- ornamentul, dupa gen ----------
            when (stil.ornament) {
                Ornament.CHENAR_INFLORAT -> chenarInflorat(w, h, cotor, aur)
                Ornament.CHENAR_DUBLU -> chenarDublu(w, h, cotor, aur)
                Ornament.COLTURI_FINE -> colturiFine(w, h, cotor, aur)
                Ornament.LINIE_SIMPLA -> chenarSimplu(w, h, cotor, aur, 0.52f)
                Ornament.LINII_DREPTE -> liniiDrepte(w, h, cotor, aur)
                Ornament.VESEL -> vesel(w, h, cotor, aur)
                Ornament.RUSTIC -> rustic(w, h, cotor, aur)
                Ornament.SOBRU -> {}
            }

            // ---------- lumina camerei pe coperta ----------
            drawRect(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.10f),
                    0.30f to Color.Transparent,
                    0.78f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.28f)
                )
            )
        }

        // ---------- titlul si autorul, presate in aur ----------
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 13.dp, top = 20.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LiniaAur(stil.aur, foitaAur, 0.36f)
            Spacer(Modifier.height(11.dp))
            TextPresat(
                text = carte.titlu,
                aur = stil.aur,
                foita = foitaAur,
                marime = if (carte.titlu.length > 34) 11.sp else 13.sp,
                inaltime = if (carte.titlu.length > 34) 15.sp else 17.sp,
                greutate = FontWeight.SemiBold,
                randuri = 5
            )
            Spacer(Modifier.height(11.dp))
            LiniaAur(stil.aur, foitaAur, 0.36f)

            if (carte.autor.isNotBlank()) {
                Spacer(Modifier.height(15.dp))
                TextPresat(
                    text = carte.autor,
                    aur = stil.aur,
                    foita = foitaAur,
                    marime = 9.5.sp,
                    inaltime = 12.sp,
                    greutate = FontWeight.Normal,
                    randuri = 2,
                    transparenta = 0.85f
                )
            }
        }
    }
}

@Composable
private fun LiniaAur(aur: Color, foita: Brush?, latime: Float) {
    Box(
        Modifier
            .fillMaxWidth(latime)
            .height(1.1.dp)
            .material(foita)
            .then(
                if (foita == null) Modifier.material(Brush.horizontalGradient(listOf(aur, aur)))
                else Modifier
            )
    )
}

/**
 * Literele presate: mai intai umbra adanca dedesubt,
 * apoi litera cu foita de aur deasupra.
 */
@Composable
private fun TextPresat(
    text: String,
    aur: Color,
    foita: Brush?,
    marime: androidx.compose.ui.unit.TextUnit,
    inaltime: androidx.compose.ui.unit.TextUnit,
    greutate: FontWeight,
    randuri: Int,
    transparenta: Float = 1f
) {
    Box(contentAlignment = Alignment.Center) {
        // adancitura in piele
        Text(
            text = text,
            color = Color.Black.copy(alpha = 0.62f),
            textAlign = TextAlign.Center,
            maxLines = randuri,
            overflow = TextOverflow.Ellipsis,
            fontFamily = FontFamily.Serif,
            fontWeight = greutate,
            fontSize = marime,
            lineHeight = inaltime,
            modifier = Modifier.padding(top = 1.4.dp)
        )
        // litera aurita
        if (foita != null) {
            Text(
                text = text,
                brush = foita,
                alpha = transparenta,
                textAlign = TextAlign.Center,
                maxLines = randuri,
                overflow = TextOverflow.Ellipsis,
                fontFamily = FontFamily.Serif,
                fontWeight = greutate,
                fontSize = marime,
                lineHeight = inaltime
            )
        } else {
            Text(
                text = text,
                color = aur.copy(alpha = transparenta),
                textAlign = TextAlign.Center,
                maxLines = randuri,
                overflow = TextOverflow.Ellipsis,
                fontFamily = FontFamily.Serif,
                fontWeight = greutate,
                fontSize = marime,
                lineHeight = inaltime
            )
        }
    }
}

// ---------- ornamentele presate in piele ----------

private fun DrawScope.presat(cale: Path, aur: Color, grosime: Float) {
    val umbra = Path()
    umbra.addPath(cale, Offset(0f, 1.3f))
    drawPath(umbra, Color.Black.copy(alpha = 0.45f), style = Stroke(width = grosime))
    drawPath(cale, aur.copy(alpha = 0.85f), style = Stroke(width = grosime))
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
    presat(dreptunghi(cotor + m, m, w - m, h - m), aur.copy(alpha = tarie), 1.2f)
}

private fun DrawScope.chenarDublu(w: Float, h: Float, cotor: Float, aur: Color) {
    val m = w * 0.085f
    presat(dreptunghi(cotor + m, m, w - m, h - m), aur, 1.5f)
    val m2 = m + w * 0.048f
    presat(dreptunghi(cotor + m2, m2, w - m2, h - m2), aur.copy(alpha = 0.6f), 0.9f)
}

private fun DrawScope.chenarInflorat(w: Float, h: Float, cotor: Float, aur: Color) {
    val m = w * 0.085f
    presat(dreptunghi(cotor + m, m, w - m, h - m), aur, 1.6f)
    val m2 = m + w * 0.052f
    presat(dreptunghi(cotor + m2, m2, w - m2, h - m2), aur.copy(alpha = 0.55f), 0.9f)

    val r = w * 0.078f
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
            val unghi = baza + (i - 1) * 0.58f
            p.moveTo(cx, cy)
            p.quadraticBezierTo(
                cx + cos(unghi - 0.42f) * r * 0.7f,
                cy + sin(unghi - 0.42f) * r * 0.7f,
                cx + cos(unghi) * r,
                cy + sin(unghi) * r
            )
            p.quadraticBezierTo(
                cx + cos(unghi + 0.42f) * r * 0.7f,
                cy + sin(unghi + 0.42f) * r * 0.7f,
                cx, cy
            )
        }
        presat(p, aur.copy(alpha = 0.8f), 1.1f)
    }
}

private fun DrawScope.colturiFine(w: Float, h: Float, cotor: Float, aur: Color) {
    val m = w * 0.09f
    val l = w * 0.17f
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
        presat(p, aur.copy(alpha = 0.85f), 1.4f)
    }
}

private fun DrawScope.liniiDrepte(w: Float, h: Float, cotor: Float, aur: Color) {
    val m = w * 0.11f
    for (y in listOf(h * 0.12f, h * 0.88f)) {
        val p = Path()
        p.moveTo(cotor + m, y)
        p.lineTo(w - m, y)
        presat(p, aur.copy(alpha = 0.75f), 1.4f)
    }
}

private fun DrawScope.vesel(w: Float, h: Float, cotor: Float, aur: Color) {
    val m = w * 0.09f
    val x0 = cotor + m
    val x1 = w - m
    val y0 = m
    val y1 = h - m
    val r = w * 0.13f
    val p = Path()
    p.moveTo(x0 + r, y0)
    p.lineTo(x1 - r, y0)
    p.quadraticBezierTo(x1, y0, x1, y0 + r)
    p.lineTo(x1, y1 - r)
    p.quadraticBezierTo(x1, y1, x1 - r, y1)
    p.lineTo(x0 + r, y1)
    p.quadraticBezierTo(x0, y1, x0, y1 - r)
    p.lineTo(x0, y0 + r)
    p.quadraticBezierTo(x0, y0, x0 + r, y0)
    presat(p, aur, 1.7f)
}

private fun DrawScope.rustic(w: Float, h: Float, cotor: Float, aur: Color) {
    val m = w * 0.10f
    presat(dreptunghi(cotor + m, m, w - m, h - m), aur.copy(alpha = 0.65f), 2.4f)
}
