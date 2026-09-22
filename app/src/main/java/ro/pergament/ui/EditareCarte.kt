package ro.pergament.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import ro.pergament.data.Carte
import ro.pergament.data.Rafturi

@Composable
fun DialogEditare(
    carte: Carte,
    onSalveaza: (String, String, String) -> Unit,
    onRenunta: () -> Unit
) {
    var titlu by remember { mutableStateOf(carte.titlu) }
    var autor by remember { mutableStateOf(carte.autor) }
    var raft by remember { mutableStateOf(carte.raft) }
    var alegeRaft by remember { mutableStateOf(false) }

    if (alegeRaft) {
        AlegereRaft(
            raftCurent = raft,
            onAles = { raft = it; alegeRaft = false },
            onRenunta = { alegeRaft = false }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onRenunta,
        containerColor = Lemn,
        title = {
            Text("Modifică datele cărții", color = Pergam, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column {
                Eticheta("Titlul")
                CampText(titlu, "Titlul cărții") { titlu = it }
                Spacer(Modifier.height(16.dp))

                Eticheta("Autorul")
                CampText(autor, "Lasă gol dacă nu știi") { autor = it }
                Spacer(Modifier.height(16.dp))

                Eticheta("Raftul")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(3.dp))
                        .background(LemnCald)
                        .clickable { alegeRaft = true }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(raft, color = Pergam, style = MaterialTheme.typography.bodyLarge)
                    Text("schimbă", color = Aur, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val t = titlu.trim().ifBlank { carte.titlu }
                onSalveaza(t, autor.trim(), raft)
            }) {
                Text("Salvează", color = Aur)
            }
        },
        dismissButton = {
            TextButton(onClick = onRenunta) {
                Text("Renunță", color = PergamStins)
            }
        }
    )
}

@Composable
private fun AlegereRaft(
    raftCurent: String,
    onAles: (String) -> Unit,
    onRenunta: () -> Unit
) {
    var nou by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onRenunta,
        containerColor = Lemn,
        title = {
            Text("Alege raftul", color = Pergam, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column {
                LazyColumn(
                    Modifier.heightIn(max = 300.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(Rafturi.ordine) { r ->
                        val activ = r == raftCurent
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (activ) Aur.copy(alpha = 0.14f) else Color.Transparent)
                                .clickable { onAles(r) }
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Text(
                                r,
                                color = if (activ) Aur else Pergam.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Eticheta("Sau scrie un raft nou")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        CampText(nou, "Nume raft nou") { nou = it }
                    }
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    TextButton(
                        onClick = { if (nou.trim().isNotBlank()) onAles(nou.trim()) },
                        enabled = nou.trim().isNotBlank()
                    ) {
                        Text("Pune", color = if (nou.trim().isNotBlank()) Aur else PergamStins)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onRenunta) {
                Text("Renunță", color = PergamStins)
            }
        }
    )
}

@Composable
private fun Eticheta(text: String) {
    Text(
        text,
        color = AurStins,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(bottom = 5.dp)
    )
}

@Composable
private fun CampText(valoare: String, gol: String, onSchimbare: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(LemnCald)
            .border(0.7.dp, AurStins.copy(alpha = 0.35f), RoundedCornerShape(3.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        if (valoare.isEmpty()) {
            Text(gol, color = PergamStins.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyLarge)
        }
        BasicTextField(
            value = valoare,
            onValueChange = onSchimbare,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Pergam),
            cursorBrush = SolidColor(Aur),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
