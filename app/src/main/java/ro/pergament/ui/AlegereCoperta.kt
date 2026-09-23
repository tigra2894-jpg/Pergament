package ro.pergament.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ro.pergament.data.Carte
import ro.pergament.data.Coperti

@Composable
fun EcranAlegereCoperta(
    carte: Carte,
    onPiele: () -> Unit,
    onPagina: (Int) -> Unit,
    onGalerie: () -> Unit,
    onInapoi: () -> Unit
) {
    val ctx = LocalContext.current
    val piele = texturaPiele()
    var previzualizari by remember { mutableStateOf<List<Pair<Int, ImageBitmap>>>(emptyList()) }
    var incarca by remember { mutableStateOf(carte.format == "PDF") }

    LaunchedEffect(carte.id) {
        if (carte.format != "PDF") {
            incarca = false
            return@LaunchedEffect
        }
        val lista = withContext(Dispatchers.IO) {
            val total = Coperti.nrPaginiPdf(ctx, carte.uri)
            val cate = minOf(total, 24)
            val out = mutableListOf<Pair<Int, ImageBitmap>>()
            for (p in 0 until cate) {
                val b: Bitmap? = Coperti.previzualizarePdf(ctx, carte.uri, p, 260)
                if (b != null) out.add(p to b.asImageBitmap())
            }
            out
        }
        previzualizari = lista
        incarca = false
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
                        Noapte.copy(alpha = 0.82f),
                        Noapte.copy(alpha = 0.95f)
                    )
                )
            )
    ) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 18.dp, top = 14.dp, bottom = 8.dp),
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
                Column(Modifier.weight(1f)) {
                    Text(
                        "Alege coperta",
                        style = MaterialTheme.typography.titleLarge,
                        color = Pergam
                    )
                    Text(
                        carte.titlu,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PergamStins,
                        maxLines = 1
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Optiune(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.MenuBook,
                    text = "Legătură de piele",
                    onClick = onPiele
                )
                Optiune(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.PhotoLibrary,
                    text = "Poză din galerie",
                    onClick = onGalerie
                )
            }

            Box(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AurStins.copy(alpha = 0.25f))
            )

            when {
                carte.format != "PDF" -> {
                    Text(
                        "Paginile se pot alege doar la cărțile PDF. " +
                                "Pentru celelalte poți folosi legătura de piele sau o poză.",
                        color = PergamStins,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(22.dp)
                    )
                }
                incarca -> {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Aur, strokeWidth = 1.5.dp)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Pregătesc paginile",
                                color = PergamStins,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                previzualizari.isEmpty() -> {
                    Text(
                        "Nu am putut citi paginile acestei cărți.",
                        color = PergamStins,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(22.dp)
                    )
                }
                else -> {
                    Text(
                        "Sau ia o pagină din carte",
                        color = AurStins,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 18.dp, top = 16.dp, bottom = 10.dp)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp, bottom = 30.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(previzualizari) { (index, imagine) ->
                            Column {
                                Image(
                                    bitmap = imagine,
                                    contentDescription = "Pagina ${index + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.70f)
                                        .clip(RoundedCornerShape(2.dp))
                                        .border(
                                            0.7.dp,
                                            AurStins.copy(alpha = 0.4f),
                                            RoundedCornerShape(2.dp)
                                        )
                                        .clickable { onPagina(index) }
                                )
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    "Pagina ${index + 1}",
                                    color = PergamStins,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Optiune(
    modifier: Modifier,
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Column(
        modifier
            .clip(RoundedCornerShape(3.dp))
            .background(LemnCald)
            .border(0.7.dp, AurStins.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
            .clickable { onClick() }
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = Aur, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            text,
            color = Pergam,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
