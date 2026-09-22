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
        "Religie" to listOf(
            "biblia", "biblie", "bible", "evanghel", "gospel", "psalm",
            "rugaciun", "prayer", "ortodox", "crestin", "christian", "christ",
            "iisus", "isus", "jesus", "dumnezeu", "god's", " god ", "gods ",
            "heaven", "rai ", "mantuire", "salvation", "credinta", "faith",
            "biserica", "church", "teolog", "theolog", "sfantul", "sfanta",
            "saint", "apostol", "profet", "prophet", "coran", "quran",
            "budism", "buddh", "spiritual", "duhul", "spirit", "ntr ",
            "noul testament", "vechiul testament", "testament", "thru the bible"
        ),
        "Limbi străine" to listOf(
            "english", "grammar", "vocabulary", "dictionary", "phrasal",
            "idioms", "deutsch", "italiano", "espanol", "francais",
            "dictionar", "gramatic", "engleza", "germana", "italiana",
            "spaniola", "franceza", "curs de limba", "vocabular", "conversatie",
            "in use", "workbook", "student book", "elementary",
            "intermediate", "advanced", "beginner", "ielts", "toefl", "cambridge"
        ),
        "Benzi desenate" to listOf("comic", "manga", "benzi desenate"),
        "Poezie" to listOf(
            "poez", "versuri", "sonet", "balad", "elegii", "lirica",
            "poetry", "poems"
        ),
        "Filosofie" to listOf(
            "filosof", "filozof", "etica", "metafizic", "logica", "stoic",
            "dialectic", "retoric", "nietzsche", "platon", "aristotel",
            "seneca", "kant", "schopenhauer", "marc aureliu", "epictet",
            "philosophy", "meditatii", "dreptate", "curajul de a nu fi"
        ),
        "Psihologie" to listOf(
            "psiholog", "psihic", "mintea", "creier", "emotii", "anxiet",
            "depres", "terapie", "subconstient", "comportament", "freud",
            "jung", "adler", "psychology", "mindset", "habits", "obiceiuri",
            "fericit", "happiness", "self help", "dezvoltare personala",
            "incredere in sine", "stima de sine", "curaj"
        ),
        "Istorie" to listOf(
            "istori", "razboi", "imperiu", "dacia", "medieval", "revolut",
            "cronica", "dinastia", "1848", "1918", "1989", "history",
            "world war", "empire", "antichitate"
        ),
        "Științe" to listOf(
            "fizica", "chimie", "biolog", "matemat", "astronom", "univers",
            "cuantic", "evolut", "geolog", "stiinta", "stiinte", "physics",
            "chemistry", "biology", "mathematics", "science"
        ),
        "Tehnică & Construcții" to listOf(
            "constructi", "instalati", "electric", "electro", "mecanic",
            "automobil", "motoare", "tehnic", "inginer", "zidarie",
            "tamplarie", "sudura", "proiectare", "arhitect", "programare",
            "python", "java", "linux", "calculator", "engineering",
            "building", "faianta", "gresie", "rigips", "sanitare"
        ),
        "Afaceri & Bani" to listOf(
            "afaceri", "business", "marketing", "vanzari", "bani", "investit",
            "bursa", "antrepren", "economi", "finant", "contabil",
            "management", "milionar", "bogat", "money", "rich ",
            "startup", "trading"
        ),
        "Sănătate" to listOf(
            "sanatate", "medicin", "anatomi", "nutrit", "diet", "vindecare",
            "remedii", "plante medicinale", "fitness", "yoga", "boli",
            "health", "medical", "antrenament"
        ),
        "Gătit" to listOf(
            "retet", "bucatar", "gatit", "culinar", "prajitur", "paine",
            "cofetar", "mancare", "recipe", "cooking", "cookbook"
        ),
        "Artă & Muzică" to listOf(
            "pictur", "sculptur", "muzic", "chitara", "pian", "desen",
            "fotograf", "teatru", "istoria artei", "arta plastica",
            "music", "guitar", "painting", "drawing", "photography"
        ),
        "Călătorii" to listOf(
            "calator", "ghid turistic", "turism", "jurnal de calatorie",
            "travel", "lonely planet"
        ),
        "Copii" to listOf(
            "copii", "povesti", "basme", "prichindel", "pentru cei mici",
            "fabule", "ilustrat", "children", "fairy tales"
        ),
        "Manuale & Ghiduri" to listOf(
            "manual", "ghid", "indrumar", "instructiuni", "lectii",
            "clasa a", "handbook", "tutorial", "guide", "ted "
        ),
        "Literatură" to listOf(
            "roman", "nuvel", "povestir", "aventur", "dragoste", "politist",
            "mister", "thriller", "science fiction", "fantasy", "clasic",
            "opere", "novel", "stories"
        )
    )

    fun faraDiacritice(s: String): String {
        val n = Normalizer.normalize(s, Normalizer.Form.NFD)
        return n.replace(Regex("\\p{Mn}+"), "")
            .replace('ș', 's').replace('ţ', 't').replace('ț', 't')
            .lowercase(Locale.ROOT)
    }

    fun detecteaza(titlu: String, autor: String, format: String): String {
        if (format.equals("CBZ", true) || format.equals("CBR", true)) return "Benzi desenate"
        // spatii la capete ca sa prindem si cuvinte scurte intregi (" god ", "ntr ")
        val t = " " + faraDiacritice("$titlu $autor").replace(Regex("[_\\-.]+"), " ") + " "
        for (pereche in cuvinte) {
            for (c in pereche.second) {
                if (t.contains(c)) return pereche.first
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
