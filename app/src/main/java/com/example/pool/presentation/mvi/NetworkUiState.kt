package com.example.pool.presentation.mvi

import com.example.pool.domain.model.BenchmarkSummary
import com.example.pool.domain.model.ClientEngine
import com.example.pool.domain.model.NetworkMetrics
import com.example.pool.domain.model.PoolingMode
import com.example.pool.domain.model.Post

data class NetworkUiState(
    val selectedEngine: ClientEngine = ClientEngine.RETROFIT_OKHTTP,
    val selectedPoolingMode: PoolingMode = PoolingMode.POOLED_SINGLETON,
    val isRequestInProgress: Boolean = false,
    val posts: List<Post> = emptyList(),
    val metricsLog: List<NetworkMetrics> = emptyList(),
    val benchmarkSummary: BenchmarkSummary? = null,
    val maxIdleConnections: Int = 10,
    val keepAliveMinutes: Long = 5,
    val errorMessage: String? = null,
    val showExplanationDialog: Boolean = false
)
