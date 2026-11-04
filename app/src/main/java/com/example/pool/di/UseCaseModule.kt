package com.example.pool.di

import com.example.pool.domain.repository.NetworkRepository
import com.example.pool.domain.usecase.FetchPostsUseCase
import com.example.pool.domain.usecase.RunBenchmarkUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideFetchPostsUseCase(repository: NetworkRepository): FetchPostsUseCase {
        return FetchPostsUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideRunBenchmarkUseCase(repository: NetworkRepository): RunBenchmarkUseCase {
        return RunBenchmarkUseCase(repository)
    }
}
