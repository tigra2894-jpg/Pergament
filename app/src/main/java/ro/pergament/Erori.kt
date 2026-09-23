package ro.pergament

import android.app.Application
import android.content.Context
import android.content.Intent
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Prinde erorile care ar inchide aplicatia brusc.
 * In loc de disparitie fara explicatie, scriem ce s-a intamplat
 * si deschidem un ecran care spune omului ce sa faca.
 */
class Pergament : Application() {

    override fun onCreate() {
        super.onCreate()
        val vechi = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { fir, eroare ->
            try {
                Jurnal.scrie(this, eroare)
                val i = Intent(this, EcranEroare::class.java)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                i.putExtra("mesaj", Jurnal.peScurt(eroare))
                startActivity(i)
            } catch (e: Throwable) {
            }
            try {
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(10)
            } catch (e: Throwable) {
                vechi?.uncaughtException(fir, eroare)
            }
        }
    }
}

object Jurnal {

    private fun fisier(ctx: Context) = File(ctx.filesDir, "erori.txt")

    fun scrie(ctx: Context, e: Throwable) {
        try {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            val data = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.ROOT).format(Date())
            val text = buildString {
                append("=== $data ===\n")
                append("Pergament 1.1\n")
                append("Android ${android.os.Build.VERSION.RELEASE}, ")
                append("${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n")
                append(sw.toString())
                append("\n\n")
            }
            val f = fisier(ctx)
            // pastram doar ultimele erori, sa nu creasca la nesfarsit
            val vechi = if (f.exists()) f.readText().takeLast(20_000) else ""
            f.writeText(vechi + text)
        } catch (x: Throwable) {
        }
    }

    fun citeste(ctx: Context): String {
        return try {
            val f = fisier(ctx)
            if (f.exists()) f.readText() else ""
        } catch (e: Exception) {
            ""
        }
    }

    fun goleste(ctx: Context) {
        try {
            fisier(ctx).delete()
        } catch (e: Exception) {
        }
    }

    fun areErori(ctx: Context): Boolean = citeste(ctx).isNotBlank()

    /** Explicatie pe intelesul omului, nu limbaj de programator. */
    fun peScurt(e: Throwable): String {
        val nume = e::class.java.simpleName
        val text = (e.message ?: "").lowercase(Locale.ROOT)
        return when {
            e is OutOfMemoryError ->
                "Cartea a cerut prea multă memorie. Încearcă să o deschizi în modul text, " +
                        "sau închide alte aplicații și reîncearcă."
            nume.contains("SecurityException") || text.contains("permission") ->
                "Aplicația nu mai are dreptul de a citi fișierul. Adaugă cartea din nou."
            nume.contains("FileNotFound") || text.contains("no such file") ->
                "Fișierul cărții nu mai există pe telefon."
            text.contains("no space") ->
                "Nu mai e loc în memoria telefonului."
            else ->
                "Ceva neprevăzut s-a întâmplat. Nimic din biblioteca ta nu s-a pierdut."
        }
    }
}
