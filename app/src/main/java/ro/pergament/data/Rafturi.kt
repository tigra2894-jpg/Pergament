package ro.pergament.data

import java.text.Normalizer
import java.util.Locale

object Rafturi {

    val ordine = listOf(
        "Literatură", "Poezie", "Istorie", "Filosofie", "Religie",
        "Psihologie", "Științe", "Tehnică & Construcții", "Afaceri & Bani",
        "Sănătate", "Artă & Muzică", "Gătit", "Călătorii", "Limbi străine",
        "Copii", "Benzi desenate", "Manuale & Ghiduri", "Diverse"
    )

    private val cuvinte: List<Pair<String, List<String>>> = listOf(
        "Poezie" to listOf("poez", "versuri", "sonet", "balad", "elegii", "lirica"),
        "Istorie" to listOf("istori", "razboi", "imperiu", "dacia", "medieval", "revolut", "cronica", "dinastia", "1848", "1918", "1989"),
        "Filosofie" to listOf("filosof", "filozof", "etica", "metafizic", "logica", "stoic", "nietzsche", "platon", "aristotel", "seneca", "kant"),
        "Religie" to listOf("biblia", "biblie", "evanghel", "psalm", "rugaciun", "ortodox", "crestin", "teolog", "sfantul", "sfanta", "coran", "budism", "spiritual"),
        "Psihologie" to listOf("psiholog", "psihic", "mintea", "creier", "emotii", "anxiet", "depres", "terapie", "subconstient", "comportament", "freud", "jung"),
        "Științe" to listOf("fizica", "chimie", "biolog", "matemat", "astronom", "univers", "cuantic", "evolut", "geolog", "stiinta", "stiinte"),
        "Tehnică & Construcții" to listOf("constructi", "instalati", "electric", "electro", "mecanic", "auto", "motor", "tehnic", "inginer", "zidarie", "tamplarie", "sudura", "proiectare", "arhitect", "programare", "python", "java", "linux", "calculator"),
        "Afaceri & Bani" to listOf("afaceri", "business", "marketing", "vanzari", "bani", "investit", "bursa", "antrepren", "economi", "finant", "contabil", "management", "succes", "milionar", "bogat"),
        "Sănătate" to listOf("sanatate", "medicin", "anatomi", "nutrit", "diet", "vindecare", "remedii", "plante medicinale", "fitness", "sport", "yoga", "boli"),
        "Artă & Muzică" to listOf("arta", "pictur", "sculptur", "muzic", "chitara", "pian", "desen", "fotograf", "design", "film", "teatru"),
        "Gătit" to listOf("retet", "bucatar", "gatit", "culinar", "prajitur", "paine", "cofetar", "mancare"),
        "Călătorii" to listOf("calator", "ghid turistic", "turism", "harta", "jurnal de calatorie"),
        "Limbi străine" to listOf("dictionar", "gramatic", "engleza", "germana", "italiana", "spaniola", "franceza", "curs de limba", "vocabular", "conversatie"),
        "Copii" to listOf("copii", "povesti", "basme", "prichindel", "pentru cei mici", "fabule", "ilustrat"),
        "Manuale & Ghiduri" to listOf("manual", "ghid", "curs", "tutorial", "indrumar", "instructiuni", "lectii", "clasa a"),
        "Literatură" to listOf("roman", "nuvel", "povestir", "aventur", "dragoste", "politist", "mister", "thriller", "science fiction", "fantasy", "clasic", "opere")
    )

    fun faraDiacritice(s: String): String {
        val n = Normalizer.normalize(s, Normalizer.Form.NFD)
        return n.replace(Regex("\\p{Mn}+"), "")
            .replace('ș', 's').replace('ţ', 't').replace('ț', 't')
            .lowercase(Locale.ROOT)
    }

    fun detecteaza(titlu: String, autor: String, format: String): String {
        if (format.equals("CBZ", true) || format.equals("CBR", true)) return "Benzi desenate"
        val t = faraDiacritice("$titlu $autor")
        for ((raft, chei) in cuvinte) {
            for (c in chei) {
                if (t.contains(c)) return raft
            }
        }
        return "Diverse"
    }

    fun sorteaza(rafturiGasite: Set<String>): List<String> {
        val cunoscute = ordine.filter { rafturiGasite.contains(it) }
        val restul = rafturiGasite.filter { !ordine.contains(it) }.sorted()
        return cunoscute + restul
    }
}
