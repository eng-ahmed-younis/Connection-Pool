package com.example.pool.data.network

import com.example.pool.data.network.retrofit.RetrofitApiService
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ConnectionPoolManager {

    private const val BASE_URL = "https://jsonplaceholder.typicode.com/"

    // 1. Shared ConnectionPool as recommended in Ahmed Aly's article
    val sharedConnectionPool: ConnectionPool by lazy {
        ConnectionPool(
            maxIdleConnections = 10,
            keepAliveDuration = 5,
            timeUnit = TimeUnit.MINUTES
        )
    }

    // 2. Shared Singleton OkHttpClient (Reuses connection pool across all Retrofit requests)
    val sharedOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionPool(sharedConnectionPool)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // 3. Shared Singleton Retrofit instance
    val sharedRetrofitService: RetrofitApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(sharedOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RetrofitApiService::class.java)
    }

    // 4. Shared Singleton Ktor CIO Client
    val sharedKtorCioClient: HttpClient by lazy {
        HttpClient(CIO) {
            engine {
                maxConnectionsCount = 1000
                endpoint {
                    maxConnectionsPerRoute = 20
                    keepAliveTime = 300_000 // 5 minutes in ms
                }
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }

    // 5. Shared Singleton Ktor OkHttp Client (uses shared OkHttpClient connection pool)
    val sharedKtorOkHttpClient: HttpClient by lazy {
        HttpClient(OkHttp) {
            engine {
                preconfigured = sharedOkHttpClient
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }

    /**
     * Creates a custom RetrofitApiService backed by a client with a custom listener to measure metrics.
     */
    fun createRetrofitServiceWithListener(
        listener: ConnectionMetricsEventListener,
        useSingletonPool: Boolean
    ): RetrofitApiService {
        val client = if (useSingletonPool) {
            sharedOkHttpClient.newBuilder()
                .eventListener(listener)
                .build()
        } else {
            // Anti-pattern: create brand new ConnectionPool and OkHttpClient per request!
            OkHttpClient.Builder()
                .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
                .eventListener(listener)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
        }

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RetrofitApiService::class.java)
    }

    /**
     * Creates an unpooled Ktor CIO HttpClient (Anti-pattern: new client per request).
     */
    fun createUnpooledKtorCioClient(): HttpClient {
        return HttpClient(CIO) {
            engine {
                maxConnectionsCount = 1
                endpoint {
                    maxConnectionsPerRoute = 1
                    keepAliveTime = 0 // Close immediately
                }
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }

    /**
     * Creates an unpooled Ktor OkHttp HttpClient (Anti-pattern: fresh OkHttpClient & Pool).
     */
    fun createUnpooledKtorOkHttpClient(listener: ConnectionMetricsEventListener? = null): HttpClient {
        val builder = OkHttpClient.Builder()
            .connectionPool(ConnectionPool(1, 1, TimeUnit.MILLISECONDS))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)

        if (listener != null) {
            builder.eventListener(listener)
        }

        return HttpClient(OkHttp) {
            engine {
                preconfigured = builder.build()
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }
}
