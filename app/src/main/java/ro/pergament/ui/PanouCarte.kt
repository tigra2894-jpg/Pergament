package ro.pergament.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import ro.pergament.data.Carte
import ro.pergament.data.Notita
import ro.pergament.reader.Capitol
import ro.pergament.reader.Cuprins

data class Gasire(val pagina: Int, val context: String)

private val FILE = listOf("Cuprins", "Caută", "Semne", "Notițe")

@Composable
fun PanouCarte(
    carte: Carte,
    pagini: List<String>?,
    paginaCurenta: Int,
    filaInitiala: Int,
    onSari: (Int) -> Unit,
    onStergeSemn: (Int) -> Unit,
    onStergeNotita: (Notita) -> Unit,
    onInchide: () -> Unit
) {
    var fila by remember { mutableIntStateOf(filaInitiala) }
    var cautare by remember { mutableStateOf("") }
    var rezultate by remember { mutableStateOf<List<Gasire>?>(null) }
    var cauta by remember { mutableStateOf(false) }

    val capitole = remember(pagini) {
        if (pagini == null) emptyList() else Cuprins.gaseste(pagini)
    }

    LaunchedEffect(cautare, pagini) {
        val q = cautare.trim()
        if (q.length < 2 || pagini == null) {
            rezultate = null
            cauta = false
            return@LaunchedEffect
        }
        cauta = true
        delay(350)
        val gasite = withContext(Dispatchers.Default) {
            val cheie = Cuprins.faraDiacritice(q)
            val out = mutableListOf<Gasire>()
            for ((i, p) in pagini.withIndex()) {
                val j = Cuprins.faraDiacritice(p)
                var de = j.indexOf(cheie)
                var pePagina = 0
                while (de >= 0 && pePagina < 3 && out.size < 300) {
                    val start = (de - 45).coerceAtLeast(0)
                    val stop = (de + cheie.length + 55).coerceAtMost(p.length)
                    var ctx = p.substring(start, stop).replace("\n", " ").trim()
                    if (start > 0) ctx = "…$ctx"
                    if (stop < p.length) ctx = "$ctx…"
                    out.add(Gasire(i, ctx))
                    pePagina++
                    de = j.indexOf(cheie, de + cheie.length)
                }
                if (out.size >= 300) break
            }
            out
        }
        rezultate = gasite
        cauta = false
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .pointerInput(Unit) { detectTapGesturesSimplu { onInchide() } }
    ) {
        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.88f)
                .background(Lemn)
                .pointerInput(Unit) { detectTapGesturesSimplu { } }
                .systemBarsPadding()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 10.dp, top = 14.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    carte.titlu,
                    color = Pergam,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Filled.Close, "Închide", tint = PergamStins,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .clickable { onInchide() }
                        .padding(8.dp)
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                FILE.forEachIndexed { i, nume ->
                    val activ = i == fila
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (activ) Aur.copy(alpha = 0.16f) else Color.Transparent)
                            .clickable { fila = i }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            nume,
                            color = if (activ) Aur else PergamStins,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                    }
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AurStins.copy(alpha = 0.3f))
            )

            when (fila) {
                1 -> FilaCautare(cautare, { cautare = it }, rezultate, cauta, pagini != null, onSari)
                2 -> FilaSemne(carte, pagini, onSari, onStergeSemn)
                3 -> FilaNotite(carte, onSari, onStergeNotita)
                else -> FilaCuprins(capitole, paginaCurenta, pagini != null, onSari)
            }
        }
    }
}

@Composable
private fun FilaCuprins(
    capitole: List<Capitol>,
    paginaCurenta: Int,
    areText: Boolean,
    onSari: (Int) -> Unit
) {
    if (!areText) {
        Gol("Cuprinsul se poate face doar în modul text. Apasă „Text” în bara de jos.")
        return
    }
    if (capitole.isEmpty()) {
        Gol("Nu am găsit capitole în această carte.")
        return
    }

    val indexCurent = remember(capitole, paginaCurenta) {
        capitole.indexOfLast { it.pagina <= paginaCurenta }.coerceAtLeast(0)
    }
    val stare = rememberLazyListState()
    LaunchedEffect(indexCurent) {
        if (indexCurent > 2) stare.scrollToItem((indexCurent - 2).coerceAtLeast(0))
    }

    LazyColumn(
        state = stare,
        contentPadding = PaddingValues(bottom = 30.dp)
    ) {
        items(capitole.size) { i ->
            val c = capitole[i]
            val activ = i == indexCurent
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { onSari(c.pagina) }
                    .background(if (activ) Aur.copy(alpha = 0.08f) else Color.Transparent)
                    .padding(
                        start = if (c.nivel == 0) 18.dp else 36.dp,
                        end = 18.dp, top = 12.dp, bottom = 12.dp
                    )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        c.titlu,
                        color = if (activ) Aur else Pergam.copy(alpha = 0.92f),
                        fontFamily = FontFamily.Serif,
                        fontWeight = if (c.nivel == 0) FontWeight.Medium else FontWeight.Normal,
                        fontSize = if (c.nivel == 0) 16.sp else 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "${c.pagina + 1}",
                        color = AurStins,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Linie()
        }
    }
}

@Composable
private fun FilaCautare(
    valoare: String,
    onSchimbare: (String) -> Unit,
    rezultate: List<Gasire>?,
    cauta: Boolean,
    areText: Boolean,
    onSari: (Int) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(3.dp))
                .background(LemnCald)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, null, tint = PergamStins, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.fillMaxWidth()) {
                if (valoare.isEmpty()) {
                    Text(
                        "Scrie un cuvânt din carte",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PergamStins.copy(alpha = 0.7f)
                    )
                }
                BasicTextField(
                    value = valoare,
                    onValueChange = onSchimbare,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Pergam),
                    cursorBrush = SolidColor(Aur),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (!areText) {
            Gol("Căutarea merge doar în modul text. Apasă „Text” în bara de jos.")
            return
        }

        when {
            cauta -> Box(
                Modifier
                    .fillMaxWidth()
                    .padding(30.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Aur, strokeWidth = 1.5.dp)
            }
            rezultate == null -> Gol("Scrie cel puțin două litere.")
            rezultate.isEmpty() -> Gol("Nu am găsit „$valoare” în această carte.")
            else -> {
                Text(
                    if (rezultate.size >= 300) "Peste 300 de potriviri"
                    else "${rezultate.size} potriviri",
                    color = AurStins,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                )
                LazyColumn(contentPadding = PaddingValues(bottom = 30.dp)) {
                    items(rezultate) { g ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSari(g.pagina) }
                                .padding(horizontal = 18.dp, vertical = 12.dp)
                        ) {
                            Text(
                                "pagina ${g.pagina + 1}",
                                color = AurStins,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                g.context,
                                color = Pergam.copy(alpha = 0.9f),
                                fontFamily = FontFamily.Serif,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Linie()
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaSemne(
    carte: Carte,
    pagini: List<String>?,
    onSari: (Int) -> Unit,
    onSterge: (Int) -> Unit
) {
    if (carte.semne.isEmpty()) {
        Gol("Niciun semn de carte. Pune unul cu semnul din bara de sus.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 30.dp)) {
        items(carte.semne) { p ->
            val preview = pagini?.getOrNull(p)?.take(90)?.replace("\n", " ")?.trim()
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onSari(p) }
                    .padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Bookmark, null, tint = Aur, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Pagina ${p + 1}",
                        color = Pergam,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (!preview.isNullOrBlank()) {
                        Text(
                            "$preview…",
                            color = PergamStins,
                            fontFamily = FontFamily.Serif,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    Icons.Filled.Delete, "Șterge", tint = Vin,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onSterge(p) }
                        .padding(8.dp)
                )
            }
            Linie()
        }
    }
}

@Composable
private fun FilaNotite(
    carte: Carte,
    onSari: (Int) -> Unit,
    onSterge: (Notita) -> Unit
) {
    if (carte.notite.isEmpty()) {
        Gol("Nicio notiță. Scrie una cu butonul „Notiță” din bara de jos.")
        return
    }
    val sortate = remember(carte.notite) { carte.notite.sortedBy { it.pagina } }
    LazyColumn(contentPadding = PaddingValues(bottom = 30.dp)) {
        items(sortate) { n ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onSari(n.pagina) }
                    .padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 12.dp)
            ) {
                Icon(
                    Icons.Filled.EditNote, null, tint = Aur,
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .size(18.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Pagina ${n.pagina + 1}",
                        color = AurStins,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        n.text,
                        color = Pergam,
                        fontFamily = FontFamily.Serif,
                        fontSize = 15.sp,
                        lineHeight = 21.sp
                    )
                }
                Icon(
                    Icons.Filled.Delete, "Șterge", tint = Vin,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onSterge(n) }
                        .padding(8.dp)
                )
            }
            Linie()
        }
    }
}

@Composable
private fun Linie() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(AurStins.copy(alpha = 0.14f))
    )
}

@Composable
private fun Gol(mesaj: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 26.dp, vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            mesaj,
            color = PergamStins,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectTapGesturesSimplu(
    onTap: () -> Unit
) {
    androidx.compose.foundation.gestures.detectTapGestures { onTap() }
}
