package com.veterapp.photo

import com.veterapp.data.remote.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import java.util.UUID

class PhotoRepository(private val clientProvider: SupabaseClientProvider) {
  suspend fun upload(clinicId: String, petId: String, photo: CompressedPhoto): String {
    val client = clientProvider.client ?: error("Supabase no está configurado")
    require(client.auth.currentUserOrNull() != null) { "Iniciá sesión para subir fotos" }

    val path = "$clinicId/$petId/${UUID.randomUUID()}.${photo.extension}"
    val bucket = client.storage.from(BUCKET)
    bucket.upload(path, photo.bytes) {
      upsert = false
      contentType =
        if (photo.mimeType == "image/webp") ContentType.Image.WEBP else ContentType.Image.JPEG
    }
    return bucket.publicUrl(path)
  }

  companion object {
    const val BUCKET = "mascotas"
  }
}
