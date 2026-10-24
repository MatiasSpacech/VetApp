package com.veterapp.photo

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object CameraFiles {
  fun create(context: Context): Uri {
    val directory = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File.createTempFile("foto_", ".jpg", directory)
    return FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      file,
    )
  }
}
