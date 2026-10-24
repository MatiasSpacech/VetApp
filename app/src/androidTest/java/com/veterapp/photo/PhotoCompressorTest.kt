package com.veterapp.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoCompressorTest {
  @Test
  fun compressesALargeNoisyPhotoToTheTarget() {
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val source = File(context.cacheDir, "photo-test.jpg")
      val bitmap = Bitmap.createBitmap(2_000, 1_500, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bitmap)
      val paint = Paint()
      repeat(900) {
        paint.color =
          Color.rgb(
            Random.nextInt(256),
            Random.nextInt(256),
            Random.nextInt(256),
          )
        canvas.drawRect(
          Random.nextInt(2_000).toFloat(),
          Random.nextInt(1_500).toFloat(),
          Random.nextInt(2_000).toFloat(),
          Random.nextInt(1_500).toFloat(),
          paint,
        )
      }
      FileOutputStream(source).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
      bitmap.recycle()

      val result = PhotoCompressor(context).compress(Uri.fromFile(source))

      assertTrue(result.width <= 800)
      assertTrue(result.height <= 800)
      assertTrue("La foto pesa ${result.bytes.size} bytes", result.bytes.size <= 100_000)
      assertTrue(result.mimeType == "image/webp" || result.mimeType == "image/jpeg")
      assertTrue(result.extension == "webp" || result.extension == "jpg")

      source.delete()
    }
  }

  @Test
  fun keepsSmallPhotoBelowEightHundred() {
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val source = File(context.cacheDir, "photo-small.png")
      val bitmap = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888)
      Canvas(bitmap).drawColor(Color.LTGRAY)
      FileOutputStream(source).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
      bitmap.recycle()

      val result = PhotoCompressor(context).compress(Uri.fromFile(source))

      assertEquals(320, result.width)
      assertEquals(240, result.height)
      assertTrue(result.bytes.isNotEmpty())

      source.delete()
    }
  }
}
