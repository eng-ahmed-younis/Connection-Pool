package com.example.pool.domain.model

enum class PoolingMode(
    val label: String,
    val description: String
) {
    POOLED_SINGLETON(
        label = "Pooled Singleton (Recommended)",
        description = "Reuses active TCP/TLS connections from a single shared pool."
    ),
    UNPOOLED_NEW_INSTANCE(
        label = "New Instance / Unpooled (Anti-pattern)",
        description = "Creates a fresh client & pool per request, triggering DNS/TCP/TLS handshakes every time."
    )
}
