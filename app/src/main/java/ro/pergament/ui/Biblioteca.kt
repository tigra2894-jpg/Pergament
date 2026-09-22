package ro.pergament.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ro.pergament.data.Carte
import ro.pergament.data.Rafturi
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val MODURI = listOf("Rafturi", "Vitrină", "Recente", "Catalog")
private val SORTARI = listOf("Adăugare", "Titlu", "Autor", "Citit")

private val SPATIU_JOS = 150.dp
private val LATIME_CARTE = 108.dp
private val POZITIE_SCANDURA = 161.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EcranBiblioteca(
    carti: List<Carte>,
    seIncarca: Boolean,
    onDeschide: (Carte) -> Unit,
    onAdauga: () -> Unit,
    onSterge: (Carte) -> Unit,
    onFavorita: (Carte) -> Unit,
    onModifica: (Carte, String, String, String) -> Unit,
    onSetari: () -> Unit
) {
    var cautare by remember { mutableStateOf("") }
    var meniuPentru by remember { mutableStateOf<Carte?>(null) }
    var editeaza by remember { mutableStateOf<Carte?>(null) }
    var mod by remember { mutableIntStateOf(0) }
    var sortare by remember { mutableIntStateOf(0) }
    val piele = texturaPiele()

    val filtrate = remember(carti, cautare, sortare) {
        val baza = if (cautare.isBlank()) carti else {
            val q = Rafturi.faraDiacritice(cautare)
            carti.filter {
                Rafturi.faraDiacritice(it.titlu).contains(q) ||
                        Rafturi.faraDiacritice(it.autor).contains(q)
            }
        }
        when (sortare) {
            1 -> baza.sortedBy { Rafturi.faraDiacritice(it.titlu) }
            2 -> baza.sortedBy { Rafturi.faraDiacritice(it.autor).ifBlank { "zzz" } }
            3 -> baza.sortedByDescending { it.progres }
            else -> baza.sortedByDescending { it.adaugat }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Noapte)
            .textura(piele)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Noapte.copy(alpha = 0.90f),
                        Noapte.copy(alpha = 0.76f),
                        Noapte.copy(alpha = 0.82f),
                        Noapte.copy(alpha = 0.95f)
                    )
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 14.dp, top = 22.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
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
                        else "${carti.size} volume în bibliotecă",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PergamStins
                    )
                }
                Icon(
                    Icons.Filled.Settings, "Setări", tint = PergamStins,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .size(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { onSetari() }
                        .padding(10.dp)
                )
            }

            if (carti.isNotEmpty()) {
                BaraCautare(cautare) { cautare = it }
                Comutator(MODURI, mod, mic = false) { mod = it }
                Spacer(Modifier.height(6.dp))
                Comutator(SORTARI, sortare, mic = true) { sortare = it }
            }

            if (carti.isEmpty() && !seIncarca) {
                BibliotecaGoala()
            } else if (filtrate.isEmpty() && carti.isNotEmpty()) {
                Text(
                    "Nicio carte nu se potrivește cu „$cautare”.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PergamStins,
                    modifier = Modifier.padding(22.dp)
                )
            } else {
                when (mod) {
                    1 -> ModVitrina(filtrate, onDeschide) { meniuPentru = it }
                    2 -> ModCronologic(filtrate, onDeschide) { meniuPentru = it }
                    3 -> ModCatalog(filtrate, onDeschide) { meniuPentru = it }
                    else -> ModRafturi(filtrate, cautare.isBlank(), onDeschide) { meniuPentru = it }
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
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Noapte.copy(alpha = 0.92f), Noapte)
                    )
                )
                .systemBarsPadding()
                .padding(top = 28.dp, bottom = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.verticalGradient(listOf(Aur, AurStins)))
                    .clickable { onAdauga() }
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint = Noapte,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Adaugă cărți", color = Noapte, style = MaterialTheme.typography.titleMedium)
            }
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
                    Text(
                        "Adăugată ${dataScurta(c.adaugat)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (c.notite.isNotEmpty())
                        Text("${c.notite.size} notițe", style = MaterialTheme.typography.bodyMedium)
                    if (c.semne.isNotEmpty())
                        Text(
                            "${c.semne.size} semne de carte",
                            style = MaterialTheme.typography.bodyMedium
                        )

                    Spacer(Modifier.height(18.dp))

                    RandMeniu("Modifică titlul, autorul, raftul", Aur) {
                        editeaza = c
                        meniuPentru = null
                    }
                    RandMeniu(
                        if (c.favorita) "Scoate din favorite" else "Pune la favorite",
                        Aur
                    ) {
                        onFavorita(c)
                        meniuPentru = null
                    }
                    RandMeniu("Scoate din bibliotecă", Vin) {
                        onSterge(c)
                        meniuPentru = null
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { meniuPentru = null }) {
                    Text("Închide", color = PergamStins)
                }
            }
        )
    }

    val e = editeaza
    if (e != null) {
        DialogEditare(
            carte = e,
            onSalveaza = { titlu, autor, raft ->
                onModifica(e, titlu, autor, raft)
                editeaza = null
            },
            onRenunta = { editeaza = null }
        )
    }
}

@Composable
private fun RandMeniu(text: String, culoare: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .clickable { onClick() }
            .padding(vertical = 13.dp)
    ) {
        Text(text, color = culoare, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun Comutator(
    optiuni: List<String>,
    ales: Int,
    mic: Boolean,
    onAlege: (Int) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(top = if (mic) 0.dp else 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        optiuni.forEachIndexed { i, text ->
            val activ = i == ales
            Box(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (activ) Aur.copy(alpha = 0.16f) else Color.Transparent)
                    .clickable { onAlege(i) }
                    .padding(vertical = if (mic) 6.dp else 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text,
                    color = if (activ) Aur else PergamStins,
                    style = if (mic) MaterialTheme.typography.bodyMedium
                    else MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

@Composable
private fun ModRafturi(
    carti: List<Carte>,
    arataSpeciale: Boolean,
    onDeschide: (Carte) -> Unit,
    onMeniu: (Carte) -> Unit
) {
    val grupate = remember(carti) {
        val m = carti.groupBy { it.raft }
        Rafturi.sorteaza(m.keys).map { it to (m[it] ?: emptyList()) }
    }
    val favorite = remember(carti) { carti.filter { it.favorita } }
    val inCurs = remember(carti) {
        carti.filter { it.paginaCurenta > 0 && it.progres < 0.99f }.take(6)
    }

    LazyColumn(contentPadding = PaddingValues(bottom = SPATIU_JOS)) {
        if (arataSpeciale && inCurs.isNotEmpty()) {
            item { Raft("Continuă lectura", inCurs, onDeschide, onMeniu) }
        }
        if (arataSpeciale && favorite.isNotEmpty()) {
            item { Raft("Favorite", favorite, onDeschide, onMeniu) }
        }
        items(grupate.size) { i ->
            Raft(grupate[i].first, grupate[i].second, onDeschide, onMeniu)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ModVitrina(
    carti: List<Carte>,
    onDeschide: (Carte) -> Unit,
    onMeniu: (Carte) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = SPATIU_JOS),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        items(carti) { carte ->
            Column {
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
                            Icons.Filled.Star, null, tint = Aur,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(13.dp)
                        )
                    }
                }
                BaraProgres(carte)
                Spacer(Modifier.height(6.dp))
                Text(
                    carte.titlu,
                    style = MaterialTheme.typography.titleMedium,
                    color = Pergam.copy(alpha = 0.92f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ModCronologic(
    carti: List<Carte>,
    onDeschide: (Carte) -> Unit,
    onMeniu: (Carte) -> Unit
) {
    val grupate = remember(carti) {
        carti.sortedByDescending { it.adaugat }.groupBy { perioada(it.adaugat) }
    }
    val chei = remember(grupate) { grupate.keys.toList() }

    LazyColumn(contentPadding = PaddingValues(bottom = SPATIU_JOS)) {
        items(chei.size) { i ->
            val cheie = chei[i]
            Raft(cheie, grupate[cheie] ?: emptyList(), onDeschide, onMeniu, arataNumar = false)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ModCatalog(
    carti: List<Carte>,
    onDeschide: (Carte) -> Unit,
    onMeniu: (Carte) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(top = 12.dp, bottom = SPATIU_JOS)) {
        items(carti) { carte ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onDeschide(carte) },
                        onLongClick = { onMeniu(carte) }
                    )
                    .padding(horizontal = 22.dp, vertical = 10.dp)
            ) {
                Coperta(
                    carte = carte,
                    modifier = Modifier
                        .width(52.dp)
                        .aspectRatio(0.66f)
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        carte.titlu,
                        style = MaterialTheme.typography.titleLarge,
                        color = Pergam,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (carte.autor.isNotBlank()) {
                        Text(
                            carte.autor,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PergamStins,
                            maxLines = 1
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        buildString {
                            append(carte.format)
                            append(", ")
                            append(carte.raft)
                            if (carte.progres > 0f) {
                                append(", citit ")
                                append("${(carte.progres * 100).toInt()}%")
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = AurStins
                    )
                }
                if (carte.favorita) {
                    Icon(
                        Icons.Filled.Star, null, tint = Aur,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(15.dp)
                    )
                }
            }
            Box(
                Modifier
                    .padding(horizontal = 22.dp)
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(AurStins.copy(alpha = 0.16f))
            )
        }
    }
}

@Composable
private fun BaraProgres(carte: Carte) {
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
}

@Composable
private fun BaraCautare(valoare: String, onSchimbare: (String) -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 22.dp, vertical = 14.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(Noapte.copy(alpha = 0.55f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Search, null, tint = PergamStins, modifier = Modifier.size(18.dp))
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
    onMeniu: (Carte) -> Unit,
    arataNumar: Boolean = true
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 22.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(nume, style = MaterialTheme.typography.headlineMedium, color = Pergam)
            if (arataNumar) {
                Text("${carti.size}", style = MaterialTheme.typography.bodyMedium, color = AurStins)
            }
        }
        Spacer(Modifier.height(12.dp))

        Box(Modifier.fillMaxWidth()) {
            ScanduraRaft(Modifier.padding(top = POZITIE_SCANDURA))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = 22.dp, end = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(carti) { carte ->
                    Column(modifier = Modifier.width(LATIME_CARTE)) {
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
                                    Icons.Filled.Star, null, tint = Aur,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(5.dp)
                                        .size(14.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        BaraProgres(carte)
                        Spacer(Modifier.height(6.dp))
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
        }
    }
}

@Composable
private fun BibliotecaGoala() {
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
    }
}

private fun perioada(t: Long): String {
    val acum = Calendar.getInstance()
    val c = Calendar.getInstance()
    c.timeInMillis = t
    if (c.get(Calendar.YEAR) == acum.get(Calendar.YEAR)) {
        val dif = acum.get(Calendar.DAY_OF_YEAR) - c.get(Calendar.DAY_OF_YEAR)
        if (dif == 0) return "Astăzi"
        if (dif == 1) return "Ieri"
        if (dif < 7) return "Săptămâna aceasta"
        if (dif < 31) return "Luna aceasta"
    }
    return SimpleDateFormat("LLLL yyyy", Locale("ro", "RO")).format(Date(t))
        .replaceFirstChar { it.uppercase() }
}

private fun dataScurta(t: Long): String =
    SimpleDateFormat("d MMMM yyyy", Locale("ro", "RO")).format(Date(t))
