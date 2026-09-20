package ro.pergament.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipInputStream

object Import {

    fun numeFisier(ctx: Context, uri: Uri): String {
        var nume: String? = null
        try {
            ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) nume = c.getString(idx)
            }
        } catch (e: Exception) {
        }
        return nume ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Carte"
    }

    private fun curataTitlu(brut: String): Pair<String, String> {
        var s = brut.substringBeforeLast('.')
        s = s.replace('_', ' ').replace(Regex("\\s+"), " ").trim()
        val p = s.split(" - ", " – ", limit = 2)
        var autor = ""
        var titlu = s
        if (p.size == 2 && p[0].length in 3..40 && p[0].split(" ").size <= 4) {
            autor = p[0].trim()
            titlu = p[1].trim()
        }
        titlu = titlu.split(" ").joinToString(" ") { w ->
            if (w.length > 2 && w == w.uppercase(Locale.ROOT))
                w.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase() }
            else w
        }
        if (titlu.isBlank()) titlu = "Carte fără titlu"
        return titlu to autor
    }

    fun adauga(ctx: Context, uri: Uri): Carte? {
        try {
            ctx.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
        }

        val nume = numeFisier(ctx, uri)
        val ext = nume.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val format = when (ext) {
            "pdf" -> "PDF"
            "epub" -> "EPUB"
            "txt", "md" -> "TXT"
            "cbz", "zip" -> "CBZ"
            else -> if (ext.isBlank()) "TXT" else ext.uppercase(Locale.ROOT)
        }

        var titlu = curataTitlu(nume).first
        var autor = curataTitlu(nume).second
        val id = "b" + System.currentTimeMillis() + "_" +
                uri.toString().hashCode().toString().replace("-", "n")

        var pagini = 0
        if (format == "PDF") pagini = numaraPaginiPdf(ctx, uri)

        if (format == "EPUB") {
            val meta = metadateEpub(ctx, uri)
            if (meta.first.isNotBlank()) titlu = meta.first
            if (meta.second.isNotBlank()) autor = meta.second
        }

        val coperta = extrageCoperta(ctx, uri, format, id)

        return Carte(
            id = id,
            uri = uri.toString(),
            titlu = titlu,
            autor = autor,
            format = format,
            raft = Rafturi.detecteaza(titlu, autor, format),
            adaugat = System.currentTimeMillis(),
            coperta = coperta,
            totalPagini = pagini
        )
    }

    private fun numaraPaginiPdf(ctx: Context, uri: Uri): Int {
        return try {
            ctx.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { r -> r.pageCount }
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private fun metadateEpub(ctx: Context, uri: Uri): Pair<String, String> {
        try {
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zis ->
                    var e = zis.nextEntry
                    while (e != null) {
                        val n = e.name.lowercase(Locale.ROOT)
                        if (n.endsWith(".opf")) {
                            val text = zis.readBytes().toString(Charsets.UTF_8)
                            val t = Regex("<dc:title[^>]*>(.*?)</dc:title>", RegexOption.DOT_MATCHES_ALL)
                                .find(text)?.groupValues?.get(1)?.trim() ?: ""
                            val a = Regex("<dc:creator[^>]*>(.*?)</dc:creator>", RegexOption.DOT_MATCHES_ALL)
                                .find(text)?.groupValues?.get(1)?.trim() ?: ""
                            return t.take(120) to a.take(60)
                        }
                        zis.closeEntry()
                        e = zis.nextEntry
                    }
                }
            }
        } catch (ex: Exception) {
        }
        return "" to ""
    }

    private fun folderCoperti(ctx: Context): File {
        val d = File(ctx.filesDir, "coperti")
        if (!d.exists()) d.mkdirs()
        return d
    }

    fun extrageCoperta(ctx: Context, uri: Uri, format: String, id: String): String? {
        val bmp = when (format) {
            "PDF" -> copertaPdf(ctx, uri)
            "EPUB", "CBZ" -> copertaZip(ctx, uri, format == "EPUB")
            else -> null
        } ?: return null
        return try {
            val f = File(folderCoperti(ctx), "$id.png")
            FileOutputStream(f).use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 92, out) }
            bmp.recycle()
            f.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun copertaPdf(ctx: Context, uri: Uri): Bitmap? {
        return try {
            ctx.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { r ->
                    if (r.pageCount == 0) return null
                    val p = r.openPage(0)
                    val lat = 700
                    val inalt = (lat.toFloat() * p.height / p.width).toInt().coerceIn(200, 1400)
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

    private fun copertaZip(ctx: Context, uri: Uri, epub: Boolean): Bitmap? {
        var celMaiBun: ByteArray? = null
        var numeBun: String? = null
        try {
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zis ->
                    var e = zis.nextEntry
                    while (e != null) {
                        val n = e.name.lowercase(Locale.ROOT)
                        val esteImagine = n.endsWith(".jpg") || n.endsWith(".jpeg") ||
                                n.endsWith(".png") || n.endsWith(".webp")
                        if (esteImagine && !e.isDirectory) {
                            val potrivireCoperta = epub && n.contains("cover")
                            val curent = numeBun
                            val maiBun = when {
                                celMaiBun == null -> true
                                potrivireCoperta && curent?.contains("cover") != true -> true
                                !epub && curent != null && n < curent -> true
                                else -> false
                            }
                            if (maiBun) {
                                val date = zis.readBytes()
                                if (date.size > 1000) {
                                    celMaiBun = date
                                    numeBun = n
                                }
                            }
                            if (potrivireCoperta && celMaiBun != null) break
                        }
                        zis.closeEntry()
                        e = zis.nextEntry
                    }
                }
            }
        } catch (ex: Exception) {
        }
        val date = celMaiBun ?: return null
        return try {
            val opt = BitmapFactory.Options()
            opt.inSampleSize = 2
            BitmapFactory.decodeByteArray(date, 0, date.size, opt)
        } catch (e: Exception) {
            null
        }
    }
}
