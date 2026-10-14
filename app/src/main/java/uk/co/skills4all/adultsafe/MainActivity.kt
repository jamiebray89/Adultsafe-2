package uk.co.skills4all.adultsafe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import uk.co.skills4all.adultsafe.ui.AdultSafeRoot
import uk.co.skills4all.adultsafe.ui.theme.AdultSafeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AdultSafeTheme {
                AdultSafeRoot()
            }
        }
    }
}
