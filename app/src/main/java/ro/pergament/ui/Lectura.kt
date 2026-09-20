package ro.pergament.ui

import android.graphics.BitmapFactory
import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ro.pergament.data.Carte
import ro.pergament.data.Notita
import ro.pergament.data.Setari
import ro.pergament.reader.Cititor
import ro.pergament.reader.Continut
import java.util.Locale
import kotlin.math.abs

@Composable
fun EcranLectura(
    carte: Carte,
    setari: Setari,
    onSetari: (Setari) -> Unit,
    onProgres: (Int, Int) -> Unit,
    onSemn: (Int) -> Unit,
    onNotita: (Notita) -> Unit,
    onInapoi: () -> Unit
) {
    val ctx = LocalContext.current
    var continut by remember { mutableStateOf<Continut?>(null) }
    val caractere = remember(setari.marimeText) {
        (1500f * (18f / setari.marimeText) * (18f / setari.marimeText)).toInt()
    }

    LaunchedEffect(carte.id, caractere) {
        continut?.inchide()
        continut = null
        val c = withContext(Dispatchers.IO) { Cititor.incarca(ctx, carte, caractere) }
        continut = c
    }

    DisposableEffect(carte.id) {
        onDispose { continut?.inchide() }
    }

    val tema = TemeLectura[setari.tema.coerceIn(0, TemeLectura.size - 1)]

    Box(
        Modifier
            .fillMaxSize()
            .background(tema.hartie)
    ) {
        val c = continut
        if (c == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = tema.accent, strokeWidth = 1.5.dp)
            }
        } else if (c is Continut.Eroare) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(36.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    c.mesaj,
                    color = tema.cerneala,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(20.dp))
                TextButton(onClick = onInapoi) {
                    Text("Înapoi la bibliotecă", color = tema.accent)
                }
            }
        } else {
            PaginiCarte(
                carte, c, setari, tema, onSetari,
                onProgres, onSemn, onNotita, onInapoi
            )
        }
    }
}

@Composable
private fun PaginiCarte(
    carte: Carte,
    continut: Continut,
    setari: Setari,
    tema: TemaLectura,
    onSetari: (Setari) -> Unit,
    onProgres: (Int, Int) -> Unit,
    onSemn: (Int) -> Unit,
    onNotita: (Notita) -> Unit,
    onInapoi: () -> Unit
) {
    val ctx = LocalContext.current
    val scop = rememberCoroutineScope()
    val total = continut.nrPaginiTotal
    val estePdf = continut is Continut.Pdf
    val esteText = continut is Continut.Litera

    val pagerState = rememberPagerState(
        initialPage = carte.paginaCurenta.coerceIn(0, (total - 1).coerceAtLeast(0))
    ) { total }

    var bareVizibile by remember { mutableStateOf(false) }
    var panouSetari by remember { mutableStateOf(false) }
    var dialogNotita by remember { mutableStateOf(false) }
    var textNotita by remember { mutableStateOf("") }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var citesteCuVoce by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val motor = TextToSpeech(ctx) { }
        motor.setLanguage(Locale("ro", "RO"))
        tts = motor
        onDispose {
            try {
                motor.stop()
                motor.shutdown()
            } catch (e: Exception) {
            }
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        onProgres(pagerState.currentPage, total)
        if (citesteCuVoce && continut is Continut.Litera) {
            val t = continut.pagini.getOrNull(pagerState.currentPage) ?: ""
            tts?.speak(t, TextToSpeech.QUEUE_FLUSH, null, "pag")
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = true
    ) { pagina ->
        val deplasare = (pagerState.currentPage - pagina) + pagerState.currentPageOffsetFraction
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (setari.intoarcereCurl) {
                        cameraDistance = 14f * density
                        rotationY = deplasare * 34f
                        transformOrigin = TransformOrigin(if (deplasare < 0f) 1f else 0f, 0.5f)
                        alpha = (1f - abs(deplasare) * 0.35f).coerceIn(0f, 1f)
                    } else {
                        val s = 1f - abs(deplasare) * 0.07f
                        scaleX = s
                        scaleY = s
                        alpha = (1f - abs(deplasare) * 0.5f).coerceIn(0f, 1f)
                    }
                }
                .background(if (estePdf) Color(0xFF101010) else tema.hartie)
        ) {
            when (continut) {
                is Continut.Litera -> Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(total) {
                            detectTapGestures { p ->
                                val treime = size.width / 3f
                                when {
                                    p.x < treime -> scop.launch {
                                        if (pagerState.currentPage > 0)
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                    p.x > treime * 2 -> scop.launch {
                                        if (pagerState.currentPage < total - 1)
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                    else -> bareVizibile = !bareVizibile
                                }
                            }
                        }
                ) {
                    PaginaText(
                        continut.pagini.getOrNull(pagina) ?: "", setari, tema, pagina, total
                    )
                }

                is Continut.Pdf -> PaginaPdf(
                    continut = continut,
                    index = pagina,
                    taieMargini = setari.taiePdf,
                    onPaginaInapoi = {
                        scop.launch {
                            if (pagerState.currentPage > 0)
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    onPaginaInainte = {
                        scop.launch {
                            if (pagerState.currentPage < total - 1)
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    onBare = { bareVizibile = !bareVizibile }
                )

                is Continut.Imagini -> PaginaImagine(
                    cale = continut.cai.getOrNull(pagina),
                    onPaginaInapoi = {
                        scop.launch {
                            if (pagerState.currentPage > 0)
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    onPaginaInainte = {
                        scop.launch {
                            if (pagerState.currentPage < total - 1)
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    onBare = { bareVizibile = !bareVizibile }
                )

                else -> {}
            }

            if (esteText) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                0f to Color.Black.copy(alpha = 0.06f),
                                0.05f to Color.Transparent,
                                0.95f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.06f)
                            )
                        )
                )
            }
        }
    }

    AnimatedVisibility(visible = bareVizibile, enter = fadeIn(), exit = fadeOut()) {
        Box(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(Noapte.copy(alpha = 0.93f))
                    .systemBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.ArrowBack, "Înapoi", tint = Pergam,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onInapoi() }
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    carte.titlu,
                    color = Pergam,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                val areSemn = carte.semne.contains(pagerState.currentPage)
                Icon(
                    if (areSemn) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    "Semn de carte",
                    tint = if (areSemn) Aur else Pergam,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onSemn(pagerState.currentPage) }
                )
            }

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Noapte.copy(alpha = 0.93f))
                    .systemBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Text(
                    "Pagina ${pagerState.currentPage + 1} din $total",
                    color = PergamStins,
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = pagerState.currentPage.toFloat(),
                    onValueChange = { v -> scop.launch { pagerState.scrollToPage(v.toInt()) } },
                    valueRange = 0f..(total - 1).coerceAtLeast(1).toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Aur,
                        activeTrackColor = Aur,
                        inactiveTrackColor = AurStins.copy(alpha = 0.3f)
                    )
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (esteText) {
                        Icon(
                            Icons.Filled.TextFields, "Reglaje text", tint = Pergam,
                            modifier = Modifier
                                .size(26.dp)
                                .clickable { panouSetari = !panouSetari }
                        )
                    }
                    if (estePdf) {
                        Icon(
                            Icons.Filled.CropFree,
                            "Taie marginile albe",
                            tint = if (setari.taiePdf) Aur else Pergam,
                            modifier = Modifier
                                .size(26.dp)
                                .clickable { onSetari(setari.copy(taiePdf = !setari.taiePdf)) }
                        )
                    }
                    Icon(
                        Icons.Filled.EditNote, "Notiță", tint = Pergam,
                        modifier = Modifier
                            .size(26.dp)
                            .clickable { dialogNotita = true }
                    )
                    if (continut is Continut.Litera) {
                        Icon(
                            if (citesteCuVoce) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            "Ascultă",
                            tint = if (citesteCuVoce) Aur else Pergam,
                            modifier = Modifier
                                .size(26.dp)
                                .clickable {
                                    citesteCuVoce = !citesteCuVoce
                                    if (citesteCuVoce) {
                                        val t = continut.pagini.getOrNull(pagerState.currentPage) ?: ""
                                        tts?.speak(t, TextToSpeech.QUEUE_FLUSH, null, "pag")
                                    } else tts?.stop()
                                }
                        )
                    }
                }

                if (estePdf) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (setari.taiePdf)
                            "Marginile albe sunt tăiate. Apropie două degete ca să mărești."
                        else
                            "Pagina întreagă. Apropie două degete ca să mărești.",
                        color = PergamStins,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (panouSetari && esteText) {
                    Spacer(Modifier.height(10.dp))
                    PanouSetari(setari, onSetari)
                }
            }
        }
    }

    if (dialogNotita) {
        AlertDialog(
            onDismissRequest = { dialogNotita = false },
            containerColor = Lemn,
            title = {
                Text(
                    "Notiță la pagina ${pagerState.currentPage + 1}",
                    color = Pergam,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(3.dp))
                        .background(LemnCald)
                        .padding(12.dp)
                ) {
                    if (textNotita.isEmpty())
                        Text("Scrie ce ai de reținut", color = PergamStins)
                    BasicTextField(
                        value = textNotita,
                        onValueChange = { textNotita = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Pergam),
                        cursorBrush = SolidColor(Aur),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (textNotita.isNotBlank()) {
                        onNotita(
                            Notita(
                                pagerState.currentPage,
                                textNotita.trim(),
                                System.currentTimeMillis()
                            )
                        )
                    }
                    textNotita = ""
                    dialogNotita = false
                }) { Text("Salvează", color = Aur) }
            },
            dismissButton = {
                TextButton(onClick = { dialogNotita = false }) {
                    Text("Renunță", color = PergamStins)
                }
            }
        )
    }
}

@Composable
private fun PaginaText(
    text: String,
    setari: Setari,
    tema: TemaLectura,
    pagina: Int,
    total: Int
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = setari.margine.dp, vertical = 30.dp)
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = text,
                color = tema.cerneala,
                fontFamily = FontFamily.Serif,
                fontSize = setari.marimeText.sp,
                lineHeight = (setari.marimeText * setari.inaltimeRand).sp,
                textAlign = TextAlign.Justify
            )
        }
        Text(
            "${pagina + 1} / $total",
            color = tema.accent.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Serif,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PaginaPdf(
    continut: Continut.Pdf,
    index: Int,
    taieMargini: Boolean,
    onPaginaInapoi: () -> Unit,
    onPaginaInainte: () -> Unit,
    onBare: () -> Unit
) {
    val densitate = LocalDensity.current
    var imagine by remember(index, taieMargini) { mutableStateOf<ImageBitmap?>(null) }
    var marire by remember(index) { mutableFloatStateOf(1f) }
    var mutX by remember(index) { mutableFloatStateOf(0f) }
    var mutY by remember(index) { mutableFloatStateOf(0f) }
    var latimeEcran by remember { mutableFloatStateOf(1200f) }

    LaunchedEffect(index, taieMargini) {
        val px = (latimeEcran * 2.4f).toInt().coerceIn(1200, 3600)
        val b = withContext(Dispatchers.IO) {
            Cititor.randeazaPdf(continut, index, px, taieMargini)
        }
        imagine = b?.asImageBitmap()
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(index) {
                latimeEcran = size.width.toFloat()
                detectTransformGestures { _, pan, zoom, _ ->
                    val nou = (marire * zoom).coerceIn(1f, 5f)
                    marire = nou
                    if (nou > 1.02f) {
                        val limX = size.width * (nou - 1f) / 2f
                        val limY = size.height * (nou - 1f) / 2f
                        mutX = (mutX + pan.x).coerceIn(-limX, limX)
                        mutY = (mutY + pan.y).coerceIn(-limY, limY)
                    } else {
                        mutX = 0f
                        mutY = 0f
                    }
                }
            }
            .pointerInput(index) {
                detectTapGestures(
                    onDoubleTap = {
                        if (marire > 1.05f) {
                            marire = 1f; mutX = 0f; mutY = 0f
                        } else {
                            marire = 2.6f
                        }
                    },
                    onTap = { p ->
                        if (marire > 1.05f) {
                            onBare()
                        } else {
                            val treime = size.width / 3f
                            when {
                                p.x < treime -> onPaginaInapoi()
                                p.x > treime * 2 -> onPaginaInainte()
                                else -> onBare()
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val img = imagine
        if (img != null) {
            Image(
                bitmap = img,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = marire
                        scaleY = marire
                        translationX = mutX
                        translationY = mutY
                    }
            )
        } else {
            CircularProgressIndicator(color = AurStins, strokeWidth = 1.5.dp)
        }
    }
}

@Composable
private fun PaginaImagine(
    cale: String?,
    onPaginaInapoi: () -> Unit,
    onPaginaInainte: () -> Unit,
    onBare: () -> Unit
) {
    var imagine by remember(cale) { mutableStateOf<ImageBitmap?>(null) }
    var marire by remember(cale) { mutableFloatStateOf(1f) }
    var mutX by remember(cale) { mutableFloatStateOf(0f) }
    var mutY by remember(cale) { mutableFloatStateOf(0f) }

    LaunchedEffect(cale) {
        if (cale != null) {
            val b = withContext(Dispatchers.IO) {
                try {
                    BitmapFactory.decodeFile(cale)
                } catch (e: Exception) {
                    null
                }
            }
            imagine = b?.asImageBitmap()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(cale) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val nou = (marire * zoom).coerceIn(1f, 5f)
                    marire = nou
                    if (nou > 1.02f) {
                        val limX = size.width * (nou - 1f) / 2f
                        val limY = size.height * (nou - 1f) / 2f
                        mutX = (mutX + pan.x).coerceIn(-limX, limX)
                        mutY = (mutY + pan.y).coerceIn(-limY, limY)
                    } else {
                        mutX = 0f; mutY = 0f
                    }
                }
            }
            .pointerInput(cale) {
                detectTapGestures(
                    onDoubleTap = {
                        if (marire > 1.05f) {
                            marire = 1f; mutX = 0f; mutY = 0f
                        } else marire = 2.6f
                    },
                    onTap = { p ->
                        if (marire > 1.05f) onBare()
                        else {
                            val treime = size.width / 3f
                            when {
                                p.x < treime -> onPaginaInapoi()
                                p.x > treime * 2 -> onPaginaInainte()
                                else -> onBare()
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val img = imagine
        if (img != null) {
            Image(
                bitmap = img,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = marire
                        scaleY = marire
                        translationX = mutX
                        translationY = mutY
                    }
            )
        } else {
            CircularProgressIndicator(color = AurStins, strokeWidth = 1.5.dp)
        }
    }
}

@Composable
private fun PanouSetari(setari: Setari, onSetari: (Setari) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text("Mărimea literei", color = PergamStins, style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = setari.marimeText,
            onValueChange = { onSetari(setari.copy(marimeText = it)) },
            valueRange = 13f..34f,
            colors = SliderDefaults.colors(
                thumbColor = Aur,
                activeTrackColor = Aur,
                inactiveTrackColor = AurStins.copy(alpha = 0.3f)
            )
        )
        Text("Spațiul dintre rânduri", color = PergamStins, style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = setari.inaltimeRand,
            onValueChange = { onSetari(setari.copy(inaltimeRand = it)) },
            valueRange = 1.2f..2.4f,
            colors = SliderDefaults.colors(
                thumbColor = Aur,
                activeTrackColor = Aur,
                inactiveTrackColor = AurStins.copy(alpha = 0.3f)
            )
        )
        Text("Marginile paginii", color = PergamStins, style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = setari.margine,
            onValueChange = { onSetari(setari.copy(margine = it)) },
            valueRange = 8f..44f,
            colors = SliderDefaults.colors(
                thumbColor = Aur,
                activeTrackColor = Aur,
                inactiveTrackColor = AurStins.copy(alpha = 0.3f)
            )
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TemeLectura.forEachIndexed { i, t ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(t.hartie)
                        .clickable { onSetari(setari.copy(tema = i)) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(t.nume, color = t.cerneala, fontSize = 10.sp, fontFamily = FontFamily.Serif)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onSetari(setari.copy(intoarcereCurl = !setari.intoarcereCurl)) }
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Întoarcerea paginii", color = PergamStins, style = MaterialTheme.typography.bodyMedium)
            Text(
                if (setari.intoarcereCurl) "hârtie îndoită" else "alunecare",
                color = Aur,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
