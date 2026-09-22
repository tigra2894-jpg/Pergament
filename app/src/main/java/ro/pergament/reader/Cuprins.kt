package ro.pergament.reader

import java.util.Locale

data class Capitol(val titlu: String, val pagina: Int, val nivel: Int = 0)

/**
 * Gaseste capitolele intr-o carte de text.
 * Nu avem cuprinsul scris nicaieri, asa ca il deducem din text:
 * randuri scurte, singure, care arata a titlu de capitol.
 */
object Cuprins {

    private val tipare = listOf(
        // Capitolul I, CAPITOLUL 12, Cap. 3
        Regex("^cap(itolul|\\.)?\\s+([ivxlcdm]+|\\d{1,3})\\b", RegexOption.IGNORE_CASE),
        // Partea a doua, PARTEA I
        Regex("^partea\\s+", RegexOption.IGNORE_CASE),
        Regex("^cartea\\s+", RegexOption.IGNORE_CASE),
        Regex("^volumul\\s+", RegexOption.IGNORE_CASE),
        Regex("^(prolog|epilog|prefata|prefață|introducere|cuvant inainte|cuvânt înainte|postfata|postfață|anexa|anexă|bibliografie|cuprins|note)\\b", RegexOption.IGNORE_CASE),
        // Chapter 5, Part II, Unit 3, Lesson 7
        Regex("^(chapter|part|unit|lesson|section|book)\\s+([ivxlcdm]+|\\d{1,3})\\b", RegexOption.IGNORE_CASE),
        // Lectia 4, Capitol 2
        Regex("^(lectia|lecția|sectiunea|secțiunea|tema)\\s+\\d{1,3}\\b", RegexOption.IGNORE_CASE),
        // carti biblice si numerotari: "1 Geneza", "Psalmul 23"
        Regex("^(psalmul|psalmi)\\s+\\d{1,3}\\b", RegexOption.IGNORE_CASE)
    )

    private val carti = listOf(
        "geneza", "exodul", "leviticul", "numeri", "deuteronomul", "iosua",
        "judecatori", "rut", "samuel", "regi", "cronici", "ezra", "neemia",
        "estera", "iov", "psalmii", "proverbele", "eclesiastul", "isaia",
        "ieremia", "plangerile", "ezechiel", "daniel", "osea", "ioel", "amos",
        "iona", "mica", "naum", "habacuc", "tefania", "hagai", "zaharia",
        "maleahi", "matei", "marcu", "luca", "ioan", "faptele", "romani",
        "corinteni", "galateni", "efeseni", "filipeni", "coloseni",
        "tesaloniceni", "timotei", "tit", "filimon", "evrei", "iacov",
        "petru", "iuda", "apocalipsa"
    )

    fun gaseste(pagini: List<String>): List<Capitol> {
        val gasite = mutableListOf<Capitol>()
        val vazute = HashSet<String>()

        for ((idx, pagina) in pagini.withIndex()) {
            // ne uitam doar la inceputul paginii: capitolele incep pe pagina noua
            val primeleRanduri = pagina.take(500).split("\n")
            for ((r, randBrut) in primeleRanduri.withIndex()) {
                if (r > 4) break
                val rand = randBrut.trim()
                if (rand.length < 3 || rand.length > 90) continue
                if (!rand.any { it.isLetter() }) continue

                var nivel = -1
                for ((i, t) in tipare.withIndex()) {
                    if (t.containsMatchIn(rand)) {
                        nivel = if (i <= 3) 0 else 1
                        break
                    }
                }
                if (nivel < 0) {
                    val j = faraDiacritice(rand)
                    for (c in carti) {
                        if (j.startsWith(c) || j.startsWith("$c ") ||
                            Regex("^[1-3]\\s+$c").containsMatchIn(j)
                        ) {
                            nivel = 0
                            break
                        }
                    }
                }
                if (nivel < 0) continue

                val cheie = faraDiacritice(rand).replace(Regex("[^a-z0-9]"), "")
                if (cheie.isBlank() || vazute.contains(cheie)) continue
                vazute.add(cheie)
                gasite.add(Capitol(rand.take(70), idx, nivel))
                break
            }
        }

        // daca nu gasim nimic, impartim cartea in bucati egale
        if (gasite.size < 2 && pagini.size > 12) {
            val n = 12
            val pas = pagini.size / n
            return (0 until n).map { i ->
                Capitol("Partea ${i + 1}", i * pas, 0)
            }
        }
        return gasite
    }

    fun faraDiacritice(s: String): String {
        return s.replace('ă', 'a').replace('â', 'a').replace('î', 'i')
            .replace('ș', 's').replace('ş', 's')
            .replace('ț', 't').replace('ţ', 't')
            .replace('Ă', 'a').replace('Â', 'a').replace('Î', 'i')
            .replace('Ș', 's').replace('Ț', 't')
            .lowercase(Locale.ROOT)
    }
}
