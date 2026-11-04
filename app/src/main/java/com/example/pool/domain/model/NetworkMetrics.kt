package com.example.pool.domain.model

data class NetworkMetrics(
    val id: String,
    val requestIndex: Int,
    val engine: ClientEngine,
    val poolingMode: PoolingMode,
    val durationMs: Long,
    val dnsDurationMs: Long = 0,
    val connectDurationMs: Long = 0,
    val isConnectionReused: Boolean,
    val statusCode: Int = 200,
    val timestampMs: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)

data class BenchmarkSummary(
    val totalRequests: Int,
    val avgDurationMs: Long,
    val firstRequestDurationMs: Long,
    val subsequentAvgDurationMs: Long,
    val reusedConnectionsCount: Int,
    val newHandshakesCount: Int,
    val estimatedTimeSavedMs: Long,
    val engine: ClientEngine,
    val poolingMode: PoolingMode
)
