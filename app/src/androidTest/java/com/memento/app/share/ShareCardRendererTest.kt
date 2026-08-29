package com.memento.app.share

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShareCardRendererTest {
    @Test
    fun rendersExactStoryDimensions() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = ShareCardRenderer(context).render(
            ShareCardContent("Una obra", "2026", "Una reflexión personal"),
            "instrumented-test",
        )
        val bitmap = context.contentResolver.openInputStream(uri).use(BitmapFactory::decodeStream)

        assertEquals(1080, bitmap.width)
        assertEquals(1920, bitmap.height)
        bitmap.recycle()
    }
}
