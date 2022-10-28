package ink.duo3.xdnmb.network

import ink.duo3.xdnmb.data.entity.ForumGroup
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class XdApi {
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                useAlternativeNames = false
            })
        }
    }
    suspend fun getForumList(): List<ForumGroup> {
        return httpClient.get("https://www.nmbxd1.com/Api/getForumList").body()
    }
}