package com.veterapp.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CompressedPhoto(
  val bytes: ByteArray,
  val mimeType: String,
  val extension: String,
  val width: Int,
  val height: Int,
)

class PhotoCompressor(private val context: Context) {
  suspend fun compress(uri: Uri): CompressedPhoto =
    withContext(Dispatchers.IO) {
      val resolver = context.contentResolver
      val bounds =
        BitmapFactory.Options().apply {
          inJustDecodeBounds = true
          resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, this) }
        }
      require(bounds.outWidth > 0 && bounds.outHeight > 0) { "No se pudo leer la imagen" }

      val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION)
      val decoded =
        BitmapFactory.Options().apply { inSampleSize = sampleSize }.let { options ->
          resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        } ?: error("No se pudo decodificar la imagen")

      val orientation = readOrientation(uri)
      val oriented = applyOrientation(decoded, orientation)
      val fitted =
        ImageSizing.fitWithin(oriented.width, oriented.height, MAX_DIMENSION).let { size ->
          if (size.width == oriented.width && size.height == oriented.height) {
            oriented
          } else {
            Bitmap.createScaledBitmap(oriented, size.width, size.height, true).also {
              if (it !== oriented) oriented.recycle()
            }
          }
        }

      try {
        encodeUntilSmallEnough(fitted)
      } finally {
        if (!fitted.isRecycled) fitted.recycle()
        if (decoded !== fitted && !decoded.isRecycled) decoded.recycle()
        if (oriented !== decoded && oriented !== fitted && !oriented.isRecycled) oriented.recycle()
      }
    }

  private fun encodeUntilSmallEnough(bitmap: Bitmap): CompressedPhoto {
    var current = bitmap
    var quality = INITIAL_QUALITY
    var best: Encoded? = null
    var attempts = 0

    try {
      while (attempts < MAX_ATTEMPTS) {
        attempts++
        val encoded = encode(current, quality)
        if (best == null || encoded.bytes.size < best.bytes.size) best = encoded

        if (encoded.bytes.size <= MAX_BYTES) {
          return encoded.toCompressedPhoto()
        }

        if (quality > MIN_QUALITY) {
          quality -= QUALITY_STEP
        } else {
          if (current.width <= MIN_DIMENSION || current.height <= MIN_DIMENSION) break
          val next =
            ImageSizing.scaledDown(current.width, current.height, SCALE_STEP).let { size ->
              Bitmap.createScaledBitmap(current, size.width, size.height, true)
            }
          if (current !== bitmap) current.recycle()
          current = next
          quality = INITIAL_QUALITY
        }
      }

      return (best ?: error("No se pudo comprimir la imagen")).toCompressedPhoto()
    } finally {
      if (current !== bitmap && !current.isRecycled) current.recycle()
    }
  }

  private fun encode(bitmap: Bitmap, quality: Int): Encoded {
    val webPBytes = ByteArrayOutputStream().use { output ->
      val format =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          Bitmap.CompressFormat.WEBP_LOSSY
        } else {
          @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
        }
      if (bitmap.compress(format, quality, output)) output.toByteArray() else null
    }
    if (webPBytes != null && webPBytes.isNotEmpty()) {
      return Encoded(
        bytes = webPBytes,
        mimeType = "image/webp",
        extension = "webp",
        width = bitmap.width,
        height = bitmap.height,
      )
    }

    val jpegBytes = ByteArrayOutputStream().use { output ->
      if (bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)) output.toByteArray() else null
    }
    require(jpegBytes != null && jpegBytes.isNotEmpty()) { "No se pudo codificar la imagen" }
    return Encoded(
      bytes = jpegBytes,
      mimeType = "image/jpeg",
      extension = "jpg",
      width = bitmap.width,
      height = bitmap.height,
    )
  }

  private fun readOrientation(uri: Uri): Int =
    runCatching {
        context.contentResolver.openInputStream(uri)?.use {
          ExifInterface(it).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
          )
        }
      }
      .getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

  private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
      ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
      ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
      ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
        matrix.setRotate(180f)
        matrix.postScale(-1f, 1f)
      }
      ExifInterface.ORIENTATION_TRANSPOSE -> {
        matrix.setRotate(90f)
        matrix.postScale(-1f, 1f)
      }
      ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
      ExifInterface.ORIENTATION_TRANSVERSE -> {
        matrix.setRotate(-90f)
        matrix.postScale(-1f, 1f)
      }
      ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
      else -> return bitmap
    }
    if (matrix.isIdentity) return bitmap
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
      if (it !== bitmap) bitmap.recycle()
    }
  }

  private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
    var sample = 1
    while (width / (sample * 2) >= maxDimension && height / (sample * 2) >= maxDimension) {
      sample *= 2
    }
    return sample
  }

  private data class Encoded(
    val bytes: ByteArray,
    val mimeType: String,
    val extension: String,
    val width: Int,
    val height: Int,
  ) {
    fun toCompressedPhoto(): CompressedPhoto =
      CompressedPhoto(
        bytes = bytes,
        mimeType = mimeType,
        extension = extension,
        width = width,
        height = height,
      )
  }

  private companion object {
    const val MAX_DIMENSION = 800
    const val MAX_BYTES = 100_000
    const val INITIAL_QUALITY = 80
    const val MIN_QUALITY = 45
    const val QUALITY_STEP = 5
    const val SCALE_STEP = 0.8
    const val MIN_DIMENSION = 240
    const val MAX_ATTEMPTS = 80
  }
}
