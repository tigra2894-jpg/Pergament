package ro.pergament.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import ro.pergament.data.Carte
import java.io.File
import java.util.Locale
import java.util.zip.ZipInputStream

sealed class Continut {
    class Litera(val pagini: List<String>) : Continut()
    class Pdf(val pfd: ParcelFileDescriptor, val renderer: PdfRenderer, val nrPagini: Int) : Continut()
    class Imagini(val cai: List<String>) : Continut()
    class Eroare(val mesaj: String) : Continut()

    val nrPaginiTotal: Int
        get() = when (this) {
            is Litera -> pagini.size
            is Pdf -> nrPagini
            is Imagini -> cai.size
            is Eroare -> 1
        }

    fun inchide() {
        if (this is Pdf) {
            try {
                renderer.close()
            } catch (e: Exception) {
            }
            try {
                pfd.close()
            } catch (e: Exception) {
            }
        }
    }
}

object Cititor {

    fun incarca(ctx: Context, carte: Carte, caracterePePagina: Int): Continut {
        val uri = try {
            Uri.parse(carte.uri)
        } catch (e: Exception) {
            return Continut.Eroare("Fișierul nu mai poate fi găsit.")
        }
        return try {
            when (carte.format) {
                "PDF" -> incarcaPdf(ctx, uri)
                "EPUB" -> Continut.Litera(paginare(textEpub(ctx, uri), caracterePePagina))
                "CBZ" -> incarcaCbz(ctx, uri, carte.id)
                else -> Continut.Litera(paginare(textSimplu(ctx, uri), caracterePePagina))
            }
        } catch (e: SecurityException) {
            Continut.Eroare("Nu mai am acces la fișier. Adaugă cartea din nou.")
        } catch (e: Exception) {
            Continut.Eroare("Fișierul nu a putut fi deschis.")
        }
    }

    private fun incarcaPdf(ctx: Context, uri: Uri): Continut {
        val pfd = ctx.contentResolver.openFileDescriptor(uri, "r")
            ?: return Continut.Eroare("Fișierul PDF nu a putut fi deschis.")
        val r = PdfRenderer(pfd)
        return Continut.Pdf(pfd, r, r.pageCount)
    }

    fun randeazaPdf(continut: Continut.Pdf, index: Int, latimePx: Int): Bitmap? {
        return try {
            synchronized(continut.renderer) {
                if (index < 0 || index >= continut.nrPagini) return null
                val p = continut.renderer.openPage(index)
                val lat = latimePx.coerceIn(400, 2000)
                val inalt = (lat.toFloat() * p.height / p.width).toInt().coerceAtLeast(200)
                val bmp = Bitmap.createBitmap(lat, inalt, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(Color.WHITE)
                p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                p.close()
                bmp
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun textSimplu(ctx: Context, uri: Uri): String {
        ctx.contentResolver.openInputStream(uri)?.use { input ->
            return input.readBytes().toString(Charsets.UTF_8)
        }
        return ""
    }

    private fun textEpub(ctx: Context, uri: Uri): String {
        val fisiere = LinkedHashMap<String, String>()
        var opf = ""
        ctx.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zis ->
                var e = zis.nextEntry
                while (e != null) {
                    val n = e.name
                    val nl = n.lowercase(Locale.ROOT)
                    if (!e.isDirectory) {
                        if (nl.endsWith(".opf")) {
                            opf = zis.readBytes().toString(Charsets.UTF_8)
                        } else if (nl.endsWith(".xhtml") || nl.endsWith(".html") || nl.endsWith(".htm")) {
                            fisiere[n] = zis.readBytes().toString(Charsets.UTF_8)
                        }
                    }
                    zis.closeEntry()
                    e = zis.nextEntry
                }
            }
        }
        if (fisiere.isEmpty()) return ""

        val sb = StringBuilder()
        for (html in ordoneazaDupaOpf(opf, fisiere)) {
            val t = htmlInText(html)
            if (t.isNotBlank()) {
                sb.append(t.trim())
                sb.append("\n\n")
            }
        }
        return sb.toString()
    }

    private fun ordoneazaDupaOpf(opf: String, fisiere: Map<String, String>): List<String> {
        if (opf.isBlank()) return fisiere.toSortedMap().values.toList()
        return try {
            val manifest = HashMap<String, String>()
            Regex("<item\\b[^>]*>").findAll(opf).forEach { m ->
                val tag = m.value
                val id = Regex("id=\"([^\"]+)\"").find(tag)?.groupValues?.get(1)
                val href = Regex("href=\"([^\"]+)\"").find(tag)?.groupValues?.get(1)
                if (id != null && href != null) manifest[id] = href
            }
            val spine = Regex("<itemref\\b[^>]*idref=\"([^\"]+)\"")
                .findAll(opf).map { it.groupValues[1] }.toList()
            if (spine.isEmpty() || manifest.isEmpty()) return fisiere.toSortedMap().values.toList()

            val rezultat = mutableListOf<String>()
            for (id in spine) {
                val href = manifest[id] ?: continue
                val capat = href.substringAfterLast('/')
                val cheie = fisiere.keys.firstOrNull { it.endsWith(href) || it.endsWith(capat) }
                if (cheie != null) fisiere[cheie]?.let { rezultat.add(it) }
            }
            if (rezultat.isEmpty()) fisiere.toSortedMap().values.toList() else rezultat
        } catch (e: Exception) {
            fisiere.toSortedMap().values.toList()
        }
    }

    private fun htmlInText(html: String): String {
        var s = html
        s = s.replace(Regex("(?is)<(script|style|head)[^>]*>.*?</\\1>"), " ")
        s = s.replace(Regex("(?i)<br\\s*/?>"), "\n")
        s = s.replace(Regex("(?i)</(p|div|h1|h2|h3|h4|li|tr)>"), "\n\n")
        s = s.replace(Regex("<[^>]+>"), "")
        s = s.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<")
            .replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
            .replace("&rsquo;", "’").replace("&ldquo;", "„").replace("&rdquo;", "”")
            .replace("&mdash;", "—").replace("&ndash;", "–").replace("&hellip;", "…")
        s = s.replace(Regex("[ \\t\\x0B\\f\\r]+"), " ")
        s = s.replace(Regex("\\n{3,}"), "\n\n")
        return s.trim()
    }

    fun paginare(text: String, caractere: Int): List<String> {
        if (text.isBlank()) return listOf("Această carte nu conține text care poate fi citit.")
        val limita = caractere.coerceIn(400, 5000)
        val pagini = mutableListOf<String>()
        var i = 0
        while (i < text.length) {
            var sfarsit = (i + limita).coerceAtMost(text.length)
            if (sfarsit < text.length) {
                val fereastra = text.substring(i, sfarsit)
                val taiereParagraf = fereastra.lastIndexOf("\n\n")
                val taiereFraza = maxOf(
                    fereastra.lastIndexOf(". "),
                    maxOf(fereastra.lastIndexOf("! "), fereastra.lastIndexOf("? "))
                )
                val taiereSpatiu = fereastra.lastIndexOf(' ')
                val taiere = when {
                    taiereParagraf > limita * 0.55 -> taiereParagraf + 2
                    taiereFraza > limita * 0.5 -> taiereFraza + 2
                    taiereSpatiu > limita * 0.5 -> taiereSpatiu + 1
                    else -> fereastra.length
                }
                sfarsit = i + taiere
            }
            val bucata = text.substring(i, sfarsit).trim()
            if (bucata.isNotEmpty()) pagini.add(bucata)
            i = sfarsit
        }
        return if (pagini.isEmpty()) listOf(text) else pagini
    }

    private fun incarcaCbz(ctx: Context, uri: Uri, id: String): Continut {
        val dir = File(ctx.cacheDir, "cbz/$id")
        if (!dir.exists()) dir.mkdirs()
        val existente = dir.listFiles()?.sortedBy { it.name }?.map { it.absolutePath } ?: emptyList()
        if (existente.isNotEmpty()) return Continut.Imagini(existente)

        var nr = 0
        ctx.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zis ->
                var e = zis.nextEntry
                while (e != null) {
                    val n = e.name.lowercase(Locale.ROOT)
                    val img = n.endsWith(".jpg") || n.endsWith(".jpeg") ||
                            n.endsWith(".png") || n.endsWith(".webp")
                    if (img && !e.isDirectory) {
                        val f = File(dir, String.format(Locale.ROOT, "%04d", nr) + ".img")
                        f.writeBytes(zis.readBytes())
                        nr++
                    }
                    zis.closeEntry()
                    e = zis.nextEntry
                }
            }
        }
        val cai = dir.listFiles()?.sortedBy { it.name }?.map { it.absolutePath } ?: emptyList()
        return if (cai.isEmpty()) Continut.Eroare("Nu am găsit imagini în fișier.")
        else Continut.Imagini(cai)
    }
}
