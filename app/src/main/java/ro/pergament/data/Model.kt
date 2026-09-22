package ro.pergament.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Notita(val pagina: Int, val text: String, val creat: Long)

data class Carte(
    val id: String,
    val uri: String,
    val titlu: String,
    val autor: String,
    val format: String,
    val raft: String,
    val adaugat: Long,
    val coperta: String? = null,
    val paginaCurenta: Int = 0,
    val totalPagini: Int = 0,
    val favorita: Boolean = false,
    val semne: List<Int> = emptyList(),
    val notite: List<Notita> = emptyList()
) {
    val progres: Float
        get() = if (totalPagini > 1) (paginaCurenta.toFloat() / (totalPagini - 1)).coerceIn(0f, 1f) else 0f
}

object Biblioteca {

    private fun fisier(ctx: Context) = File(ctx.filesDir, "biblioteca.json")

    fun incarca(ctx: Context): List<Carte> {
        val f = fisier(ctx)
        if (!f.exists()) return emptyList()
        return try {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).map { i -> dinJson(arr.getJSONObject(i)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun salveaza(ctx: Context, carti: List<Carte>) {
        try {
            val arr = JSONArray()
            carti.forEach { arr.put(inJson(it)) }
            fisier(ctx).writeText(arr.toString())
        } catch (e: Exception) {
        }
    }

    private fun inJson(c: Carte): JSONObject {
        val o = JSONObject()
        o.put("id", c.id)
        o.put("uri", c.uri)
        o.put("titlu", c.titlu)
        o.put("autor", c.autor)
        o.put("format", c.format)
        o.put("raft", c.raft)
        o.put("adaugat", c.adaugat)
        o.put("coperta", c.coperta ?: "")
        o.put("paginaCurenta", c.paginaCurenta)
        o.put("totalPagini", c.totalPagini)
        o.put("favorita", c.favorita)
        val s = JSONArray()
        c.semne.forEach { s.put(it) }
        o.put("semne", s)
        val n = JSONArray()
        c.notite.forEach {
            val no = JSONObject()
            no.put("pagina", it.pagina)
            no.put("text", it.text)
            no.put("creat", it.creat)
            n.put(no)
        }
        o.put("notite", n)
        return o
    }

    private fun dinJson(o: JSONObject): Carte {
        val semne = mutableListOf<Int>()
        val sa = o.optJSONArray("semne")
        if (sa != null) for (i in 0 until sa.length()) semne.add(sa.getInt(i))
        val notite = mutableListOf<Notita>()
        val na = o.optJSONArray("notite")
        if (na != null) for (i in 0 until na.length()) {
            val no = na.getJSONObject(i)
            notite.add(Notita(no.optInt("pagina"), no.optString("text"), no.optLong("creat")))
        }
        val cop = o.optString("coperta")
        return Carte(
            id = o.optString("id"),
            uri = o.optString("uri"),
            titlu = o.optString("titlu"),
            autor = o.optString("autor"),
            format = o.optString("format"),
            raft = o.optString("raft", "Diverse"),
            adaugat = o.optLong("adaugat"),
            coperta = if (cop.isNullOrBlank()) null else cop,
            paginaCurenta = o.optInt("paginaCurenta"),
            totalPagini = o.optInt("totalPagini"),
            favorita = o.optBoolean("favorita"),
            semne = semne,
            notite = notite
        )
    }
}

data class Setari(
    val marimeText: Float = 20f,
    val inaltimeRand: Float = 1.7f,
    val margine: Float = 22f,
    val tema: Int = 0,
    val intoarcereCurl: Boolean = true,
    val taiePdf: Boolean = true,
    val pdfCaText: Boolean = true
)

object SetariStore {
    private fun fisier(ctx: Context) = File(ctx.filesDir, "setari.json")

    fun incarca(ctx: Context): Setari {
        val f = fisier(ctx)
        if (!f.exists()) return Setari()
        return try {
            val o = JSONObject(f.readText())
            Setari(
                marimeText = o.optDouble("marimeText", 20.0).toFloat(),
                inaltimeRand = o.optDouble("inaltimeRand", 1.7).toFloat(),
                margine = o.optDouble("margine", 22.0).toFloat(),
                tema = o.optInt("tema", 0),
                intoarcereCurl = o.optBoolean("intoarcereCurl", true),
                taiePdf = o.optBoolean("taiePdf", true),
                pdfCaText = o.optBoolean("pdfCaText", true)
            )
        } catch (e: Exception) {
            Setari()
        }
    }

    fun salveaza(ctx: Context, s: Setari) {
        try {
            val o = JSONObject()
            o.put("marimeText", s.marimeText.toDouble())
            o.put("inaltimeRand", s.inaltimeRand.toDouble())
            o.put("margine", s.margine.toDouble())
            o.put("tema", s.tema)
            o.put("intoarcereCurl", s.intoarcereCurl)
            o.put("taiePdf", s.taiePdf)
            o.put("pdfCaText", s.pdfCaText)
            fisier(ctx).writeText(o.toString())
        } catch (e: Exception) {
        }
    }
}
