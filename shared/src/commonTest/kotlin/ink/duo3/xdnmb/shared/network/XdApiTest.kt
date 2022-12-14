package ink.duo3.xdnmb.shared.network

import io.ktor.http.Cookie
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
        xdApi.getTreadList("",4, 1).forEach {
            println(it)
        }
    }

    @Test
    fun testGetReply() = runBlocking {
        xdApi.getReply("" ,53213787, 1).run {
            println(this)
        }
    }
}