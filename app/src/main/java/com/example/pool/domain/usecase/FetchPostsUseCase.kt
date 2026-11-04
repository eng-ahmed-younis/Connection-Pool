package com.example.pool.domain.usecase

import com.example.pool.domain.model.ClientEngine
import com.example.pool.domain.model.NetworkMetrics
import com.example.pool.domain.model.PoolingMode
import com.example.pool.domain.model.Post
import com.example.pool.domain.repository.NetworkRepository

class FetchPostsUseCase(
    private val repository: NetworkRepository
) {
    suspend operator fun invoke(
        engine: ClientEngine,
        poolingMode: PoolingMode
    ): Pair<Result<List<Post>>, NetworkMetrics> {
        return repository.fetchPosts(engine, poolingMode)
    }
}
