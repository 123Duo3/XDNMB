package ink.duo3.xdnmb.shared.network

import kotlinx.coroutines.runBlocking
import kotlin.test.Test

internal class XdApiTest {
    @Test
    fun testGetForumList() = runBlocking {
        XdApi().getForumList().forEach {
            println(it)
        }
    }
}