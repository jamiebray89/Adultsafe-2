package uk.co.skills4all.adultsafe

import android.os.Bundle
import java.io.File
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import uk.co.skills4all.adultsafe.ui.AdultSafeRoot
import uk.co.skills4all.adultsafe.ui.theme.AdultSafeTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Recover from a camera/process crash: delete abandoned private ID photo cache.
        runCatching {
            File(cacheDir, "adultsafe_capture").listFiles()?.forEach { image ->
                if (image.isFile && image.name.startsWith("evidence_")) image.delete()
            }
        }
        setContent {
            AdultSafeTheme {
                AdultSafeRoot()
            }
        }
    }
}
