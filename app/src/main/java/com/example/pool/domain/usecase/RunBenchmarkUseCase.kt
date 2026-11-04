package com.example.pool.domain.usecase

import com.example.pool.domain.model.BenchmarkSummary
import com.example.pool.domain.model.ClientEngine
import com.example.pool.domain.model.NetworkMetrics
import com.example.pool.domain.model.PoolingMode
import com.example.pool.domain.repository.NetworkRepository

class RunBenchmarkUseCase(
    private val repository: NetworkRepository
) {
    suspend operator fun invoke(
        engine: ClientEngine,
        poolingMode: PoolingMode,
        requestCount: Int = 5,
        parallel: Boolean = false
    ): Pair<List<NetworkMetrics>, BenchmarkSummary> {
        return repository.runBenchmark(
            engine = engine,
            poolingMode = poolingMode,
            requestCount = requestCount,
            parallel = parallel
        )
    }
}
