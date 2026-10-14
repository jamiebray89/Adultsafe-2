package uk.co.skills4all.adultsafe.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.co.skills4all.adultsafe.R
import uk.co.skills4all.adultsafe.model.TileTone
import uk.co.skills4all.adultsafe.ui.theme.AdultSafeMuted
import uk.co.skills4all.adultsafe.ui.theme.AdultSafeNavy
import uk.co.skills4all.adultsafe.ui.theme.AdultSafeText

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    contentPadding: Int = 18,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(contentPadding.dp), content = content)
    }
}

@Composable
fun Logo(size: Int, @DrawableRes resId: Int = R.drawable.adultsafe_logo) {
    Image(
        painter = painterResource(resId),
        contentDescription = "AdultSafe logo",
        modifier = Modifier.size(size.dp)
    )
}

@Composable
fun BrandHero(title: String, subtitle: String) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFEAF6FF), Color(0xFFF2EAFE))))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Logo(size = 72)
            Column(Modifier.padding(start = 16.dp)) {
                Text(title, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = AdultSafeNavy)
                Text(subtitle, color = AdultSafeMuted, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun ToneStrip(tone: TileTone, modifier: Modifier = Modifier) {
    val colour = when (tone) {
        TileTone.Good -> Color(0xFF12BFA8)
        TileTone.Attention -> Color(0xFFF59E0B)
        TileTone.Warning -> Color(0xFFEA580C)
        TileTone.Danger -> Color(0xFFDC2626)
    }
    Spacer(modifier.height(6.dp).fillMaxWidth().background(colour))
}

@Composable
fun PrimaryAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = AdultSafeNavy)
    ) { Text(label) }
}

@Composable
fun SecondaryAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled) { Text(label) }
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null) {
    Text(title, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = AdultSafeText)
    if (!subtitle.isNullOrBlank()) {
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = AdultSafeMuted)
    }
}
