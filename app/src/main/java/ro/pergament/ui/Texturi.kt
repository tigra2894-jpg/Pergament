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
 * Texturile se incarca o singura data si se tin minte.
 * inScaled = false: Android nu le mai umfla dupa densitatea ecranului,
 * altfel fiecare ar ocupa de 7 ori mai multa memorie.
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

/** Face din textura o pensula care se repeta la nesfarsit, la scara ceruta. */
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

@Composable
fun texturaPiele(): Brush? = rememberTextura(R.drawable.textura_piele, 0.8f)

@Composable
fun texturaLemn(): Brush? = rememberTextura(R.drawable.textura_lemn, 0.35f)

@Composable
fun texturaPergament(): Brush? = rememberTextura(R.drawable.textura_pergament, 1f)

@Composable
fun texturaHartie(): Brush? = rememberTextura(R.drawable.textura_hartie, 1f)

@Composable
fun texturaPanza(): Brush? = rememberTextura(R.drawable.textura_panza, 0.6f)

/** Deseneaza textura sub continut. Daca textura lipseste, nu face nimic. */
fun Modifier.textura(brush: Brush?, alpha: Float = 1f): Modifier =
    if (brush == null) this
    else this.drawBehind { drawRect(brush, alpha = alpha) }

/**
 * Pune textura PESTE continut, amestecata prin inmultire:
 * culorile raman, dar capata firul si petele texturii.
 */
fun Modifier.texturaPeste(brush: Brush?, alpha: Float): Modifier =
    if (brush == null) this
    else this.drawWithContent {
        drawContent()
        drawRect(brush, alpha = alpha, blendMode = BlendMode.Multiply)
    }

/**
 * Scandura de lemn de sub fiecare raft:
 * fibra lemnului, muchie luminata sus, muchie in umbra jos,
 * si umbra pe care o arunca raftul dedesubt.
 */
@Composable
fun ScanduraRaft(modifier: Modifier = Modifier) {
    val lemn = texturaLemn()
    Canvas(
        modifier
            .fillMaxWidth()
            .height(24.dp)
    ) {
        val w = size.width
        val h = size.height
        val grosime = h * 0.58f

        if (lemn != null) {
            drawRect(lemn, size = Size(w, grosime))
        } else {
            drawRect(Color(0xFF5A3A20), size = Size(w, grosime))
        }

        // lumina calda pe muchia de sus
        drawRect(Color(0xFFF2CB84).copy(alpha = 0.42f), size = Size(w, 2.2f))
        drawRect(
            Brush.verticalGradient(
                0f to Color(0xFFF2CB84).copy(alpha = 0.18f),
                1f to Color.Transparent,
                startY = 0f,
                endY = grosime * 0.45f
            ),
            size = Size(w, grosime * 0.45f)
        )

        // partea din fata a scandurii, mai intunecata spre jos
        drawRect(
            Brush.verticalGradient(
                0f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.50f),
                startY = grosime * 0.45f,
                endY = grosime
            ),
            topLeft = Offset(0f, grosime * 0.45f),
            size = Size(w, grosime * 0.55f)
        )

        // capetele raftului se pierd in intuneric
        drawRect(
            Brush.horizontalGradient(
                0f to Color.Black.copy(alpha = 0.55f),
                0.12f to Color.Transparent,
                0.88f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.55f)
            ),
            size = Size(w, grosime)
        )

        // umbra aruncata sub raft
        drawRect(
            Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.60f),
                1f to Color.Transparent,
                startY = grosime,
                endY = h
            ),
            topLeft = Offset(0f, grosime),
            size = Size(w, h - grosime)
        )
    }
}
