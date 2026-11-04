package com.example.pool.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pool.domain.usecase.FetchPostsUseCase
import com.example.pool.domain.usecase.RunBenchmarkUseCase
import com.example.pool.presentation.mvi.NetworkUiEffect
import com.example.pool.presentation.mvi.NetworkUiIntent
import com.example.pool.presentation.mvi.NetworkUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val fetchPostsUseCase: FetchPostsUseCase,
    private val runBenchmarkUseCase: RunBenchmarkUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NetworkUiState())
    val uiState: StateFlow<NetworkUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<NetworkUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun onIntent(intent: NetworkUiIntent) {
        when (intent) {
            is NetworkUiIntent.SelectEngine -> {
                _uiState.update { it.copy(selectedEngine = intent.engine) }
                sendEffect(NetworkUiEffect.ShowToast("Switched to engine: ${intent.engine.title}"))
            }

            is NetworkUiIntent.SelectPoolingMode -> {
                _uiState.update { it.copy(selectedPoolingMode = intent.mode) }
                sendEffect(NetworkUiEffect.ShowToast("Switched pooling mode: ${intent.mode.label}"))
            }

            is NetworkUiIntent.ExecuteSingleRequest -> {
                executeSingleRequest()
            }

            is NetworkUiIntent.ExecuteBatchBenchmark -> {
                executeBatchBenchmark(count = intent.count, parallel = intent.parallel)
            }

            is NetworkUiIntent.ClearMetricsLog -> {
                _uiState.update {
                    it.copy(
                        metricsLog = emptyList(),
                        benchmarkSummary = null,
                        posts = emptyList(),
                        errorMessage = null
                    )
                }
                sendEffect(NetworkUiEffect.ShowToast("Metrics log cleared"))
            }

            is NetworkUiIntent.ToggleExplanationDialog -> {
                _uiState.update { it.copy(showExplanationDialog = intent.show) }
            }

            is NetworkUiIntent.UpdatePoolConfig -> {
                _uiState.update {
                    it.copy(
                        maxIdleConnections = intent.maxIdle,
                        keepAliveMinutes = intent.keepAliveMinutes
                    )
                }
                sendEffect(NetworkUiEffect.ShowToast("Pool config updated: Max ${intent.maxIdle} idle, ${intent.keepAliveMinutes}m keep-alive"))
            }
        }
    }

    private fun executeSingleRequest() {
        val currentState = _uiState.value
        if (currentState.isRequestInProgress) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRequestInProgress = true, errorMessage = null) }

            val (result, metrics) = fetchPostsUseCase(
                engine = currentState.selectedEngine,
                poolingMode = currentState.selectedPoolingMode
            )

            result.fold(
                onSuccess = { fetchedPosts ->
                    val updatedLog = listOf(metrics) + currentState.metricsLog
                    _uiState.update {
                        it.copy(
                            isRequestInProgress = false,
                            posts = fetchedPosts,
                            metricsLog = updatedLog
                        )
                    }
                    sendEffect(NetworkUiEffect.ShowToast("Request completed in ${metrics.durationMs}ms"))
                    sendEffect(NetworkUiEffect.ScrollToBottom)
                },
                onFailure = { error ->
                    val updatedLog = listOf(metrics) + currentState.metricsLog
                    _uiState.update {
                        it.copy(
                            isRequestInProgress = false,
                            metricsLog = updatedLog,
                            errorMessage = error.localizedMessage ?: "Failed to fetch posts"
                        )
                    }
                    sendEffect(NetworkUiEffect.ShowToast("Request failed: ${error.localizedMessage}"))
                }
            )
        }
    }

    private fun executeBatchBenchmark(count: Int, parallel: Boolean) {
        val currentState = _uiState.value
        if (currentState.isRequestInProgress) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRequestInProgress = true, errorMessage = null) }

            val modeName = if (parallel) "Parallel ($count)" else "Sequential ($count)"
            sendEffect(NetworkUiEffect.ShowToast("Running $modeName benchmark..."))

            val (newMetrics, summary) = runBenchmarkUseCase(
                engine = currentState.selectedEngine,
                poolingMode = currentState.selectedPoolingMode,
                requestCount = count,
                parallel = parallel
            )

            val updatedLog = newMetrics + currentState.metricsLog
            _uiState.update {
                it.copy(
                    isRequestInProgress = false,
                    metricsLog = updatedLog,
                    benchmarkSummary = summary
                )
            }

            val speedupMsg = if (summary.reusedConnectionsCount > 0) {
                "Benchmark done! Reused ${summary.reusedConnectionsCount} connections. Avg latency: ${summary.avgDurationMs}ms"
            } else {
                "Benchmark done! All requests opened new sockets. Avg latency: ${summary.avgDurationMs}ms"
            }
            sendEffect(NetworkUiEffect.ShowToast(speedupMsg))
            sendEffect(NetworkUiEffect.ScrollToBottom)
        }
    }

    private fun sendEffect(effect: NetworkUiEffect) {
        viewModelScope.launch {
            _uiEffect.send(effect)
        }
    }
}
