package ro.pergament.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import ro.pergament.data.Carte
import java.io.File
import java.util.Locale
import java.util.zip.ZipInputStream

sealed class Continut {
    class Litera(val pagini: List<String>, val dinPdf: Boolean = false) : Continut()
    class Pdf(
        val pfd: ParcelFileDescriptor,
        val renderer: PdfRenderer,
        val nrPagini: Int,
        val scanat: Boolean = false
    ) : Continut()
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

    fun incarca(
        ctx: Context,
        carte: Carte,
        caracterePePagina: Int,
        pdfCaText: Boolean = false,
        progres: (Float) -> Unit = {}
    ): Continut {
        val uri = try {
            Uri.parse(carte.uri)
        } catch (e: Exception) {
            return Continut.Eroare("Fișierul nu mai poate fi găsit.")
        }
        return try {
            when (carte.format) {
                "PDF" -> {
                    if (pdfCaText) {
                        val text = textPdf(ctx, uri, carte.id, progres)
                        if (esteScanat(text, carte.totalPagini)) {
                            incarcaPdf(ctx, uri, scanat = true)
                        } else {
                            Continut.Litera(paginare(text, caracterePePagina), dinPdf = true)
                        }
                    } else {
                        incarcaPdf(ctx, uri, scanat = false)
                    }
                }
                "EPUB" -> Continut.Litera(paginare(textEpub(ctx, uri), caracterePePagina))
                "CBZ" -> incarcaCbz(ctx, uri, carte.id)
                else -> Continut.Litera(paginare(textSimplu(ctx, uri), caracterePePagina))
            }
        } catch (e: SecurityException) {
            Continut.Eroare("Nu mai am acces la fișier. Adaugă cartea din nou.")
        } catch (e: OutOfMemoryError) {
            Continut.Eroare("Cartea e prea mare pentru modul text. Folosește pagina originală.")
        } catch (e: Exception) {
            Continut.Eroare("Fișierul nu a putut fi deschis.")
        }
    }

    // ---------- PDF ca imagine ----------

    private fun incarcaPdf(ctx: Context, uri: Uri, scanat: Boolean): Continut {
        val pfd = ctx.contentResolver.openFileDescriptor(uri, "r")
            ?: return Continut.Eroare("Fișierul PDF nu a putut fi deschis.")
        val r = PdfRenderer(pfd)
        return Continut.Pdf(pfd, r, r.pageCount, scanat)
    }

    fun randeazaPdf(
        continut: Continut.Pdf,
        index: Int,
        latimePx: Int,
        taieMargini: Boolean
    ): Bitmap? {
        return try {
            synchronized(continut.renderer) {
                if (index < 0 || index >= continut.nrPagini) return null
                val p = continut.renderer.openPage(index)
                val lat = latimePx.coerceIn(600, 4200)
                val inalt = (lat.toFloat() * p.height / p.width).toInt().coerceIn(300, 8000)
                val bmp = Bitmap.createBitmap(lat, inalt, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(Color.WHITE)
                p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                p.close()
                if (!taieMargini) return bmp
                val taiat = taieMarginiAlbe(bmp)
                if (taiat !== bmp) bmp.recycle()
                taiat
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun taieMarginiAlbe(bmp: Bitmap): Bitmap {
        try {
            val L = bmp.width
            val I = bmp.height
            if (L < 200 || I < 200) return bmp
            val pas = 4
            val l = L / pas
            val i = I / pas
            val px = IntArray(l * i)
            val mic = Bitmap.createScaledBitmap(bmp, l, i, true)
            mic.getPixels(px, 0, l, 0, 0, l, i)
            mic.recycle()
            val prag = 232

            fun eContinut(k: Int): Boolean {
                val c = px[k]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                return (r + g + b) / 3 < prag
            }

            var sus = -1
            var jos = -1
            var stanga = -1
            var dreapta = -1
            for (y in 0 until i) {
                var n = 0
                for (x in 0 until l) if (eContinut(y * l + x)) n++
                if (n > l * 0.004) { sus = y; break }
            }
            for (y in i - 1 downTo 0) {
                var n = 0
                for (x in 0 until l) if (eContinut(y * l + x)) n++
                if (n > l * 0.004) { jos = y; break }
            }
            for (x in 0 until l) {
                var n = 0
                for (y in 0 until i) if (eContinut(y * l + x)) n++
                if (n > i * 0.004) { stanga = x; break }
            }
            for (x in l - 1 downTo 0) {
                var n = 0
                for (y in 0 until i) if (eContinut(y * l + x)) n++
                if (n > i * 0.004) { dreapta = x; break }
            }
            if (sus < 0 || jos <= sus || stanga < 0 || dreapta <= stanga) return bmp

            val rama = (l * 0.012).toInt().coerceAtLeast(2)
            val x0 = ((stanga - rama).coerceAtLeast(0)) * pas
            val x1 = ((dreapta + rama).coerceAtMost(l - 1)) * pas
            val y0 = ((sus - rama).coerceAtLeast(0)) * pas
            val y1 = ((jos + rama).coerceAtMost(i - 1)) * pas
            val lw = (x1 - x0).coerceAtMost(L - x0)
            val lh = (y1 - y0).coerceAtMost(I - y0)
            if (lw > L * 0.97 && lh > I * 0.97) return bmp
            if (lw < L * 0.25 || lh < I * 0.25) return bmp
            return Bitmap.createBitmap(bmp, x0, y0, lw, lh)
        } catch (e: Exception) {
            return bmp
        }
    }

    // ---------- PDF ca text ----------

    private fun textPdf(ctx: Context, uri: Uri, id: String, progres: (Float) -> Unit): String {
        val dir = File(ctx.filesDir, "texte")
        if (!dir.exists()) dir.mkdirs()
        val f = File(dir, "$id.txt")
        if (f.exists()) {
            progres(1f)
            return f.readText()
        }

        PDFBoxResourceLoader.init(ctx.applicationContext)
        val brut = StringBuilder()
        ctx.contentResolver.openInputStream(uri)?.use { input ->
            PDDocument.load(input, MemoryUsageSetting.setupMixed(48L * 1024 * 1024)).use { doc ->
                val n = doc.numberOfPages
                val s = PDFTextStripper()
                s.sortByPosition = false
                var p = 1
                while (p <= n) {
                    val q = minOf(n, p + 19)
                    s.startPage = p
                    s.endPage = q
                    brut.append(s.getText(doc))
                    brut.append("\n")
                    progres(q.toFloat() / n.coerceAtLeast(1))
                    p = q + 1
                }
            }
        }

        val text = reasaza(brut.toString())
        try {
            f.writeText(text)
        } catch (e: Exception) {
        }
        return text
    }

    private fun esteScanat(text: String, pagini: Int): Boolean {
        val litere = text.count { it.isLetter() }
        val p = pagini.coerceAtLeast(1)
        return litere < 200 || litere / p < 60
    }

    /**
     * Textul scos din PDF are cate o rupere de rand la fiecare rand tiparit.
     * Aici lipim randurile inapoi in paragrafe, ca textul sa se aseze
     * singur pe latimea telefonului.
     */
    private fun reasaza(brut: String): String {
        val linii = brut.replace("\r", "").split("\n").map { it.trim() }
        val lungimi = linii.filter { it.length > 10 }.map { it.length }.sorted()
        val mediana = if (lungimi.isEmpty()) 60 else lungimi[lungimi.size / 2]
        val sfarsitFraza = ".!?:;”\"»)"

        val sb = StringBuilder()
        var lungPrec = 0

        for (l in linii) {
            if (l.isEmpty()) {
                if (sb.isNotEmpty() && !sb.endsWith("\n\n")) sb.append("\n\n")
                lungPrec = 0
                continue
            }
            // numere de pagina singure pe rand
            if (l.length <= 4 && l.all { it.isDigit() }) continue

            if (sb.isEmpty() || sb.endsWith("\n\n")) {
                sb.append(l)
                lungPrec = l.length
                continue
            }

            val ultim = sb[sb.length - 1]
            val prim = l[0]
            val randScurt = lungPrec < mediana * 0.80
            val incepeVers = prim.isDigit() && l.length > 2 &&
                    (l[1] == ' ' || (l[1].isDigit() && l.length > 3 && l[2] == ' '))

            when {
                ultim == '-' && prim.isLowerCase() -> {
                    sb.setLength(sb.length - 1)
                    sb.append(l)
                }
                sfarsitFraza.indexOf(ultim) >= 0 && (randScurt || incepeVers) -> {
                    sb.append("\n\n").append(l)
                }
                else -> {
                    sb.append(' ').append(l)
                }
            }
            lungPrec = l.length
        }
        return sb.toString().replace(Regex("\\n{3,}"), "\n\n").trim()
    }

    // ---------- TXT si EPUB ----------

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

    // ---------- CBZ ----------

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
