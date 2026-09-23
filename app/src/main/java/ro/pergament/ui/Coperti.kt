package ro.pergament.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object Coperti {

    private fun folder(ctx: Context): File {
        val d = File(ctx.filesDir, "coperti")
        if (!d.exists()) d.mkdirs()
        return d
    }

    private fun scrie(ctx: Context, bmp: Bitmap, id: String): String? {
        return try {
            val f = File(folder(ctx), "$id.png")
            FileOutputStream(f).use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 92, out) }
            bmp.recycle()
            f.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /** Sterge coperta salvata: cartea trece pe legatura de piele. */
    fun sterge(ctx: Context, id: String) {
        try {
            File(folder(ctx), "$id.png").delete()
        } catch (e: Exception) {
        }
    }

    /** Cate pagini are cartea, ca sa stim din cate poate alege utilizatorul. */
    fun nrPaginiPdf(ctx: Context, uriText: String): Int {
        return try {
            val uri = Uri.parse(uriText)
            ctx.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { r -> r.pageCount }
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    /** Randeaza o pagina anume din PDF ca imagine mica, pentru alegere. */
    fun previzualizarePdf(ctx: Context, uriText: String, pagina: Int, latime: Int): Bitmap? {
        return try {
            val uri = Uri.parse(uriText)
            ctx.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { r ->
                    if (pagina < 0 || pagina >= r.pageCount) return null
                    val p = r.openPage(pagina)
                    val lat = latime.coerceIn(120, 1200)
                    val inalt = (lat.toFloat() * p.height / p.width).toInt().coerceIn(60, 2000)
                    val bmp = Bitmap.createBitmap(lat, inalt, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(Color.WHITE)
                    p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    p.close()
                    bmp
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Pune o pagina din PDF drept coperta. */
    fun dinPagina(ctx: Context, uriText: String, pagina: Int, id: String): String? {
        val bmp = previzualizarePdf(ctx, uriText, pagina, 700) ?: return null
        return scrie(ctx, bmp, id)
    }

    /** Pune o poza aleasa din galerie drept coperta. */
    fun dinImagine(ctx: Context, sursa: Uri, id: String): String? {
        return try {
            val brut = ctx.contentResolver.openInputStream(sursa)?.use { it.readBytes() }
                ?: return null

            val masura = BitmapFactory.Options()
            masura.inJustDecodeBounds = true
            BitmapFactory.decodeByteArray(brut, 0, brut.size, masura)

            val opt = BitmapFactory.Options()
            var pas = 1
            while (masura.outWidth / pas > 1400) pas *= 2
            opt.inSampleSize = pas

            val bmp = BitmapFactory.decodeByteArray(brut, 0, brut.size, opt) ?: return null
            scrie(ctx, bmp, id)
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    /**
     * Cat de "plina" e o pagina: cat la suta din ea nu e alb curat.
     * O pagina de titlu alba are sub 4%, o coperta adevarata are mult mai mult.
     */
    fun incarcatura(bmp: Bitmap): Float {
        return try {
            val l = 60
            val i = (l * bmp.height / bmp.width.coerceAtLeast(1)).coerceIn(20, 120)
            val mic = Bitmap.createScaledBitmap(bmp, l, i, true)
            val px = IntArray(l * i)
            mic.getPixels(px, 0, l, 0, 0, l, i)
            mic.recycle()
            var n = 0
            for (c in px) {
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                if ((r + g + b) / 3 < 242) n++
            }
            n.toFloat() / px.size
        } catch (e: Exception) {
            0f
        }
    }
}
