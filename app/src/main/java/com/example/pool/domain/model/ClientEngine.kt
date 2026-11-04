package com.example.pool.domain.model

enum class ClientEngine(
    val title: String,
    val subtitle: String
) {
    RETROFIT_OKHTTP(
        title = "Retrofit + OkHttp",
        subtitle = "Uses OkHttp ConnectionPool (default: 5 idle, 5 mins)"
    ),
    KTOR_CIO(
        title = "Ktor + CIO Engine",
        subtitle = "Uses Ktor's Coroutine-based I/O engine connection pool"
    ),
    KTOR_OKHTTP(
        title = "Ktor + OkHttp Engine",
        subtitle = "Delegates network pooling to OkHttp inside Ktor"
    )
}
