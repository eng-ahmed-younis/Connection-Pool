package com.example.pool

import com.example.pool.data.repository.NetworkRepositoryImpl
import com.example.pool.domain.model.ClientEngine
import com.example.pool.domain.model.PoolingMode
import com.example.pool.domain.usecase.FetchPostsUseCase
import com.example.pool.domain.usecase.RunBenchmarkUseCase
import com.example.pool.presentation.NetworkViewModel
import com.example.pool.presentation.mvi.NetworkUiIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSelectEngineIntentUpdatesUiState() = runTest {
        val repository = NetworkRepositoryImpl()
        val viewModel = NetworkViewModel(
            fetchPostsUseCase = FetchPostsUseCase(repository),
            runBenchmarkUseCase = RunBenchmarkUseCase(repository)
        )

        assertEquals(ClientEngine.RETROFIT_OKHTTP, viewModel.uiState.value.selectedEngine)

        viewModel.onIntent(NetworkUiIntent.SelectEngine(ClientEngine.KTOR_CIO))

        assertEquals(ClientEngine.KTOR_CIO, viewModel.uiState.value.selectedEngine)
    }

    @Test
    fun testSelectPoolingModeIntentUpdatesUiState() = runTest {
        val repository = NetworkRepositoryImpl()
        val viewModel = NetworkViewModel(
            fetchPostsUseCase = FetchPostsUseCase(repository),
            runBenchmarkUseCase = RunBenchmarkUseCase(repository)
        )

        assertEquals(PoolingMode.POOLED_SINGLETON, viewModel.uiState.value.selectedPoolingMode)

        viewModel.onIntent(NetworkUiIntent.SelectPoolingMode(PoolingMode.UNPOOLED_NEW_INSTANCE))

        assertEquals(PoolingMode.UNPOOLED_NEW_INSTANCE, viewModel.uiState.value.selectedPoolingMode)
    }

    @Test
    fun testClearMetricsLogResetsState() = runTest {
        val repository = NetworkRepositoryImpl()
        val viewModel = NetworkViewModel(
            fetchPostsUseCase = FetchPostsUseCase(repository),
            runBenchmarkUseCase = RunBenchmarkUseCase(repository)
        )

        viewModel.onIntent(NetworkUiIntent.ClearMetricsLog)

        assertTrue(viewModel.uiState.value.metricsLog.isEmpty())
        assertEquals(null, viewModel.uiState.value.benchmarkSummary)
    }
}
