package com.tauri.quicksave

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : TauriActivity() {

    // JNI function implemented in Rust
    private external fun onFileSaved(filename: String, path: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Handle intent when app is created from cold start
        handleShareIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle intent when app is already running
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return

        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null) {
            val uri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
            uri?.let { saveFileAndFinish(it, type) }
        } else if (Intent.ACTION_SEND_MULTIPLE == action && type != null) {
            val uris: ArrayList<Uri>? = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
            uris?.let { saveMultipleFilesAndFinish(it, type) }
        }
    }

    private fun saveFileAndFinish(uri: Uri, mimeType: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val savedPath = saveFileInternal(uri, mimeType)
            withContext(Dispatchers.Main) {
                if (savedPath != null) {
                    Toast.makeText(
                        this@MainActivity,
                        "🌸 FlorSave: ¡Guardado en $savedPath!",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "❌ FlorSave: Error al guardar archivo",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                finish() // Cierra la pantalla y vuelve a WhatsApp al instante
            }
        }
    }

    private fun saveMultipleFilesAndFinish(uris: List<Uri>, mimeType: String) {
        CoroutineScope(Dispatchers.IO).launch {
            var count = 0
            var lastSavedPath = ""
            for (uri in uris) {
                val path = saveFileInternal(uri, mimeType)
                if (path != null) {
                    count++
                    lastSavedPath = path
                }
            }
            withContext(Dispatchers.Main) {
                if (count > 0) {
                    val msg = if (count == 1) {
                        "🌸 FlorSave: ¡Guardado en $lastSavedPath!"
                    } else {
                        "🌸 FlorSave: ¡$count archivos guardados con éxito!"
                    }
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "❌ FlorSave: Error al guardar los archivos",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                finish() // Cierra la pantalla y vuelve a WhatsApp al instante
            }
        }
    }

    private suspend fun saveFileInternal(uri: Uri, mimeType: String): String? {
        return try {
            val fileName = getFileName(uri) ?: "shared_file_${System.currentTimeMillis()}"

            // Determine folder based on mime type
            val relativePath = when {
                mimeType.startsWith("image/") -> Environment.DIRECTORY_PICTURES
                mimeType == "application/pdf" ||
                mimeType.contains("document") ||
                mimeType.contains("msword") ||
                mimeType.contains("pdf") -> Environment.DIRECTORY_DOCUMENTS
                else -> Environment.DIRECTORY_DOWNLOADS
            } + "/FlorSave"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            }

            // Choose the appropriate MediaStore collection
            val collection = when {
                mimeType.startsWith("image/") ->
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                mimeType.startsWith("video/") ->
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                mimeType.startsWith("audio/") ->
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else ->
                    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }

            val targetUri = contentResolver.insert(collection, contentValues)

            if (targetUri != null) {
                contentResolver.openInputStream(uri)?.use { input ->
                    contentResolver.openOutputStream(targetUri)?.use { output ->
                        input.copyTo(output)
                    }
                }

                val displayPath = "$relativePath/$fileName"
                Log.i("QuickSave", "File saved to $displayPath")

                // Notify Rust via JNI to emit Tauri event to WebView
                try {
                    onFileSaved(fileName, displayPath)
                } catch (e: Exception) {
                    Log.e("QuickSave", "Error calling JNI: ${e.message}")
                }
                displayPath
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("QuickSave", "Error saving file: ${e.message}")
            null
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }
}
