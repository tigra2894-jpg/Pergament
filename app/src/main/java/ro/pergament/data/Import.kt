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

    private val GUNOI = setOf(
        "pdf", "epub", "txt", "download", "downloads", "downloaded",
        "free", "gratis", "ebook", "ebooks", "e-book", "scan", "scanat",
        "ocr", "copy", "copie", "final", "new", "zlib", "z-lib",
        "libgen", "annas", "archive", "ru", "en", "vol", "ed"
    )

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

    private fun titluCurat(brut: String): String {
        var s = brut
        s = s.replace(Regex("(?i)\\bwww\\.[^\\s]+"), " ")
        s = s.replace(Regex("(?i)\\b[a-z0-9-]+\\.(com|net|org|ro|info|xyz|site)\\b"), " ")
        s = s.replace(Regex("[._\\-–—]+"), " ")
        s = s.replace(Regex("[\\[\\]{}()]+"), " ")
        s = s.replace(Regex("\\s+"), " ").trim()

        val pastrate = s.split(" ").filter { cuv ->
            val c = cuv.lowercase(Locale.ROOT).trim('\'', '"', ',')
            c.isNotBlank() && !GUNOI.contains(c)
        }
        s = pastrate.joinToString(" ")

        s = s.split(" ").joinToString(" ") { w ->
            if (w.length > 2 && w == w.uppercase(Locale.ROOT) && w.any { it.isLetter() })
                w.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase() }
            else w
        }
        return s.trim().trim(',', ';', '-').trim()
    }

    /**
     * Desparte autorul de titlu DOAR daca numele fisierului contine
     * " - " cu spatii in jur, inainte de curatare. Asa "English-grammar-in-Use"
     * nu mai e taiat gresit, fiindca liniutele lui nu au spatii.
     */
    private fun desparteAutor(numeBrut: String): Pair<String, String> {
        val fara = numeBrut.substringBeforeLast('.')
        val p = fara.split(" - ", " – ", " — ", limit = 2)
        if (p.size == 2) {
            val stanga = p[0].trim()
            val dreapta = p[1].trim()
            if (pareNume(stanga) && dreapta.length >= 3) {
                return titluCurat(dreapta) to stanga
            }
        }
        return titluCurat(fara) to ""
    }

    /** Un autor are 1-4 cuvinte, litere, si nu e o propozitie. */
    private fun pareNume(s: String): Boolean {
        if (s.length !in 3..45) return false
        val cuv = s.split(" ").filter { it.isNotBlank() }
        if (cuv.size !in 1..4) return false
        if (!s.any { it.isLetter() }) return false
        if (s.count { it.isDigit() } > 4) return false
        val j = Rafturi.faraDiacritice(s)
        val legaturi = listOf(" de ", " a ", " si ", " in ", " la ", " cu ", " pe ", " the ", " of ", " and ")
        for (l in legaturi) if (j.contains(l)) return false
        return true
    }

    /** Numele celor care urca fisiere pe net, nu autori. */
    private fun autorValid(a: String, titlu: String): Boolean {
        if (!pareNume(a)) return false
        val j = Rafturi.faraDiacritice(a)
        val t = Rafturi.faraDiacritice(titlu)
        // autorul nu poate fi tot titlul
        if (t.contains(j) || j.contains(t)) return false
        val respinse = listOf(
            "anysam", "admin", "user", "windows", "microsoft", "adobe",
            "acrobat", "calibre", "scanner", "hp", "canon", "epson",
            "unknown", "necunoscut", "owner", "pc", "laptop", "office",
            "word", "writer", "creator", "author", "autor"
        )
        for (r in respinse) if (j.contains(r)) return false
        // nume in alfabet chirilic sau alte alfabete: nu ne putem baza pe ele
        val latine = a.count { it.isLetter() && it.code < 0x250 }
        val toate = a.count { it.isLetter() }
        if (toate > 0 && latine.toFloat() / toate < 0.7f) return false
        return true
    }

    private fun titluValid(t: String): Boolean {
        if (t.length < 3 || t.length > 150) return false
        if (!t.any { it.isLetter() }) return false
        val j = t.lowercase(Locale.ROOT)
        val respins = listOf(
            "microsoft word", "untitled", "fara titlu", "document1",
            ".indd", ".doc", ".qxd", ".pmd", "layout", "printer",
            "adobe", "pagemaker", "acrobat", "calibre"
        )
        for (r in respins) if (j.contains(r)) return false
        return true
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

        val dinNume = desparteAutor(nume)
        var titlu = dinNume.first
        var autor = dinNume.second

        var pagini = 0

        if (format == "PDF") {
            pagini = numaraPaginiPdf(ctx, uri)
            val meta = metadatePdf(ctx, uri)
            if (titluValid(meta.first)) titlu = titluCurat(meta.first)
            if (autor.isBlank() && autorValid(meta.second, titlu)) autor = meta.second.trim()
        }

        if (format == "EPUB") {
            val meta = metadateEpub(ctx, uri)
            if (titluValid(meta.first)) titlu = meta.first.trim()
            if (autorValid(meta.second, titlu)) autor = meta.second.trim()
        }

        if (!titluValid(titlu)) titlu = "Carte fără titlu"
        if (!autorValid(autor, titlu)) autor = ""

        val id = "b" + System.currentTimeMillis() + "_" +
                uri.toString().hashCode().toString().replace("-", "n")

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

    // ---------- metadate PDF ----------

    private fun metadatePdf(ctx: Context, uri: Uri): Pair<String, String> {
        try {
            val limita = 3 * 1024 * 1024
            val buf = ByteArray(limita)
            var n = 0
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                while (n < limita) {
                    val r = input.read(buf, n, limita - n)
                    if (r <= 0) break
                    n += r
                }
            }
            if (n <= 0) return "" to ""
            val s = String(buf, 0, n, Charsets.ISO_8859_1)
            return extrageCamp(s, "Title") to extrageCamp(s, "Author")
        } catch (e: Exception) {
            return "" to ""
        }
    }

    private fun extrageCamp(s: String, camp: String): String {
        try {
            val hex = Regex("/$camp\\s*<([0-9A-Fa-f\\s]{4,})>").find(s)
            if (hex != null) {
                val t = decodeHex(hex.groupValues[1])
                if (t.isNotBlank()) return t
            }
            val lit = Regex("/$camp\\s*\\(((?:\\\\.|[^\\\\)]){0,300})\\)").find(s)
            if (lit != null) {
                val t = decodeLiteral(lit.groupValues[1])
                if (t.isNotBlank()) return t
            }
            if (camp == "Title") {
                val xmp = Regex("<dc:title>.*?<rdf:li[^>]*>(.{2,200}?)</rdf:li>",
                    RegexOption.DOT_MATCHES_ALL).find(s)
                if (xmp != null) return xmp.groupValues[1].trim()
            }
        } catch (e: Exception) {
        }
        return ""
    }

    private fun decodeHex(brut: String): String {
        val curat = brut.replace(Regex("\\s"), "")
        if (curat.length < 4 || curat.length % 2 != 0) return ""
        val octeti = ByteArray(curat.length / 2)
        for (i in octeti.indices) {
            octeti[i] = curat.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return decodeOcteti(octeti)
    }

    private fun decodeLiteral(brut: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < brut.length) {
            val c = brut[i]
            if (c == '\\' && i + 1 < brut.length) {
                val u = brut[i + 1]
                when (u) {
                    'n', 'r', 't' -> { sb.append(' '); i += 2 }
                    '(', ')', '\\' -> { sb.append(u); i += 2 }
                    else -> {
                        if (u.isDigit() && i + 3 < brut.length) {
                            val oct = brut.substring(i + 1, i + 4)
                            val v = oct.toIntOrNull(8)
                            if (v != null) sb.append(v.toChar()) else sb.append(u)
                            i += 4
                        } else {
                            sb.append(u); i += 2
                        }
                    }
                }
            } else {
                sb.append(c); i++
            }
        }
        val octeti = ByteArray(sb.length)
        for (k in sb.indices) octeti[k] = sb[k].code.toByte()
        return decodeOcteti(octeti)
    }

    private fun decodeOcteti(o: ByteArray): String {
        if (o.size >= 2 && (o[0].toInt() and 0xFF) == 0xFE && (o[1].toInt() and 0xFF) == 0xFF) {
            return String(o, 2, o.size - 2, Charsets.UTF_16BE).trim()
        }
        return String(o, Charsets.ISO_8859_1).trim()
    }

    // ---------- PDF ----------

    private fun numaraPaginiPdf(ctx: Context, uri: Uri): Int {
        return try {
            ctx.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { r -> r.pageCount }
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    // ---------- EPUB ----------

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
                            return t.take(140) to a.take(60)
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

    // ---------- coperti ----------

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
