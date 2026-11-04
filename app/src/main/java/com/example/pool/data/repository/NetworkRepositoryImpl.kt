package com.example.pool.data.repository

import com.example.pool.data.network.ConnectionMetricsEventListener
import com.example.pool.data.network.ConnectionPoolManager
import com.example.pool.data.network.ktor.KtorNetworkClient
import com.example.pool.domain.model.BenchmarkSummary
import com.example.pool.domain.model.ClientEngine
import com.example.pool.domain.model.NetworkMetrics
import com.example.pool.domain.model.PoolingMode
import com.example.pool.domain.model.Post
import com.example.pool.domain.repository.NetworkRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.UUID

class NetworkRepositoryImpl(
    private val ktorNetworkClient: KtorNetworkClient = KtorNetworkClient()
) : NetworkRepository {

    override suspend fun fetchPosts(
        engine: ClientEngine,
        poolingMode: PoolingMode,
        requestIndex: Int
    ): Pair<Result<List<Post>>, NetworkMetrics> {
        val startTime = System.currentTimeMillis()
        val listener = ConnectionMetricsEventListener()

        return try {
            val posts: List<Post> = when (engine) {
                ClientEngine.RETROFIT_OKHTTP -> {
                    val isPooled = (poolingMode == PoolingMode.POOLED_SINGLETON)
                    val api = ConnectionPoolManager.createRetrofitServiceWithListener(
                        listener = listener,
                        useSingletonPool = isPooled
                    )
                    api.getPostsWithCacheBuster()
                }

                ClientEngine.KTOR_CIO -> {
                    val client = if (poolingMode == PoolingMode.POOLED_SINGLETON) {
                        ConnectionPoolManager.sharedKtorCioClient
                    } else {
                        ConnectionPoolManager.createUnpooledKtorCioClient()
                    }
                    try {
                        ktorNetworkClient.fetchPosts(client)
                    } finally {
                        if (poolingMode == PoolingMode.UNPOOLED_NEW_INSTANCE) {
                            client.close()
                        }
                    }
                }

                ClientEngine.KTOR_OKHTTP -> {
                    val isPooled = (poolingMode == PoolingMode.POOLED_SINGLETON)
                    val client = if (isPooled) {
                        ConnectionPoolManager.sharedKtorOkHttpClient
                    } else {
                        ConnectionPoolManager.createUnpooledKtorOkHttpClient(listener)
                    }
                    try {
                        ktorNetworkClient.fetchPosts(client)
                    } finally {
                        if (poolingMode == PoolingMode.UNPOOLED_NEW_INSTANCE) {
                            client.close()
                        }
                    }
                }
            }

            val duration = System.currentTimeMillis() - startTime
            val timing = listener.timing

            // Determine if connection was reused
            val isReused = when {
                engine == ClientEngine.RETROFIT_OKHTTP || engine == ClientEngine.KTOR_OKHTTP -> {
                    if (poolingMode == PoolingMode.POOLED_SINGLETON) {
                        // In pooled mode, if connectDurationMs is 0 or timing says reused, socket was reused
                        timing.isConnectionReused || (timing.connectDurationMs == 0L && requestIndex > 1)
                    } else {
                        false
                    }
                }
                engine == ClientEngine.KTOR_CIO -> {
                    // Ktor CIO reuses connection in pooled mode for requests after the first
                    (poolingMode == PoolingMode.POOLED_SINGLETON) && (requestIndex > 1)
                }
                else -> false
            }

            val metrics = NetworkMetrics(
                id = UUID.randomUUID().toString(),
                requestIndex = requestIndex,
                engine = engine,
                poolingMode = poolingMode,
                durationMs = duration,
                dnsDurationMs = timing.dnsDurationMs,
                connectDurationMs = timing.connectDurationMs,
                isConnectionReused = isReused,
                statusCode = 200,
                errorMessage = null
            )

            Pair(Result.success(posts), metrics)
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val metrics = NetworkMetrics(
                id = UUID.randomUUID().toString(),
                requestIndex = requestIndex,
                engine = engine,
                poolingMode = poolingMode,
                durationMs = duration,
                isConnectionReused = false,
                statusCode = 500,
                errorMessage = e.localizedMessage ?: "Network error"
            )
            Pair(Result.failure(e), metrics)
        }
    }

    override suspend fun runBenchmark(
        engine: ClientEngine,
        poolingMode: PoolingMode,
        requestCount: Int,
        parallel: Boolean
    ): Pair<List<NetworkMetrics>, BenchmarkSummary> {
        val metricsList = mutableListOf<NetworkMetrics>()

        if (parallel) {
            coroutineScope {
                val deferreds = (1..requestCount).map { index ->
                    async {
                        fetchPosts(engine, poolingMode, requestIndex = index)
                    }
                }
                deferreds.forEach { deferred ->
                    val (_, metrics) = deferred.await()
                    metricsList.add(metrics)
                }
            }
        } else {
            for (index in 1..requestCount) {
                val (_, metrics) = fetchPosts(engine, poolingMode, requestIndex = index)
                metricsList.add(metrics)
            }
        }

        metricsList.sortBy { it.requestIndex }

        val totalRequests = metricsList.size
        val avgDuration = if (totalRequests > 0) metricsList.map { it.durationMs }.average().toLong() else 0L
        val firstDuration = metricsList.firstOrNull()?.durationMs ?: 0L
        val subsequentList = if (metricsList.size > 1) metricsList.drop(1) else emptyList()
        val subsequentAvg = if (subsequentList.isNotEmpty()) subsequentList.map { it.durationMs }.average().toLong() else firstDuration

        val reusedCount = metricsList.count { it.isConnectionReused }
        val newHandshakesCount = totalRequests - reusedCount

        // Time saved estimation: difference between full handshake duration and pooled request duration multiplied by reused count
        val timeSavedPerReusedRequest = (firstDuration - subsequentAvg).coerceAtLeast(0L)
        val estimatedTimeSaved = timeSavedPerReusedRequest * reusedCount

        val summary = BenchmarkSummary(
            totalRequests = totalRequests,
            avgDurationMs = avgDuration,
            firstRequestDurationMs = firstDuration,
            subsequentAvgDurationMs = subsequentAvg,
            reusedConnectionsCount = reusedCount,
            newHandshakesCount = newHandshakesCount,
            estimatedTimeSavedMs = estimatedTimeSaved,
            engine = engine,
            poolingMode = poolingMode
        )

        return Pair(metricsList, summary)
    }
}
