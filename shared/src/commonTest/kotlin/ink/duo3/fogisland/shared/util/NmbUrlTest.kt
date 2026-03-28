package ink.duo3.fogisland.shared.util

import kotlin.test.Test
import kotlin.test.assertEquals

class NmbUrlTest {

    @Test
    fun buildsThumbAndFullUrlsFromImageIdAndExt() {
        assertEquals(
            "https://image.nmb.best/thumb/abc123.jpg",
            buildNmbThumbImageUrl(image = "abc123", ext = ".jpg")
        )
        assertEquals(
            "https://image.nmb.best/image/abc123.jpg",
            buildNmbFullImageUrl(image = "abc123", ext = "jpg")
        )
    }

    @Test
    fun mapsAbsoluteThumbToFullAndFullToThumb() {
        val thumbUrl = "https://image.nmb.best/thumb/abc123.jpg"
        val fullUrl = "https://image.nmb.best/image/abc123.jpg"

        assertEquals(fullUrl, buildNmbFullImageUrl(image = thumbUrl, ext = null))
        assertEquals(thumbUrl, buildNmbThumbImageUrl(image = fullUrl, ext = null))
    }

    @Test
    fun buildsWithCustomCdnBaseUrl() {
        assertEquals(
            "https://cdn.example.com/thumb/abc123.jpg",
            buildNmbThumbImageUrl(
                image = "abc123",
                ext = "jpg",
                cdnBaseUrl = "https://cdn.example.com/"
            )
        )
        assertEquals(
            "https://cdn.example.com/image/abc123.jpg",
            buildNmbFullImageUrl(
                image = "abc123",
                ext = ".jpg",
                cdnBaseUrl = "https://cdn.example.com"
            )
        )
    }
}
