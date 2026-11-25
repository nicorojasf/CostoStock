package cl.costoStock.proveedor.nativeapi

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import cl.costoStock.proveedor.data.storage.PhotoStorage
import java.io.File

/**
 * Recurso nativo: cámara.
 *
 * Devuelve una función que, al invocarla, pide el permiso CAMERA en tiempo de ejecución (si
 * hace falta) y abre la cámara. La foto queda en un archivo temporal y se entrega por
 * [onPhotoTaken] (ruta del archivo). Maneja permiso rechazado y dispositivo sin cámara.
 */
@Composable
fun rememberCameraAction(
    onPhotoTaken: (String) -> Unit,
    onMessage: (String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val currentOnPhoto by rememberUpdatedState(onPhotoTaken)
    val currentOnMessage by rememberUpdatedState(onMessage)
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val path = pendingPath
        pendingPath = null
        if (path != null) {
            val file = File(path)
            if (success && file.exists() && file.length() > 0L) currentOnPhoto(path) else file.delete()
        }
    }

    fun launchCamera() {
        val file = PhotoStorage.newCaptureFile(context)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingPath = file.absolutePath
        try {
            takePictureLauncher.launch(uri)
        } catch (e: ActivityNotFoundException) {
            pendingPath = null
            currentOnMessage("No se encontró una aplicación de cámara en este dispositivo")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else currentOnMessage("Para tomar fotos debes permitir el acceso a la cámara")
    }

    return {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) launchCamera() else permissionLauncher.launch(Manifest.permission.CAMERA)
    }
}
