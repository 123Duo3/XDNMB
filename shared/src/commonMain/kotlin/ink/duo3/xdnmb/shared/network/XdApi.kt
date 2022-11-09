package ink.duo3.xdnmb.shared.network

import ink.duo3.xdnmb.shared.data.entity.ForumGroup
import ink.duo3.xdnmb.shared.data.entity.Thread
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.ConstantCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.get
import io.ktor.http.Cookie
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

class XdApi {
    private val xdUrl = "https://api.nmb.best/Api"

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
        install(HttpCookies) {
            storage = ConstantCookiesStorage(Cookie(
                name = "5noQQ4U",
                value = "%0A%C2%1Ed%1A%5D%7C%F9%A5%AC%C2%9DT%E3%DC%ED%850%F1I%D7%D8%1B%13",
                domain = "https://www.nmbxd1.com/" ))
        }
    }

    suspend fun getForumList(): List<ForumGroup> {
        return httpClient.get("$xdUrl/getForumList").body()
    }

    suspend fun getTimeLine(page: Int): List<Thread> {
        return httpClient.get("$xdUrl/timeline/$page").body()
    }

    suspend fun getTreadList(fid: Int, page: Int): List<Thread> {
        return httpClient.get("$xdUrl/showf?id=$fid&page=$page").body()
    }

    suspend fun getReply(threadId: Int, page: Int): Thread {
        return httpClient.get("$xdUrl/thread?id=$threadId&page=$page").body()
    }
}