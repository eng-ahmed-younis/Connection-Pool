package com.example.pool.presentation.mvi

import com.example.pool.domain.model.ClientEngine
import com.example.pool.domain.model.PoolingMode

sealed interface NetworkUiIntent {
    data class SelectEngine(val engine: ClientEngine) : NetworkUiIntent
    data class SelectPoolingMode(val mode: PoolingMode) : NetworkUiIntent
    data object ExecuteSingleRequest : NetworkUiIntent
    data class ExecuteBatchBenchmark(val count: Int = 5, val parallel: Boolean = false) : NetworkUiIntent
    data object ClearMetricsLog : NetworkUiIntent
    data class ToggleExplanationDialog(val show: Boolean) : NetworkUiIntent
    data class UpdatePoolConfig(val maxIdle: Int, val keepAliveMinutes: Long) : NetworkUiIntent
}
