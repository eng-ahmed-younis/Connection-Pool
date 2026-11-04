package com.example.pool.di

import com.example.pool.data.network.ktor.KtorNetworkClient
import com.example.pool.data.repository.NetworkRepositoryImpl
import com.example.pool.domain.repository.NetworkRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideNetworkRepository(
        ktorNetworkClient: KtorNetworkClient
    ): NetworkRepository {
        return NetworkRepositoryImpl(ktorNetworkClient = ktorNetworkClient)
    }
}
