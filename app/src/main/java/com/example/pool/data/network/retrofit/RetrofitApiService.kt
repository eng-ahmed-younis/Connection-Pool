package com.example.pool.data.network.retrofit

import com.example.pool.domain.model.Post
import retrofit2.http.GET
import retrofit2.http.Query

interface RetrofitApiService {
    @GET("posts")
    suspend fun getPosts(): List<Post>

    @GET("posts")
    suspend fun getPostsWithCacheBuster(@Query("_") timestamp: Long = System.currentTimeMillis()): List<Post>
}
