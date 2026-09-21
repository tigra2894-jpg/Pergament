package ro.pergament.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Tine minte pe ce pagina esti si unde e degetul care trage de colt.
 * directie: 0 = pagina sta, 1 = se intoarce inainte, -1 = se intoarce inapoi
 */
class StareRasfoire(paginaInitiala: Int, val total: Int) {
    var pagina by mutableIntStateOf(paginaInitiala)
    var directie by mutableIntStateOf(0)
    var deget by mutableStateOf(Offset.Zero)
    var colt by mutableStateOf(Offset.Zero)
    var latime by mutableFloatStateOf(1f)
    var inaltime by mutableFloatStateOf(1f)
    private var ocupat = false

    // pagina e prinsa in cotor: coltul nu se poate departa de cotor mai mult decat latimea paginii
    private fun limiteaza(p: Offset): Offset {
        val cotor = Offset(0f, colt.y)
        val v = p - cotor
        val dist = v.getDistance()
        val max = latime * 0.999f
        return if (dist > max && dist > 0f) cotor + v * (max / dist) else p
    }

    fun incepeTragere(start: Offset): Boolean {
        if (ocupat) return false
        val W = latime
        val H = inaltime
        val y = if (start.y < H * 0.5f) 0f else H
        if (start.x > W * 0.5f) {
            if (pagina >= total - 1) return false
            colt = Offset(W, y)
            deget = limiteaza(start)
            directie = 1
        } else {
            if (pagina <= 0) return false
            colt = Offset(W, y)
            deget = limiteaza(start)
            directie = -1
        }
        ocupat = true
        return true
    }

    fun muta(p: Offset) {
        deget = limiteaza(p)
    }

    suspend fun elibereaza() {
        val W = latime
        val finalizeaza: Boolean
        val tinta: Offset
        if (directie == 1) {
            finalizeaza = deget.x < W * 0.55f
            tinta = if (finalizeaza) Offset(-W, colt.y) else colt
        } else {
            finalizeaza = deget.x > W * 0.45f
            tinta = if (finalizeaza) colt else Offset(-W, colt.y)
        }
        val de = deget
        animate(0f, 1f, animationSpec = tween(380, easing = FastOutSlowInEasing)) { v, _ ->
            deget = limiteaza(Offset(de.x + (tinta.x - de.x) * v, de.y + (tinta.y - de.y) * v))
        }
        if (finalizeaza) pagina += directie
        directie = 0
        ocupat = false
    }

    suspend fun inainte() {
        if (ocupat || pagina >= total - 1) return
        ocupat = true
        val W = latime
        val H = inaltime
        colt = Offset(W, H)
        deget = colt
        directie = 1
        animate(0f, 1f, animationSpec = tween(640, easing = FastOutSlowInEasing)) { v, _ ->
            deget = limiteaza(Offset(W - 2f * W * v, H - H * 0.22f * sin(PI.toFloat() * v)))
        }
        pagina += 1
        directie = 0
        ocupat = false
    }

    suspend fun inapoi() {
        if (ocupat || pagina <= 0) return
        ocupat = true
        val W = latime
        val H = inaltime
        colt = Offset(W, H)
        deget = limiteaza(Offset(-W, H))
        directie = -1
        animate(0f, 1f, animationSpec = tween(640, easing = FastOutSlowInEasing)) { v, _ ->
            deget = limiteaza(Offset(-W + 2f * W * v, H - H * 0.22f * sin(PI.toFloat() * v)))
        }
        pagina -= 1
        directie = 0
        ocupat = false
    }

    fun sari(p: Int) {
        if (!ocupat) {
            directie = 0
            pagina = p.coerceIn(0, (total - 1).coerceAtLeast(0))
        }
    }
}

// ---------- geometria indoiturii ----------

private class Geometrie(
    val vizibil: Path,
    val descoperit: Path,
    val clapa: Path,
    val m: Offset,
    val n: Offset,
    val varf: Offset,
    val valid: Boolean
)

private fun latura(p: Offset, m: Offset, n: Offset): Float =
    (p.x - m.x) * n.x + (p.y - m.y) * n.y

private fun taie(poli: List<Offset>, m: Offset, n: Offset, pozitiv: Boolean): List<Offset> {
    val semn = if (pozitiv) 1f else -1f
    val out = mutableListOf<Offset>()
    for (i in poli.indices) {
        val a = poli[i]
        val b = poli[(i + 1) % poli.size]
        val fa = latura(a, m, n) * semn
        val fb = latura(b, m, n) * semn
        if (fa >= 0f) out.add(a)
        if ((fa >= 0f) != (fb >= 0f)) {
            val t = fa / (fa - fb)
            out.add(Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t))
        }
    }
    return out
}

private fun reflecta(p: Offset, m: Offset, n: Offset): Offset {
    val k = 2f * latura(p, m, n)
    return Offset(p.x - k * n.x, p.y - k * n.y)
}

private fun caleDin(pts: List<Offset>): Path {
    val p = Path()
    if (pts.isEmpty()) return p
    p.moveTo(pts[0].x, pts[0].y)
    for (i in 1 until pts.size) p.lineTo(pts[i].x, pts[i].y)
    p.close()
    return p
}

private fun calculeaza(W: Float, H: Float, colt: Offset, deget: Offset): Geometrie {
    val rect = listOf(Offset(0f, 0f), Offset(W, 0f), Offset(W, H), Offset(0f, H))
    val d = deget - colt
    val len = d.getDistance()
    if (len < 1f) {
        return Geometrie(caleDin(rect), Path(), Path(), colt, Offset(1f, 0f), colt, false)
    }
    val n = Offset(d.x / len, d.y / len)
    val m = Offset((colt.x + deget.x) / 2f, (colt.y + deget.y) / 2f)
    val viz = taie(rect, m, n, true)
    val desc = taie(rect, m, n, false)
    val clapa = desc.map { reflecta(it, m, n) }
    return Geometrie(caleDin(viz), caleDin(desc), caleDin(clapa), m, n, deget, true)
}

private fun Color.inchide(f: Float) = Color(red * f, green * f, blue * f, alpha)

/**
 * Cartea: afiseaza pagina curenta si, cand tragi de colt, o indoaie
 * aratand spatele paginii, umbra si pagina de dedesubt.
 */
@Composable
fun CarteRasfoita(
    stare: StareRasfoire,
    culoareSpate: Color,
    onAtingere: (Int) -> Unit,
    onDouaDegete: () -> Unit,
    modifier: Modifier = Modifier,
    continut: @Composable (Int) -> Unit
) {
    val scop = rememberCoroutineScope()
    val atingere by rememberUpdatedState(onAtingere)
    val douaDegete by rememberUpdatedState(onDouaDegete)

    Box(
        modifier
            .fillMaxSize()
            .onSizeChanged {
                stare.latime = it.width.toFloat().coerceAtLeast(1f)
                stare.inaltime = it.height.toFloat().coerceAtLeast(1f)
            }
            .pointerInput(stare) {
                awaitEachGesture {
                    val primul = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val start = primul.position
                    val t0 = System.currentTimeMillis()
                    val slop = viewConfiguration.touchSlop
                    var tras = false
                    var doua = false
                    var ultim = start

                    while (true) {
                        val ev = awaitPointerEvent(PointerEventPass.Initial)
                        if (ev.changes.count { it.pressed } >= 2) doua = true
                        val ch = ev.changes.firstOrNull { it.id == primul.id } ?: break
                        ultim = ch.position
                        if (!ch.pressed) break
                        if (doua) continue
                        val dx = ch.position.x - start.x
                        val dy = ch.position.y - start.y
                        if (!tras && abs(dx) > slop && abs(dx) > abs(dy)) {
                            tras = stare.incepeTragere(start)
                        }
                        if (tras) {
                            stare.muta(ch.position)
                            ch.consume()
                        }
                    }

                    if (doua) {
                        if (tras) scop.launch { stare.elibereaza() }
                        douaDegete()
                    } else if (tras) {
                        scop.launch { stare.elibereaza() }
                    } else {
                        val durata = System.currentTimeMillis() - t0
                        if (durata < 400 && (ultim - start).getDistance() < slop) {
                            val W = size.width.toFloat()
                            val zona = when {
                                start.x < W * 0.3f -> -1
                                start.x > W * 0.7f -> 1
                                else -> 0
                            }
                            atingere(zona)
                        }
                    }
                }
            }
    ) {
        val dir = stare.directie
        val p = stare.pagina
        val deasupra = if (dir == -1) p - 1 else p
        val ordine = when (dir) {
            1 -> listOf(p - 1, p + 1, p)
            -1 -> listOf(p + 1, p, p - 1)
            else -> listOf(p + 1, p - 1, p)
        }

        for (idx in ordine) {
            if (idx < 0 || idx >= stare.total) continue
            key(idx) {
                val mod = if (dir != 0 && idx == deasupra) {
                    Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            val g = calculeaza(size.width, size.height, stare.colt, stare.deget)
                            clipPath(g.vizibil) { this@drawWithContent.drawContent() }
                        }
                } else {
                    Modifier.fillMaxSize()
                }
                Box(mod) { continut(idx) }
            }
        }

        if (dir != 0) {
            Canvas(Modifier.fillMaxSize()) {
                val g = calculeaza(size.width, size.height, stare.colt, stare.deget)
                if (!g.valid) return@Canvas

                // umbra pe pagina de dedesubt, langa indoitura
                val lung = (stare.deget - stare.colt).getDistance()
                val umbraL = (lung * 0.22f).coerceIn(12f, 220f)
                clipPath(g.descoperit) {
                    drawRect(
                        Brush.linearGradient(
                            0f to Color.Black.copy(alpha = 0.40f),
                            1f to Color.Transparent,
                            start = g.m,
                            end = Offset(g.m.x - g.n.x * umbraL, g.m.y - g.n.y * umbraL)
                        )
                    )
                }

                // umbra aruncata de pagina ridicata
                drawPath(g.clapa, Color.Black.copy(alpha = 0.10f), style = Stroke(width = 24f))
                drawPath(g.clapa, Color.Black.copy(alpha = 0.14f), style = Stroke(width = 9f))

                // spatele paginii
                drawPath(
                    g.clapa,
                    Brush.linearGradient(
                        0f to culoareSpate.inchide(0.78f),
                        0.16f to culoareSpate,
                        0.72f to culoareSpate.inchide(0.97f),
                        1f to culoareSpate.inchide(0.86f),
                        start = g.m,
                        end = g.varf
                    )
                )

                // lumina pe muchia indoita
                drawPath(g.clapa, Color.White.copy(alpha = 0.20f), style = Stroke(width = 1.5f))
            }
        }
    }
}
