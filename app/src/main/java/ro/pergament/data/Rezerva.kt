package ro.pergament.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RezultatRestaurare(
    val reusit: Boolean,
    val carti: List<Carte>,
    val setari: Setari?,
    val mesaj: String
)

object Rezerva {

    private const val VERSIUNE = 1

    fun numeFisier(): String {
        val d = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.ROOT).format(Date())
        return "pergament_$d.json"
    }

    /** Scrie tot ce stie aplicatia intr-un fisier ales de utilizator. */
    fun salveaza(ctx: Context, uri: Uri, carti: List<Carte>, setari: Setari): String {
        return try {
            val o = JSONObject()
            o.put("versiune", VERSIUNE)
            o.put("aplicatie", "Pergament")
            o.put("creat", System.currentTimeMillis())
            o.put("nrCarti", carti.size)

            val arr = JSONArray()
            for (c in carti) {
                val k = JSONObject()
                k.put("id", c.id)
                k.put("uri", c.uri)
                k.put("titlu", c.titlu)
                k.put("autor", c.autor)
                k.put("format", c.format)
                k.put("raft", c.raft)
                k.put("adaugat", c.adaugat)
                k.put("paginaCurenta", c.paginaCurenta)
                k.put("totalPagini", c.totalPagini)
                k.put("favorita", c.favorita)
                val s = JSONArray()
                c.semne.forEach { s.put(it) }
                k.put("semne", s)
                val n = JSONArray()
                c.notite.forEach {
                    val no = JSONObject()
                    no.put("pagina", it.pagina)
                    no.put("text", it.text)
                    no.put("creat", it.creat)
                    n.put(no)
                }
                k.put("notite", n)
                arr.put(k)
            }
            o.put("carti", arr)

            val s = JSONObject()
            s.put("marimeText", setari.marimeText.toDouble())
            s.put("inaltimeRand", setari.inaltimeRand.toDouble())
            s.put("margine", setari.margine.toDouble())
            s.put("tema", setari.tema)
            s.put("intoarcereCurl", setari.intoarcereCurl)
            s.put("taiePdf", setari.taiePdf)
            s.put("pdfCaText", setari.pdfCaText)
            o.put("setari", s)

            ctx.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(o.toString(2).toByteArray(Charsets.UTF_8))
            } ?: return "Nu am putut scrie fișierul."

            "Am salvat ${carti.size} cărți în copia de siguranță."
        } catch (e: Exception) {
            "Salvarea nu a reușit."
        }
    }

    /**
     * Citeste o copie de siguranta si o imbina cu biblioteca de acum.
     * Cartile existente isi pastreaza fisierul, dar primesc inapoi
     * progresul, semnele si notitele din copie.
     */
    fun restaureaza(ctx: Context, uri: Uri, existente: List<Carte>): RezultatRestaurare {
        try {
            val text = ctx.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            } ?: return RezultatRestaurare(false, existente, null, "Nu am putut citi fișierul.")

            val o = JSONObject(text)
            if (o.optString("aplicatie") != "Pergament") {
                return RezultatRestaurare(false, existente, null,
                    "Acest fișier nu este o copie de siguranță Pergament.")
            }

            val arr = o.optJSONArray("carti")
                ?: return RezultatRestaurare(false, existente, null, "Fișierul nu conține cărți.")

            val dinCopie = mutableListOf<Carte>()
            for (i in 0 until arr.length()) {
                val k = arr.getJSONObject(i)
                val semne = mutableListOf<Int>()
                k.optJSONArray("semne")?.let { sa ->
                    for (j in 0 until sa.length()) semne.add(sa.getInt(j))
                }
                val notite = mutableListOf<Notita>()
                k.optJSONArray("notite")?.let { na ->
                    for (j in 0 until na.length()) {
                        val no = na.getJSONObject(j)
                        notite.add(Notita(no.optInt("pagina"), no.optString("text"), no.optLong("creat")))
                    }
                }
                dinCopie.add(
                    Carte(
                        id = k.optString("id"),
                        uri = k.optString("uri"),
                        titlu = k.optString("titlu"),
                        autor = k.optString("autor"),
                        format = k.optString("format"),
                        raft = k.optString("raft", "Diverse"),
                        adaugat = k.optLong("adaugat"),
                        coperta = null,
                        paginaCurenta = k.optInt("paginaCurenta"),
                        totalPagini = k.optInt("totalPagini"),
                        favorita = k.optBoolean("favorita"),
                        semne = semne,
                        notite = notite
                    )
                )
            }

            val dupaUri = existente.associateBy { it.uri }
            val rezultat = mutableListOf<Carte>()
            val folosite = HashSet<String>()
            var actualizate = 0
            var adaugate = 0

            for (c in dinCopie) {
                val vechi = dupaUri[c.uri]
                if (vechi != null) {
                    // cartea exista: pastram coperta si fisierul, luam datele din copie
                    rezultat.add(
                        vechi.copy(
                            titlu = c.titlu,
                            autor = c.autor,
                            raft = c.raft,
                            paginaCurenta = c.paginaCurenta,
                            totalPagini = c.totalPagini,
                            favorita = c.favorita,
                            semne = c.semne,
                            notite = c.notite
                        )
                    )
                    folosite.add(vechi.id)
                    actualizate++
                } else {
                    rezultat.add(c)
                    adaugate++
                }
            }

            // cartile care nu sunt in copie raman asa cum sunt
            for (v in existente) {
                if (!folosite.contains(v.id) && rezultat.none { it.uri == v.uri }) {
                    rezultat.add(v)
                }
            }

            var setari: Setari? = null
            o.optJSONObject("setari")?.let { s ->
                setari = Setari(
                    marimeText = s.optDouble("marimeText", 20.0).toFloat(),
                    inaltimeRand = s.optDouble("inaltimeRand", 1.7).toFloat(),
                    margine = s.optDouble("margine", 22.0).toFloat(),
                    tema = s.optInt("tema", 0),
                    intoarcereCurl = s.optBoolean("intoarcereCurl", true),
                    taiePdf = s.optBoolean("taiePdf", true),
                    pdfCaText = s.optBoolean("pdfCaText", true)
                )
            }

            val mesaj = buildString {
                append("Am restaurat: ")
                if (actualizate > 0) append("$actualizate actualizate")
                if (actualizate > 0 && adaugate > 0) append(", ")
                if (adaugate > 0) append("$adaugate adăugate")
                if (actualizate == 0 && adaugate == 0) append("nimic nou")
                append(".")
                if (adaugate > 0) {
                    append(" Cărțile adăugate se deschid doar dacă fișierele lor mai există pe telefon.")
                }
            }

            return RezultatRestaurare(true, rezultat, setari, mesaj)
        } catch (e: Exception) {
            return RezultatRestaurare(false, existente, null, "Fișierul nu a putut fi citit.")
        }
    }
}
