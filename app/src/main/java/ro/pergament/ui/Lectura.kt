package ro.pergament.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
import kotlin.math.roundToInt

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
    var pregatire by remember { mutableFloatStateOf(-1f) }
    val caractere = remember(setari.marimeText) {
        (1500f * (18f / setari.marimeText) * (18f / setari.marimeText)).toInt()
    }

    LaunchedEffect(carte.id, caractere, setari.pdfCaText) {
        continut?.inchide()
        continut = null
        pregatire = -1f
        val c = withContext(Dispatchers.IO) {
            Cititor.incarca(ctx, carte, caractere, setari.pdfCaText) { p -> pregatire = p }
        }
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
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(36.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (carte.format == "PDF" && setari.pdfCaText && pregatire in 0f..0.999f) {
                    Text(
                        "Pregătesc cartea pentru citit",
                        color = tema.cerneala,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(22.dp))
                    LinearProgressIndicator(
                        progress = { pregatire },
                        modifier = Modifier.width(220.dp),
                        color = tema.accent,
                        trackColor = tema.accent.copy(alpha = 0.2f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "${(pregatire * 100).toInt()}%",
                        color = tema.accent,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Se face doar prima dată. Data viitoare cartea se deschide imediat.",
                        color = tema.cerneala.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                } else {
                    CircularProgressIndicator(color = tema.accent, strokeWidth = 1.5.dp)
                }
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
                if (carte.format == "PDF" && setari.pdfCaText) {
                    TextButton(onClick = { onSetari(setari.copy(pdfCaText = false)) }) {
                        Text("Deschide pagina originală", color = tema.accent)
                    }
                }
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
    val esteScanat = continut is Continut.Pdf && continut.scanat
    val esteImagini = continut is Continut.Imagini
    val esteText = continut is Continut.Litera
    val estePdfText = continut is Continut.Litera && continut.dinPdf
    val prefs = remember { ctx.getSharedPreferences("pergament", Context.MODE_PRIVATE) }

    val stare = remember(continut) {
        val t = carte.totalPagini
        val p = carte.paginaCurenta
        val pornire = if (t > 1 && total > 1 && t != total)
            ((p.toFloat() / (t - 1)) * (total - 1)).roundToInt()
        else p
        StareRasfoire(pornire.coerceIn(0, (total - 1).coerceAtLeast(0)), total)
    }

    var bareVizibile by remember { mutableStateOf(false) }
    var panouSetari by remember { mutableStateOf(false) }
    var dialogNotita by remember { mutableStateOf(false) }
    var textNotita by remember { mutableStateOf("") }
    var marire by remember { mutableStateOf(false) }
    var ghid by remember { mutableStateOf(!prefs.getBoolean("ghid_vazut", false)) }

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

    LaunchedEffect(stare.pagina) {
        onProgres(stare.pagina, total)
        if (citesteCuVoce && continut is Continut.Litera) {
            val t = continut.pagini.getOrNull(stare.pagina) ?: ""
            tts?.speak(t, TextToSpeech.QUEUE_FLUSH, null, "pag")
        }
    }

    val culoareHartie = when {
        estePdf -> Color(0xFFFAF8F3)
        esteImagini -> Color(0xFF0E0E0E)
        else -> tema.hartie
    }
    val culoareSpate = when {
        estePdf -> Color(0xFFEAE4D6)
        esteImagini -> Color(0xFF2A2A2A)
        else -> tema.hartie
    }

    Box(Modifier.fillMaxSize()) {

        CarteRasfoita(
            stare = stare,
            culoareSpate = culoareSpate,
            onAtingere = { zona ->
                when (zona) {
                    -1 -> scop.launch { stare.inapoi() }
                    1 -> scop.launch { stare.inainte() }
                    else -> bareVizibile = !bareVizibile
                }
            },
            onDouaDegete = { if (estePdf || esteImagini) marire = true }
        ) { idx ->
            Box(
                Modifier
                    .fillMaxSize()
                    .background(culoareHartie)
            ) {
                when (continut) {
                    is Continut.Litera -> PaginaText(
                        continut.pagini.getOrNull(idx) ?: "", setari, tema, idx, total
                    )
                    is Continut.Pdf -> PaginaPdf(continut, idx, setari.taiePdf, 2.0f)
                    is Continut.Imagini -> PaginaImagine(continut.cai.getOrNull(idx))
                    else -> {}
                }
                if (esteText) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    0f to Color.Black.copy(alpha = 0.07f),
                                    0.05f to Color.Transparent,
                                    0.95f to Color.Transparent,
                                    1f to Color.Black.copy(alpha = 0.05f)
                                )
                            )
                    )
                }
            }
        }

        if (!bareVizibile && !marire && !ghid) {
            if (stare.pagina > 0) {
                Sageata(Modifier.align(Alignment.CenterStart), true) {
                    scop.launch { stare.inapoi() }
                }
            }
            if (stare.pagina < total - 1) {
                Sageata(Modifier.align(Alignment.CenterEnd), false) {
                    scop.launch { stare.inainte() }
                }
            }
        }

        AnimatedVisibility(visible = bareVizibile, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(Noapte.copy(alpha = 0.94f))
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
                    Icon(
                        Icons.Filled.HelpOutline, "Cum se folosește", tint = PergamStins,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable {
                                bareVizibile = false
                                ghid = true
                            }
                    )
                    Spacer(Modifier.width(16.dp))
                    val areSemn = carte.semne.contains(stare.pagina)
                    Icon(
                        if (areSemn) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        "Semn de carte",
                        tint = if (areSemn) Aur else Pergam,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onSemn(stare.pagina) }
                    )
                }

                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Noapte.copy(alpha = 0.94f))
                        .systemBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Text(
                        "Pagina ${stare.pagina + 1} din $total",
                        color = PergamStins,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = stare.pagina.toFloat(),
                        onValueChange = { v -> stare.sari(v.toInt()) },
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
                            ButonBara(Icons.Filled.TextFields, "Litere", panouSetari) {
                                panouSetari = !panouSetari
                            }
                        }
                        if (estePdfText) {
                            ButonBara(Icons.Filled.Image, "Original", false) {
                                onSetari(setari.copy(pdfCaText = false))
                            }
                        }
                        if (estePdf && !esteScanat) {
                            ButonBara(Icons.Filled.Article, "Text", false) {
                                onSetari(setari.copy(pdfCaText = true))
                            }
                        }
                        if (estePdf) {
                            ButonBara(Icons.Filled.CropFree, "Margini", setari.taiePdf) {
                                onSetari(setari.copy(taiePdf = !setari.taiePdf))
                            }
                        }
                        if (estePdf || esteImagini) {
                            ButonBara(Icons.Filled.ZoomIn, "Mărește", false) {
                                bareVizibile = false
                                marire = true
                            }
                        }
                        ButonBara(Icons.Filled.EditNote, "Notiță", false) {
                            dialogNotita = true
                        }
                        if (continut is Continut.Litera) {
                            ButonBara(
                                if (citesteCuVoce) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                if (citesteCuVoce) "Oprește" else "Ascultă",
                                citesteCuVoce
                            ) {
                                citesteCuVoce = !citesteCuVoce
                                if (citesteCuVoce) {
                                    val t = continut.pagini.getOrNull(stare.pagina) ?: ""
                                    tts?.speak(t, TextToSpeech.QUEUE_FLUSH, null, "pag")
                                } else tts?.stop()
                            }
                        }
                    }

                    if (esteScanat) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Carte scanată: paginile sunt fotografii, textul nu poate fi scos. Rămâne pagina originală.",
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

        if (marire) {
            ZoomPagina(continut, stare.pagina, setari.taiePdf) { marire = false }
        }

        if (ghid) {
            GhidLectura(estePdf || esteImagini) {
                prefs.edit().putBoolean("ghid_vazut", true).apply()
                ghid = false
            }
        }
    }

    if (dialogNotita) {
        AlertDialog(
            onDismissRequest = { dialogNotita = false },
            containerColor = Lemn,
            title = {
                Text(
                    "Notiță la pagina ${stare.pagina + 1}",
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
                        onNotita(Notita(stare.pagina, textNotita.trim(), System.currentTimeMillis()))
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
private fun ButonBara(icon: ImageVector, eticheta: String, activ: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(3.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, eticheta, tint = if (activ) Aur else Pergam, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(3.dp))
        Text(
            eticheta,
            color = if (activ) Aur else PergamStins,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun Sageata(modifier: Modifier, stanga: Boolean, onClick: () -> Unit) {
    Box(
        modifier
            .padding(horizontal = 6.dp)
            .size(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Noapte.copy(alpha = 0.30f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (stanga) Icons.Filled.ChevronLeft else Icons.Filled.ChevronRight,
            if (stanga) "Pagina înapoi" else "Pagina înainte",
            tint = Pergam.copy(alpha = 0.85f),
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun GhidLectura(cuMarire: Boolean, onGata: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.87f))
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(start = 8.dp, end = 8.dp, top = 40.dp, bottom = 250.dp)
        ) {
            ZonaGhid(Modifier.weight(0.3f), Icons.Filled.ChevronLeft, "Înapoi", "atinge\nmarginea\nstângă")
            ZonaGhid(Modifier.weight(0.4f), Icons.Filled.Menu, "Meniu", "atinge\nla mijloc")
            ZonaGhid(Modifier.weight(0.3f), Icons.Filled.ChevronRight, "Înainte", "atinge\nmarginea\ndreaptă")
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(horizontal = 28.dp, vertical = 28.dp)
        ) {
            Text(
                "Sau apucă colțul paginii cu degetul și trage-l spre stânga, exact ca la o carte.",
                color = Pergam,
                style = MaterialTheme.typography.titleMedium
            )
            if (cuMarire) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Două degete pe pagină o măresc. Dublu-tap sau X te scot din mărire.",
                    color = PergamStins,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(22.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(2.dp))
                    .background(Aur)
                    .clickable { onGata() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Am înțeles", color = Noapte, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ZonaGhid(modifier: Modifier, icon: ImageVector, titlu: String, detaliu: String) {
    Column(
        modifier
            .fillMaxHeight()
            .padding(4.dp)
            .clip(RoundedCornerShape(3.dp))
            .border(0.8.dp, AurStins.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
            .background(Aur.copy(alpha = 0.05f)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = Aur, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(10.dp))
        Text(titlu, color = Pergam, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(detaliu, color = PergamStins, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ZoomPagina(continut: Continut, index: Int, taie: Boolean, onInchide: () -> Unit) {
    var scara by remember { mutableFloatStateOf(2.2f) }
    var mx by remember { mutableFloatStateOf(0f) }
    var my by remember { mutableFloatStateOf(0f) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0B))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val nou = (scara * zoom).coerceIn(1f, 6f)
                    scara = nou
                    val limX = size.width * (nou - 1f) / 2f
                    val limY = size.height * (nou - 1f) / 2f
                    mx = (mx + pan.x).coerceIn(-limX, limX)
                    my = (my + pan.y).coerceIn(-limY, limY)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onInchide() })
            }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scara
                    scaleY = scara
                    translationX = mx
                    translationY = my
                }
        ) {
            when (continut) {
                is Continut.Pdf -> PaginaPdf(continut, index, taie, 3.0f)
                is Continut.Imagini -> PaginaImagine(continut.cai.getOrNull(index))
                else -> {}
            }
        }

        Box(
            Modifier
                .align(Alignment.TopEnd)
                .systemBarsPadding()
                .padding(14.dp)
                .size(46.dp)
                .clip(RoundedCornerShape(23.dp))
                .background(Noapte.copy(alpha = 0.85f))
                .clickable { onInchide() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Close, "Ieși din mărire", tint = Pergam)
        }

        Text(
            "Două degete: mărești sau micșorezi. Un deget: plimbi pagina.",
            color = Pergam.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Noapte.copy(alpha = 0.75f))
                .systemBarsPadding()
                .padding(12.dp)
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
            .systemBarsPadding()
            .padding(horizontal = setari.margine.dp, vertical = 22.dp)
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
private fun PaginaPdf(continut: Continut.Pdf, index: Int, taieMargini: Boolean, calitate: Float) {
    val cfg = LocalConfiguration.current
    val dens = LocalDensity.current.density
    var imagine by remember(index, taieMargini, calitate) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(index, taieMargini, calitate) {
        val px = (cfg.screenWidthDp * dens * calitate).toInt().coerceIn(900, 3600)
        val b = withContext(Dispatchers.IO) {
            Cititor.randeazaPdf(continut, index, px, taieMargini)
        }
        imagine = b?.asImageBitmap()
    }

    Box(
        Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        val img = imagine
        if (img != null) {
            Image(
                bitmap = img,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CircularProgressIndicator(color = AurStins, strokeWidth = 1.5.dp)
        }
    }
}

@Composable
private fun PaginaImagine(cale: String?) {
    var imagine by remember(cale) { mutableStateOf<ImageBitmap?>(null) }
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
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val img = imagine
        if (img != null) {
            Image(
                bitmap = img,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
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
                        .border(
                            if (setari.tema == i) 1.5.dp else 0.dp,
                            if (setari.tema == i) Aur else Color.Transparent,
                            RoundedCornerShape(2.dp)
                        )
                        .clickable { onSetari(setari.copy(tema = i)) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(t.nume, color = t.cerneala, fontSize = 10.sp, fontFamily = FontFamily.Serif)
                }
            }
        }
    }
}
