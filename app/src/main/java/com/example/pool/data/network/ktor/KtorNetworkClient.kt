package com.example.pool.data.network.ktor

import com.example.pool.data.network.ConnectionPoolManager
import com.example.pool.domain.model.Post
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class KtorNetworkClient {

    suspend fun fetchPosts(client: HttpClient): List<Post> {
        val url = "https://jsonplaceholder.typicode.com/posts"
        return client.get(url) {
            parameter("_", System.currentTimeMillis())
        }.body()
    }
}
