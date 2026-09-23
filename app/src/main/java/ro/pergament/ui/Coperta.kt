package ro.pergament.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import ro.pergament.data.Carte
import java.io.File

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
            // rama aurita fina peste coperta originala
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(3.dp)
                    .border(0.6.dp, Aur.copy(alpha = 0.22f), RoundedCornerShape(1.dp))
            )
        } else {
            CopertaPiele(carte)
        }

        // cotorul si lumina, la fel pentru toate cartile de pe raft
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to Color.Black.copy(alpha = 0.50f),
                    0.045f to Color.Black.copy(alpha = 0.16f),
                    0.07f to Color.White.copy(alpha = 0.07f),
                    0.12f to Color.Transparent,
                    0.90f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.30f)
                )
            )
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.08f),
                    0.25f to Color.Transparent,
                    0.80f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.20f)
                )
            )
        }
    }
}
