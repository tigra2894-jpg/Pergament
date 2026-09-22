package ro.pergament.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ro.pergament.data.Carte

@Composable
fun EcranSetari(
    carti: List<Carte>,
    lucreaza: Boolean,
    onSalveazaCopie: () -> Unit,
    onRestaureaza: () -> Unit,
    onInapoi: () -> Unit
) {
    val piele = texturaPiele()

    val terminate = carti.count { it.progres >= 0.99f }
    val incepute = carti.count { it.paginaCurenta > 0 && it.progres < 0.99f }
    val semne = carti.sumOf { it.semne.size }
    val notite = carti.sumOf { it.notite.size }
    val rafturi = carti.map { it.raft }.distinct().size

    Box(
        Modifier
            .fillMaxSize()
            .background(Noapte)
            .textura(piele)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Noapte.copy(alpha = 0.90f),
                        Noapte.copy(alpha = 0.80f),
                        Noapte.copy(alpha = 0.95f)
                    )
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 18.dp, top = 16.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.ArrowBack, "Înapoi", tint = Pergam,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .clickable { onInapoi() }
                        .padding(9.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Setări", style = MaterialTheme.typography.headlineMedium, color = Pergam)
            }

            Titlu("Biblioteca ta")

            Column(
                Modifier
                    .padding(horizontal = 18.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(3.dp))
                    .background(Noapte.copy(alpha = 0.55f))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Rand("Volume", "${carti.size}")
                Rand("Rafturi", "$rafturi")
                Rand("Terminate", "$terminate")
                Rand("În curs de citire", "$incepute")
                Rand("Semne de carte", "$semne")
                Rand("Notițe", "$notite")
            }

            Titlu("Copie de siguranță")

            Text(
                "Copia păstrează titlurile, rafturile, unde ai rămas cu cititul, semnele și notițele. " +
                        "Cărțile în sine rămân acolo unde sunt pe telefon, copia nu le mută.",
                color = PergamStins,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp)
            )

            Spacer(Modifier.height(14.dp))

            ButonMare(
                icon = Icons.Filled.Backup,
                titlu = "Salvează o copie",
                detaliu = "Alegi unde se scrie fișierul",
                activ = !lucreaza,
                onClick = onSalveazaCopie
            )

            Spacer(Modifier.height(10.dp))

            ButonMare(
                icon = Icons.Filled.Restore,
                titlu = "Restaurează dintr-o copie",
                detaliu = "Alegi fișierul salvat mai demult",
                activ = !lucreaza,
                onClick = onRestaureaza
            )

            Spacer(Modifier.height(16.dp))

            Text(
                "Sfat: după ce salvezi copia, trimite-ți fișierul pe mail sau pune-l pe Google Drive. " +
                        "Dacă pierzi telefonul, copia de pe telefon se pierde odată cu el.",
                color = PergamStins.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 22.dp)
            )

            Spacer(Modifier.height(30.dp))

            Text(
                text = "Pergament 1.1",
                color = AurStins.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 30.dp)
            )
        }

        if (lucreaza) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Noapte.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Aur, strokeWidth = 1.5.dp)
            }
        }
    }
}

@Composable
private fun Titlu(text: String) {
    Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 10.dp)) {
        Text(text, style = MaterialTheme.typography.titleLarge, color = Pergam)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .width(48.dp)
                .height(1.dp)
                .background(Aur.copy(alpha = 0.7f))
        )
    }
}

@Composable
private fun Rand(eticheta: String, valoare: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(eticheta, color = PergamStins, style = MaterialTheme.typography.bodyLarge)
        Text(valoare, color = Aur, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ButonMare(
    icon: ImageVector,
    titlu: String,
    detaliu: String,
    activ: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .padding(horizontal = 18.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(LemnCald.copy(alpha = if (activ) 1f else 0.4f))
            .border(0.7.dp, AurStins.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
            .clickable(enabled = activ) { onClick() }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (activ) Aur else PergamStins,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                titlu,
                color = if (activ) Pergam else PergamStins,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(2.dp))
            Text(detaliu, color = PergamStins, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
