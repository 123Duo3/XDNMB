package ink.duo3.fogisland.shared.network.model

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ThreadDtoSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun feedResponseSupportsStringNumbers() {
        val payload = """
            [
              {
                "id":"50000002",
                "fid":"4",
                "img":"2022-06-18/62acedc59ef24",
                "ext":".png",
                "now":"2022-06-18(六)05:10:29",
                "user_hash":"Admin",
                "name":"",
                "title":"",
                "content":"test",
                "admin":"1"
              }
            ]
        """.trimIndent()

        val threads = json.decodeFromString(ListSerializer(ThreadDto.serializer()), payload)
        val thread = threads.single()

        assertEquals(50000002L, thread.id)
        assertEquals(4L, thread.forumId)
        assertEquals(1, thread.admin)
        assertNotNull(thread.postedAtRaw)
    }
}
