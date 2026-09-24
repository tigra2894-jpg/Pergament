package ro.pergament.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ro.pergament.R

/**
 * Texturile se citesc o singura data de pe disc si se tin minte.
 * inScaled = false: Android nu le mai umfla dupa densitatea ecranului,
 * altfel fiecare ar ocupa de cateva ori mai multa memorie.
 */
object Texturi {
    private val cache = HashMap<Int, ImageBitmap>()

    fun imagine(ctx: Context, id: Int): ImageBitmap? = synchronized(cache) {
        cache[id] ?: try {
            val opt = BitmapFactory.Options()
            opt.inScaled = false
            BitmapFactory.decodeResource(ctx.resources, id, opt)
                ?.asImageBitmap()
                ?.also { cache[id] = it }
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }
}

/** Face din textura o pensula care se repeta la nesfarsit. */
@Composable
fun rememberTextura(id: Int, scara: Float = 1f): Brush? {
    val ctx = LocalContext.current
    return remember(id, scara) {
        val img = Texturi.imagine(ctx, id) ?: return@remember null
        val shader = ImageShader(img, TileMode.Repeated, TileMode.Repeated)
        if (scara != 1f) {
            val m = Matrix()
            m.setScale(scara, scara)
            shader.setLocalMatrix(m)
        }
        ShaderBrush(shader)
    }
}

// ---------- materialele aplicatiei ----------

@Composable fun pieleMaro(s: Float = 0.55f) = rememberTextura(R.drawable.piele_maro, s)
@Composable fun pieleVisinie(s: Float = 0.55f) = rememberTextura(R.drawable.piele_visinie, s)
@Composable fun pieleVerde(s: Float = 0.55f) = rememberTextura(R.drawable.piele_verde, s)
@Composable fun pieleAlbastra(s: Float = 0.55f) = rememberTextura(R.drawable.piele_albastra, s)
@Composable fun pieleCaramel(s: Float = 0.55f) = rememberTextura(R.drawable.piele_caramel, s)

@Composable fun lemnStejar(s: Float = 0.42f) = rememberTextura(R.drawable.lemn_stejar, s)
@Composable fun lemnNuc(s: Float = 0.75f) = rememberTextura(R.drawable.lemn_nuc, s)
@Composable fun catifea(s: Float = 0.50f) = rememberTextura(R.drawable.catifea, s)
@Composable fun aurFoita(s: Float = 0.30f) = rememberTextura(R.drawable.aur, s)
@Composable fun hartieVeche(s: Float = 0.80f) = rememberTextura(R.drawable.hartie_veche, s)
@Composable fun hartieCrem(s: Float = 0.80f) = rememberTextura(R.drawable.hartie_crem, s)

/** Pune materialul ca fundal. Daca lipseste, nu face nimic. */
fun Modifier.material(brush: Brush?, alpha: Float = 1f): Modifier =
    if (brush == null) this
    else this.drawBehind { drawRect(brush, alpha = alpha) }

/** Pune materialul PESTE continut, ca sa-i dea fir si relief. */
fun Modifier.materialPeste(brush: Brush?, alpha: Float): Modifier =
    if (brush == null) this
    else this.drawWithContent {
        drawContent()
        drawRect(brush, alpha = alpha, blendMode = BlendMode.Multiply)
    }

// numele vechi, ca sa nu se strice restul codului
fun Modifier.textura(brush: Brush?, alpha: Float = 1f) = material(brush, alpha)
fun Modifier.texturaPeste(brush: Brush?, alpha: Float) = materialPeste(brush, alpha)

@Composable fun texturaPiele() = pieleMaro(0.85f)
@Composable fun texturaLemn() = lemnStejar()
@Composable fun texturaPergament() = hartieVeche()
@Composable fun texturaHartie() = hartieCrem()
@Composable fun texturaPanza() = catifea()

/**
 * Scandura raftului: lemn adevarat, muchie luminata sus,
 * fata mai intunecata jos si umbra aruncata dedesubt.
 */
@Composable
fun ScanduraRaft(modifier: Modifier = Modifier) {
    val lemn = lemnStejar(0.38f)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(26.dp)
    ) {
        val w = size.width
        val h = size.height
        val grosime = h * 0.60f

        if (lemn != null) {
            drawRect(lemn, size = Size(w, grosime))
        } else {
            drawRect(Color(0xFF5A3A20), size = Size(w, grosime))
        }

        // muchia de sus prinde lumina
        drawRect(Color(0xFFFFE2A8).copy(alpha = 0.34f), size = Size(w, 2f))
        drawRect(
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.14f),
                1f to Color.Transparent,
                startY = 0f,
                endY = grosime * 0.40f
            ),
            size = Size(w, grosime * 0.40f)
        )

        // fata scandurii se intuneca spre jos
        drawRect(
            Brush.verticalGradient(
                0f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.52f),
                startY = grosime * 0.40f,
                endY = grosime
            ),
            topLeft = Offset(0f, grosime * 0.40f),
            size = Size(w, grosime * 0.60f)
        )

        // capetele se pierd in intuneric
        drawRect(
            Brush.horizontalGradient(
                0f to Color.Black.copy(alpha = 0.60f),
                0.10f to Color.Transparent,
                0.90f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.60f)
            ),
            size = Size(w, grosime)
        )

        // umbra aruncata sub raft
        drawRect(
            Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.62f),
                1f to Color.Transparent,
                startY = grosime,
                endY = h
            ),
            topLeft = Offset(0f, grosime),
            size = Size(w, h - grosime)
        )
    }
}
