package ro.pergament

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ro.pergament.data.Biblioteca
import ro.pergament.data.Carte
import ro.pergament.data.Import
import ro.pergament.data.Setari
import ro.pergament.data.SetariStore
import ro.pergament.ui.EcranBiblioteca
import ro.pergament.ui.EcranLectura
import ro.pergament.ui.Noapte
import ro.pergament.ui.PergamentTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PergamentTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Noapte) {
                    Aplicatia()
                }
            }
        }
    }
}

@Composable
fun Aplicatia() {
    val ctx = LocalContext.current
    val scop = rememberCoroutineScope()

    var carti by remember { mutableStateOf<List<Carte>>(emptyList()) }
    var setari by remember { mutableStateOf(Setari()) }
    var deschisa by remember { mutableStateOf<String?>(null) }
    var seIncarca by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val c = withContext(Dispatchers.IO) { Biblioteca.incarca(ctx) }
        val s = withContext(Dispatchers.IO) { SetariStore.incarca(ctx) }
        carti = c
        setari = s
    }

    fun salveaza(lista: List<Carte>) {
        carti = lista
        scop.launch(Dispatchers.IO) { Biblioteca.salveaza(ctx, lista) }
    }

    val selector = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uriuri: List<Uri> ->
        if (uriuri.isNotEmpty()) {
            seIncarca = true
            scop.launch {
                val noi = withContext(Dispatchers.IO) {
                    uriuri.mapNotNull { u ->
                        try {
                            Import.adauga(ctx, u)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                val existente = carti.map { it.uri }.toSet()
                val filtrate = noi.filter { !existente.contains(it.uri) }
                salveaza(carti + filtrate)
                seIncarca = false
            }
        }
    }

    val carteDeschisa = carti.firstOrNull { it.id == deschisa }

    if (carteDeschisa == null) {
        EcranBiblioteca(
            carti = carti,
            seIncarca = seIncarca,
            onDeschide = { deschisa = it.id },
            onAdauga = {
                selector.launch(
                    arrayOf(
                        "application/pdf",
                        "application/epub+zip",
                        "text/plain",
                        "application/zip",
                        "application/x-cbz",
                        "application/octet-stream"
                    )
                )
            },
            onSterge = { c ->
                c.coperta?.let { cale ->
                    scop.launch(Dispatchers.IO) {
                        try {
                            File(cale).delete()
                        } catch (e: Exception) {
                        }
                    }
                }
                salveaza(carti.filter { it.id != c.id })
            },
            onFavorita = { c ->
                salveaza(carti.map { if (it.id == c.id) it.copy(favorita = !it.favorita) else it })
            }
        )
    } else {
        EcranLectura(
            carte = carteDeschisa,
            setari = setari,
            onSetari = { s ->
                setari = s
                scop.launch(Dispatchers.IO) { SetariStore.salveaza(ctx, s) }
            },
            onProgres = { pagina, total ->
                salveaza(carti.map {
                    if (it.id == carteDeschisa.id)
                        it.copy(paginaCurenta = pagina, totalPagini = total)
                    else it
                })
            },
            onSemn = { pagina ->
                salveaza(carti.map {
                    if (it.id == carteDeschisa.id) {
                        val s = if (it.semne.contains(pagina)) it.semne - pagina
                        else (it.semne + pagina).sorted()
                        it.copy(semne = s)
                    } else it
                })
            },
            onNotita = { n ->
                salveaza(carti.map {
                    if (it.id == carteDeschisa.id) it.copy(notite = it.notite + n) else it
                })
            },
            onInapoi = { deschisa = null }
        )
    }
}
