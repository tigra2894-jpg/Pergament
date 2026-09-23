package ro.pergament.reader

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale

data class VoceDisponibila(val id: String, val nume: String, val calitate: Int)

object Voce {

    private val RO = Locale("ro", "RO")

    /** Vocile romanesti instalate pe telefon, cele mai bune primele. */
    fun listeaza(tts: TextToSpeech?): List<VoceDisponibila> {
        val motor = tts ?: return emptyList()
        return try {
            val toate: Set<Voice> = motor.voices ?: return emptyList()
            toate
                .filter { it.locale.language == "ro" }
                .filter { !it.isNetworkConnectionRequired || it.features?.contains("notInstalled") != true }
                .sortedByDescending { it.quality }
                .map { v ->
                    VoceDisponibila(
                        id = v.name,
                        nume = numeFrumos(v),
                        calitate = v.quality
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Numele tehnic al vocii arata "ro-ro-x-jfa-local".
     * Scoatem din el ceva citibil.
     */
    private fun numeFrumos(v: Voice): String {
        val n = v.name.lowercase(Locale.ROOT)
        val retea = if (v.isNetworkConnectionRequired) " (de pe internet)" else ""
        val cod = n.substringAfter("-x-", "").substringBefore("-")
        val eticheta = when {
            cod.isBlank() -> "Voce românească"
            cod.startsWith("j") -> "Voce feminină"
            cod.startsWith("m") -> "Voce masculină"
            else -> "Voce $cod"
        }
        val calitate = when {
            v.quality >= Voice.QUALITY_VERY_HIGH -> ", foarte bună"
            v.quality >= Voice.QUALITY_HIGH -> ", bună"
            else -> ""
        }
        return eticheta + calitate + retea
    }

    fun aplica(tts: TextToSpeech?, idVoce: String, viteza: Float, ton: Float) {
        val motor = tts ?: return
        try {
            motor.setLanguage(RO)
            motor.setSpeechRate(viteza.coerceIn(0.5f, 1.8f))
            motor.setPitch(ton.coerceIn(0.6f, 1.5f))
            if (idVoce.isNotBlank()) {
                val v = motor.voices?.firstOrNull { it.name == idVoce }
                if (v != null) motor.setVoice(v)
            }
        } catch (e: Exception) {
        }
    }

    /**
     * Pregateste textul pentru citit: taie ce nu se rosteste
     * si pune pauze mai lungi acolo unde un om ar respira.
     */
    fun pentruCitit(text: String, pauzeLungi: Boolean): String {
        var s = text

        // numere de pagina si note de subsol singure pe rand
        s = s.replace(Regex("(?m)^\\s*\\d{1,4}\\s*$"), " ")
        // adrese de internet
        s = s.replace(Regex("(?i)\\bhttps?://\\S+"), " ")
        s = s.replace(Regex("(?i)\\bwww\\.\\S+"), " ")
        // linii de puncte din cuprins
        s = s.replace(Regex("\\.{4,}"), " ")
        // liniile de dialog citite ca "minus"
        s = s.replace(Regex("(?m)^[\\s]*[-–—]\\s*"), " ")

        // prescurtari romanesti, ca sa nu fie silabisite
        s = s.replace(Regex("(?i)\\bdl\\.\\s"), "domnul ")
        s = s.replace(Regex("(?i)\\bdna\\.\\s"), "doamna ")
        s = s.replace(Regex("(?i)\\bnr\\.\\s"), "numărul ")
        s = s.replace(Regex("(?i)\\bpag\\.\\s"), "pagina ")
        s = s.replace(Regex("(?i)\\bsec\\.\\s"), "secolul ")
        s = s.replace(Regex("(?i)\\betc\\."), "și așa mai departe")
        s = s.replace(Regex("(?i)\\bde ex\\."), "de exemplu")
        s = s.replace(Regex("(?i)\\bde exemplu:"), "de exemplu,")

        if (pauzeLungi) {
            // un om se opreste mai mult intre paragrafe decat intre fraze
            s = s.replace(Regex("\\n{2,}"), ".\n\n... ")
            // dupa doua puncte si punct si virgula, o respiratie scurta
            s = s.replace(": ", ": .. ")
            s = s.replace("; ", "; .. ")
        }

        s = s.replace(Regex("[ \\t]{2,}"), " ")
        return s.trim()
    }
}
