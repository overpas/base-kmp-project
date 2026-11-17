package by.overpas.template.core.ui.test.gallery

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import java.util.concurrent.CompletableFuture

internal class PickImageActivity : ComponentActivity() {

    val pickedImage = CompletableFuture<Uri?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val picker = registerForActivityResult(PickVisualMedia()) { image ->
            pickedImage.complete(image)
            finish()
        }
        if (savedInstanceState == null) {
            picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        }
    }
}
