package ink.duo3.xdnmb.shared.network

import ink.duo3.xdnmb.shared.data.entity.ForumGroup
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
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