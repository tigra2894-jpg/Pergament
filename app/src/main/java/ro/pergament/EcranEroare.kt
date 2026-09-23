package ro.pergament

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ro.pergament.ui.Aur
import ro.pergament.ui.AurStins
import ro.pergament.ui.LemnCald
import ro.pergament.ui.Noapte
import ro.pergament.ui.Pergam
import ro.pergament.ui.PergamentTheme
import ro.pergament.ui.PergamStins

class EcranEroare : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mesaj = intent.getStringExtra("mesaj")
            ?: "Ceva neprevăzut s-a întâmplat."

        setContent {
            PergamentTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Noapte) {
                    Continut(
                        mesaj = mesaj,
                        onRedeschide = {
                            val i = Intent(this, MainActivity::class.java)
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(i)
                            finish()
                        },
                        onTrimite = { text ->
                            try {
                                val i = Intent(Intent.ACTION_SEND)
                                i.type = "text/plain"
                                i.putExtra(Intent.EXTRA_SUBJECT, "Pergament - raport de eroare")
                                i.putExtra(Intent.EXTRA_TEXT, text)
                                startActivity(Intent.createChooser(i, "Trimite raportul"))
                            } catch (e: Exception) {
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun Continut(
    mesaj: String,
    onRedeschide: () -> Unit,
    onTrimite: (String) -> Unit
) {
    val ctx = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .background(Noapte)
            .systemBarsPadding()
            .padding(horizontal = 30.dp),
        verticalArrangement = Arrangement.Center,
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
                .width(64.dp)
                .height(1.dp)
                .background(Aur.copy(alpha = 0.8f))
        )

        Spacer(Modifier.height(36.dp))

        Text(
            mesaj,
            style = MaterialTheme.typography.titleLarge,
            color = Pergam,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "Cărțile, semnele și notițele tale sunt în siguranță.",
            style = MaterialTheme.typography.bodyMedium,
            color = PergamStins,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(40.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(2.dp))
                .background(Aur)
                .clickable { onRedeschide() }
                .padding(vertical = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Deschide biblioteca",
                color = Noapte,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(Modifier.height(12.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(2.dp))
                .background(LemnCald)
                .border(0.7.dp, AurStins.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                .clickable { onTrimite(Jurnal.citeste(ctx)) }
                .padding(vertical = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Trimite raportul erorii",
                color = Pergam,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(Modifier.height(18.dp))

        Text(
            "Raportul conține doar ce s-a stricat și ce telefon ai. " +
                    "Nu conține cărțile și nici textul lor.",
            style = MaterialTheme.typography.bodyMedium,
            color = PergamStins.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )
    }
}
