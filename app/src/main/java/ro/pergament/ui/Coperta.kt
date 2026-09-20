package ro.pergament.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ro.pergament.data.Carte
import java.io.File
import kotlin.math.abs

private fun Color.umbra(f: Float) = Color(red * f, green * f, blue * f, alpha)

@Composable
fun Coperta(carte: Carte, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(
        topStart = 2.dp, topEnd = 5.dp, bottomEnd = 5.dp, bottomStart = 2.dp
    )

    val imagine = remember(carte.coperta) {
        val cale = carte.coperta
        if (cale != null && File(cale).exists()) {
            try {
                BitmapFactory.decodeFile(cale)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else null
    }

    Box(
        modifier = modifier
            .clip(forma)
            .background(Lemn)
            .border(0.7.dp, AurStins.copy(alpha = 0.45f), forma)
    ) {
        if (imagine != null) {
            Image(
                bitmap = imagine,
                contentDescription = carte.titlu,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CopertaGenerata(carte)
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to Color.Black.copy(alpha = 0.45f),
                    0.06f to Color.Black.copy(alpha = 0.10f),
                    0.10f to Color.Transparent,
                    0.92f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.25f)
                )
            )
        }
    }
}

@Composable
private fun CopertaGenerata(carte: Carte) {
    val baza = remember(carte.titlu) {
        CuloriCoperti[abs(carte.titlu.hashCode()) % CuloriCoperti.size]
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(baza, baza.umbra(0.62f), baza.umbra(0.85f))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 9.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier
                    .fillMaxWidth(0.34f)
                    .height(0.8.dp)
                    .background(Aur.copy(alpha = 0.75f))
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = carte.titlu,
                color = Pergam,
                textAlign = TextAlign.Center,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth(0.34f)
                    .height(0.8.dp)
                    .background(Aur.copy(alpha = 0.75f))
            )
            if (carte.autor.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = carte.autor,
                    color = Pergam.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = FontFamily.Serif,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            }
        }
        Text(
            text = carte.format,
            color = Pergam.copy(alpha = 0.35f),
            fontSize = 8.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 6.dp)
        )
    }
}
