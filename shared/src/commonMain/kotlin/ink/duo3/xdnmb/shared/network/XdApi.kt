package ink.duo3.xdnmb.shared.network

import ink.duo3.xdnmb.shared.data.entity.ForumGroup
import ink.duo3.xdnmb.shared.data.entity.Thread
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.ConstantCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.cookies.cookies
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import io.ktor.http.*
import io.ktor.util.collections.*
import io.ktor.util.date.*
import kotlinx.coroutines.sync.*
import kotlin.math.*

class XdApi {
    private val xdUrl = "https://api.nmb.best/Api"
    private val cookiesStorage = AcceptAllCookiesStorage()

    @OptIn(ExperimentalSerializationApi::class)
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                useAlternativeNames = false
                explicitNulls = false
            })
        }
        install(ContentEncoding) {
            gzip()
        }
        install(UserAgent) {
            agent = "frogIslandApp"
        }
    }

    suspend fun getForumList(): List<ForumGroup> {
        return httpClient.get("$xdUrl/getForumList").body()
    }

    suspend fun getTimeLine(page: Int): List<Thread> {
        return httpClient.get("$xdUrl/timeline/$page").body()
    }

    suspend fun getTreadList(cookie: String?, fid: Int, page: Int): List<Thread> {
        return httpClient.get("$xdUrl/showf?id=$fid&page=$page"){
            header("Cookie", "userhash=$cookie;")
        }.body()
    }

    suspend fun getReply(cookie: String?, threadId: Int, page: Int): Thread {
        return httpClient.get("$xdUrl/thread?id=$threadId&page=$page"){
            header("Cookie", "userhash=$cookie;")
        }.body()
    }
}