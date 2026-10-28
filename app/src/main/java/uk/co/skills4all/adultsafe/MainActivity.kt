package uk.co.skills4all.adultsafe

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import uk.co.skills4all.adultsafe.ui.AdultSafeRoot
import uk.co.skills4all.adultsafe.ui.theme.AdultSafeTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Do not delete pending camera files during activity recreation.
        setContent {
            AdultSafeTheme {
                AdultSafeRoot()
            }
        }
    }
}
