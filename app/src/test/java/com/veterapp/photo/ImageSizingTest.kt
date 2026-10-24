package com.veterapp.photo

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageSizingTest {
  @Test
  fun keepsSmallImagesUntouched() {
    assertEquals(Dimensions(640, 480), ImageSizing.fitWithin(640, 480, 800))
  }

  @Test
  fun scalesLandscapeToEightHundred() {
    assertEquals(Dimensions(800, 600), ImageSizing.fitWithin(4000, 3000, 800))
  }

  @Test
  fun scalesPortraitToEightHundred() {
    assertEquals(Dimensions(600, 800), ImageSizing.fitWithin(3000, 4000, 800))
  }

  @Test
  fun scalesByFactor() {
    assertEquals(Dimensions(640, 480), ImageSizing.scaledDown(800, 600, 0.8))
  }
}
