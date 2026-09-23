package ro.pergament.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Cartile aduse in aplicatie: fisierul se copiaza in memoria ei,
 * ca sa nu mai depinda de unde a fost luat.
 */
object Depozit {

    private fun folder(ctx: Context): File {
        val d = File(ctx.filesDir, "carti")
        if (!d.exists()) d.mkdirs()
        return d
    }

    fun fisier(ctx: Context, id: String, format: String): File {
        val ext = when (format) {
            "PDF" -> "pdf"
            "EPUB" -> "epub"
            "CBZ" -> "cbz"
            else -> "txt"
        }
        return File(folder(ctx), "$id.$ext")
    }

    /** Cat loc ocupa toate cartile copiate, in octeti. */
    fun spatiuFolosit(ctx: Context): Long {
        return try {
            folder(ctx).listFiles()?.sumOf { it.length() } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun spatiuCitibil(octeti: Long): String {
        val mb = octeti / (1024.0 * 1024.0)
        return if (mb >= 1024) String.format("%.1f GB", mb / 1024)
        else String.format("%.0f MB", mb)
    }

    /**
     * Copiaza fisierul in aplicatie. Intoarce adresa noua,
     * sau null daca nu a reusit (fara spatiu, fisier ilizibil).
     */
    fun copiaza(ctx: Context, sursa: Uri, id: String, format: String): String? {
        return try {
            val tinta = fisier(ctx, id, format)
            ctx.contentResolver.openInputStream(sursa)?.use { input ->
                tinta.outputStream().use { out ->
                    input.copyTo(out, 128 * 1024)
                }
            } ?: return null
            if (tinta.length() < 100) {
                tinta.delete()
                return null
            }
            Uri.fromFile(tinta).toString()
        } catch (e: Exception) {
            try {
                fisier(ctx, id, format).delete()
            } catch (x: Exception) {
            }
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    fun sterge(ctx: Context, id: String, format: String) {
        try {
            fisier(ctx, id, format).delete()
        } catch (e: Exception) {
        }
    }

    /** Cartea sta in aplicatie sau e doar imprumutata de afara? */
    fun esteInAplicatie(uri: String): Boolean =
        uri.startsWith("file://") && uri.contains("/carti/")

    /** Verifica daca fisierul chiar exista si poate fi deschis. */
    fun existaFisier(ctx: Context, uri: String): Boolean {
        return try {
            if (esteInAplicatie(uri)) {
                File(Uri.parse(uri).path ?: return false).exists()
            } else {
                ctx.contentResolver.openInputStream(Uri.parse(uri))?.use { true } ?: false
            }
        } catch (e: Exception) {
            false
        }
    }

    /** Adresa prin care alte aplicatii pot primi cartea (partajare). */
    fun uriPartajabil(ctx: Context, id: String, format: String): Uri? {
        return try {
            val f = fisier(ctx, id, format)
            if (!f.exists()) return null
            FileProvider.getUriForFile(ctx, "${ctx.packageName}.fisiere", f)
        } catch (e: Exception) {
            null
        }
    }
}
