package com.example.pool.domain.repository

import com.example.pool.domain.model.BenchmarkSummary
import com.example.pool.domain.model.ClientEngine
import com.example.pool.domain.model.NetworkMetrics
import com.example.pool.domain.model.PoolingMode
import com.example.pool.domain.model.Post

interface NetworkRepository {
    suspend fun fetchPosts(
        engine: ClientEngine,
        poolingMode: PoolingMode,
        requestIndex: Int = 1
    ): Pair<Result<List<Post>>, NetworkMetrics>

    suspend fun runBenchmark(
        engine: ClientEngine,
        poolingMode: PoolingMode,
        requestCount: Int = 5,
        parallel: Boolean = false
    ): Pair<List<NetworkMetrics>, BenchmarkSummary>
}
