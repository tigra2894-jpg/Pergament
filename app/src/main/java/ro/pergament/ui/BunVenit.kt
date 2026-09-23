package ro.pergament.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private data class Pagina(
    val icon: ImageVector,
    val titlu: String,
    val text: String
)

private val PAGINI = listOf(
    Pagina(
        Icons.Filled.LibraryBooks,
        "Biblioteca ta, ca acasă",
        "Cărțile tale stau pe rafturi de lemn, fiecare la genul ei. " +
                "Cele fără copertă primesc legătură de piele, cu titlul presat în aur."
    ),
    Pagina(
        Icons.Filled.AutoStories,
        "Telefonul devine carte",
        "Apuci colțul paginii cu degetul și îl tragi. Pagina se îndoaie, " +
                "vezi umbra ei pe foaia de dedesubt, exact ca la o carte adevărată."
    ),
    Pagina(
        Icons.Filled.Headphones,
        "Citește sau ascultă",
        "PDF, EPUB, TXT și benzi desenate. Mărești litera cât vrei, " +
                "alegi hârtia, pui semne și notițe. Sau lași cartea să-ți citească ea."
    )
)

@Composable
fun EcranBunVenit(
    onGata: () -> Unit,
    onAdauga: () -> Unit
) {
    val piele = texturaPiele()
    val stare = rememberPagerState(pageCount = { PAGINI.size })
    val scop = rememberCoroutineScope()
    val ultima = stare.currentPage == PAGINI.size - 1

    Box(
        Modifier
            .fillMaxSize()
            .background(Noapte)
            .textura(piele)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Noapte.copy(alpha = 0.88f),
                        Noapte.copy(alpha = 0.74f),
                        Noapte.copy(alpha = 0.94f)
                    )
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 42.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Pergament",
                    style = MaterialTheme.typography.displayLarge,
                    color = Pergam
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .width(72.dp)
                        .height(1.dp)
                        .background(Aur.copy(alpha = 0.8f))
                )
            }

            HorizontalPager(
                state = stare,
                modifier = Modifier.weight(1f)
            ) { index ->
                val p = PAGINI[index]
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 34.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF3A2A1C), Color(0xFF241A11))
                                )
                            )
                            .border(0.8.dp, Aur.copy(alpha = 0.45f), RoundedCornerShape(3.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            p.icon,
                            contentDescription = null,
                            tint = Aur,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    Spacer(Modifier.height(34.dp))

                    Text(
                        p.titlu,
                        color = Pergam,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 23.sp,
                        lineHeight = 30.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        p.text,
                        color = PergamStins,
                        fontFamily = FontFamily.Serif,
                        fontSize = 16.sp,
                        lineHeight = 25.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 26.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in PAGINI.indices) {
                    val activ = i == stare.currentPage
                    val latime by animateFloatAsState(
                        targetValue = if (activ) 22f else 7f,
                        animationSpec = tween(250),
                        label = "punct"
                    )
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .width(latime.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (activ) Aur else AurStins.copy(alpha = 0.35f))
                    )
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp, vertical = 20.dp)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(2.dp))
                        .background(Brush.verticalGradient(listOf(Aur, AurStins)))
                        .clickable {
                            if (ultima) onAdauga()
                            else scop.launch { stare.animateScrollToPage(stare.currentPage + 1) }
                        }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (ultima) {
                            Icon(
                                Icons.Filled.MenuBook,
                                contentDescription = null,
                                tint = Noapte,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(Modifier.width(9.dp))
                        }
                        Text(
                            if (ultima) "Adaugă prima carte" else "Mai departe",
                            color = Noapte,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Box(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onGata() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (ultima) "Intru mai târziu" else "Sari peste",
                        color = PergamStins,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
