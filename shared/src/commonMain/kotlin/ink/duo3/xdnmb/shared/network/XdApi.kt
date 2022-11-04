package ink.duo3.xdnmb.shared.network

import ink.duo3.xdnmb.shared.data.entity.ForumGroup
import ink.duo3.xdnmb.shared.data.entity.Thread
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class XdApi {
    private val xdUrl = "https://www.nmbxd1.com/Api"

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
    }

    suspend fun getForumList(): List<ForumGroup> {
        return httpClient.get("$xdUrl/getForumList").body()
    }

    suspend fun getTimeLine(page: Int): List<Thread> {
        return httpClient.get("$xdUrl/timeline/$page").body()
    }

    suspend fun getTreadList(fid: String, page: Int): List<Thread> {
        return httpClient.get("$xdUrl/showf?id=$fid&page=$page").body()
    }
}