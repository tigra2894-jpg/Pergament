package ro.pergament.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ro.pergament.data.Carte
import ro.pergament.data.Rafturi

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EcranBiblioteca(
    carti: List<Carte>,
    seIncarca: Boolean,
    onDeschide: (Carte) -> Unit,
    onAdauga: () -> Unit,
    onSterge: (Carte) -> Unit,
    onFavorita: (Carte) -> Unit
) {
    var cautare by remember { mutableStateOf("") }
    var meniuPentru by remember { mutableStateOf<Carte?>(null) }

    val filtrate = remember(carti, cautare) {
        if (cautare.isBlank()) carti
        else {
            val q = Rafturi.faraDiacritice(cautare)
            carti.filter {
                Rafturi.faraDiacritice(it.titlu).contains(q) ||
                        Rafturi.faraDiacritice(it.autor).contains(q)
            }
        }
    }

    val grupate = remember(filtrate) {
        val m = filtrate.groupBy { it.raft }
        Rafturi.sorteaza(m.keys).map { it to (m[it] ?: emptyList()) }
    }

    val favorite = remember(filtrate) { filtrate.filter { it.favorita } }
    val inCurs = remember(filtrate) {
        filtrate.filter { it.paginaCurenta > 0 && it.progres < 0.99f }
            .sortedByDescending { it.adaugat }
            .take(6)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Noapte, Color(0xFF12100D), Noapte)))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 6.dp)) {
                    Text("Pergament", style = MaterialTheme.typography.displayLarge, color = Pergam)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .width(72.dp)
                            .height(1.dp)
                            .background(Aur.copy(alpha = 0.8f))
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (carti.isEmpty()) "Biblioteca ta așteaptă prima carte"
                        else "${carti.size} volume, pe ${grupate.size} rafturi",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PergamStins
                    )
                }
            }

            if (carti.isNotEmpty()) {
                item { BaraCautare(cautare) { cautare = it } }
            }

            if (inCurs.isNotEmpty() && cautare.isBlank()) {
                item { Raft("Continuă lectura", inCurs, onDeschide) { meniuPentru = it } }
            }

            if (favorite.isNotEmpty() && cautare.isBlank()) {
                item { Raft("Favorite", favorite, onDeschide) { meniuPentru = it } }
            }

            items(grupate.size) { idx ->
                val pereche = grupate[idx]
                Raft(pereche.first, pereche.second, onDeschide) { meniuPentru = it }
            }

            if (carti.isEmpty() && !seIncarca) {
                item { BibliotecaGoala(onAdauga) }
            }
            if (filtrate.isEmpty() && carti.isNotEmpty()) {
                item {
                    Text(
                        "Nicio carte nu se potrivește cu „$cautare”.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PergamStins,
                        modifier = Modifier.padding(22.dp)
                    )
                }
            }
        }

        if (seIncarca) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Noapte.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Aur, strokeWidth = 1.5.dp)
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Se așază cărțile pe rafturi",
                        color = Pergam,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .systemBarsPadding()
                .padding(end = 22.dp, bottom = 26.dp)
                .size(58.dp)
                .clip(RoundedCornerShape(29.dp))
                .background(Brush.verticalGradient(listOf(Aur, AurStins)))
                .combinedClickable(onClick = onAdauga),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Adaugă cărți", tint = Noapte)
        }
    }

    val c = meniuPentru
    if (c != null) {
        AlertDialog(
            onDismissRequest = { meniuPentru = null },
            containerColor = Lemn,
            titleContentColor = Pergam,
            textContentColor = PergamStins,
            title = { Text(c.titlu, style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    if (c.autor.isNotBlank())
                        Text(c.autor, style = MaterialTheme.typography.bodyMedium)
                    Text("${c.format}, raftul ${c.raft}", style = MaterialTheme.typography.bodyMedium)
                    if (c.totalPagini > 0)
                        Text(
                            "Pagina ${c.paginaCurenta + 1} din ${c.totalPagini}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    if (c.notite.isNotEmpty())
                        Text("${c.notite.size} notițe", style = MaterialTheme.typography.bodyMedium)
                    if (c.semne.isNotEmpty())
                        Text("${c.semne.size} semne de carte", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { onFavorita(c); meniuPentru = null }) {
                    Text(
                        if (c.favorita) "Scoate din favorite" else "Pune la favorite",
                        color = Aur
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onSterge(c); meniuPentru = null }) {
                    Text("Scoate din bibliotecă", color = Vin)
                }
            }
        )
    }
}

@Composable
private fun BaraCautare(valoare: String, onSchimbare: (String) -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 22.dp, vertical = 14.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(LemnCald)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = PergamStins,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Box(Modifier.fillMaxWidth()) {
            if (valoare.isEmpty()) {
                Text(
                    "Caută după titlu sau autor",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PergamStins.copy(alpha = 0.7f)
                )
            }
            BasicTextField(
                value = valoare,
                onValueChange = onSchimbare,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Pergam),
                cursorBrush = SolidColor(Aur),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Raft(
    nume: String,
    carti: List<Carte>,
    onDeschide: (Carte) -> Unit,
    onMeniu: (Carte) -> Unit
) {
    Column(Modifier.padding(top = 20.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(nume, style = MaterialTheme.typography.headlineMedium, color = Pergam)
            Text("${carti.size}", style = MaterialTheme.typography.bodyMedium, color = AurStins)
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(carti) { carte ->
                Column(modifier = Modifier.width(112.dp)) {
                    Box {
                        Coperta(
                            carte = carte,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.66f)
                                .combinedClickable(
                                    onClick = { onDeschide(carte) },
                                    onLongClick = { onMeniu(carte) }
                                )
                        )
                        if (carte.favorita) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                tint = Aur,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(5.dp)
                                    .size(14.dp)
                            )
                        }
                    }
                    if (carte.progres > 0f) {
                        Box(
                            Modifier
                                .padding(top = 6.dp)
                                .fillMaxWidth()
                                .height(1.5.dp)
                                .background(AurStins.copy(alpha = 0.22f))
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(carte.progres)
                                    .height(1.5.dp)
                                    .background(Aur)
                            )
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        carte.titlu,
                        style = MaterialTheme.typography.titleMedium,
                        color = Pergam.copy(alpha = 0.92f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (carte.autor.isNotBlank()) {
                        Text(
                            carte.autor,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PergamStins,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .padding(horizontal = 18.dp)
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, AurStins.copy(alpha = 0.55f), Color.Transparent)
                    )
                )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BibliotecaGoala(onAdauga: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp, vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Rafturile sunt goale", style = MaterialTheme.typography.headlineMedium, color = Pergam)
        Spacer(Modifier.height(12.dp))
        Text(
            "Adaugă fișiere PDF, EPUB, TXT sau benzi desenate CBZ. Pergament le citește titlul, le caută coperta și le așază singur pe raftul potrivit.",
            style = MaterialTheme.typography.bodyMedium,
            color = PergamStins
        )
        Spacer(Modifier.height(26.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(2.dp))
                .background(Aur)
                .combinedClickable(onClick = onAdauga)
                .padding(horizontal = 26.dp, vertical = 13.dp)
        ) {
            Text("Adaugă prima carte", color = Noapte, style = MaterialTheme.typography.titleMedium)
        }
    }
}
