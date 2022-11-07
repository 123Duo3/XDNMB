package ink.duo3.xdnmb.shared.network

import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class XdApiTest {
    private lateinit var xdApi: XdApi

    @BeforeTest
    fun setup() {
        xdApi = XdApi()
    }

    @Test
    fun testGetTimeline() = runBlocking {
        xdApi.getTimeLine(1).forEach {
            println(it)
        }
    }

    @Test
    fun testGetForumList() = runBlocking {
        xdApi.getForumList().forEach {
            println(it)
        }
    }

    @Test
    fun testGetThreadList() = runBlocking {
        xdApi.getTreadList("14", 1).forEach {
            println(it)
        }
    }
}