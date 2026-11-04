# 🚀 HTTP Connection Pooling in Android

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20MVI-orange.svg)](https://developer.android.com/topic/architecture)
[![DI](https://img.shields.io/badge/DI-Hilt-purple.svg)](https://dagger.dev/hilt/)

A production-ready reference Android application demonstrating **HTTP Connection Pooling**, latency benchmarking, and connection socket reuse optimization across **Retrofit (OkHttp)** and **Ktor Client (CIO & OkHttp engines)**, architected with **Clean Architecture**, **MVI (Model-View-Intent)**, and **Hilt Dependency Injection**.

---

## ⚡ Why HTTP Connection Pooling?

In mobile development, network performance directly impacts user experience. A common pitfall is creating a new HTTP client or connection pool for every request, which introduces hundreds of milliseconds of unnecessary latency.

Every unpooled HTTP request must undergo:
1. 🌐 **DNS Lookup** (~50-100 ms)
2. 🤝 **TCP Handshake** (~50-100 ms)
3. 🔒 **TLS Handshake** (~100-200 ms)

**Total latency overhead before data exchange: ~200 – 400 ms per request!**

### 💡 The Solution: Connection Pooling
Connection pooling keeps active TCP/TLS sockets open in an **idle state** (e.g., for 5 minutes). Subsequent network calls to the same host **reuse warm sockets immediately**, completely skipping the DNS, TCP, and TLS handshake setup phase.

> 🚨 **The Golden Rule**: Network clients (`OkHttpClient` / `HttpClient`) MUST be instantiated as **Singletons** via Dependency Injection (Hilt/Dagger) so that all repositories share a single `ConnectionPool`.

---

## 🏗️ Clean Architecture & MVI Overview

This project strictly follows **Clean Architecture** principles separated into domain, data, and presentation layers, combined with **MVI** for predictable unidirectional data flow.

```mermaid
graph TD
    subgraph Presentation Layer (MVI)
        UI[NetworkScreen.kt] -->|NetworkUiIntent| VM[NetworkViewModel.kt]
        VM -->|NetworkUiState| UI
        VM -->|NetworkUiEffect| UI
    end

    subgraph Domain Layer (Clean Architecture)
        VM --> FetchPostsUseCase[FetchPostsUseCase.kt]
        VM --> RunBenchmarkUseCase[RunBenchmarkUseCase.kt]
        FetchPostsUseCase --> NetworkRepository[NetworkRepository.kt]
        RunBenchmarkUseCase --> NetworkRepository
    end

    subgraph Data Layer
        NetworkRepositoryImpl[NetworkRepositoryImpl.kt] -.->|Implements| NetworkRepository
        NetworkRepositoryImpl --> ConnectionPoolManager[ConnectionPoolManager.kt]
        NetworkRepositoryImpl --> OkHttpEventListener[OkHttpEventListener.kt]
        ConnectionPoolManager --> RetrofitApiService[RetrofitApiService.kt]
        ConnectionPoolManager --> KtorNetworkClient[KtorNetworkClient.kt]
    end

    subgraph Dependency Injection (Hilt)
        NetworkModule[NetworkModule.kt] --> ConnectionPoolManager
        RepositoryModule[RepositoryModule.kt] --> NetworkRepositoryImpl
        UseCaseModule[UseCaseModule.kt] --> FetchPostsUseCase
    end
```

---

## ✨ Features

- ⚙️ **Multi-Engine Support**:
  - **Retrofit + OkHttp**: Custom `ConnectionPool(maxIdleConnections = 10, keepAliveDuration = 5, TimeUnit.MINUTES)`.
  - **Ktor + CIO Engine**: Coroutine-based I/O engine connection pool (`maxConnectionsPerRoute = 20`, `keepAliveTime = 300,000ms`).
  - **Ktor + OkHttp Engine**: Ktor client delegating connection pooling to OkHttp.
- 🧪 **Interactive Latency & Socket Reuse Benchmarking**:
  - **Pooled Singleton Mode**: Reuses warm sockets across requests.
  - **Unpooled / New Instance Mode**: Anti-pattern comparison creating new client & pool per request.
  - **Single Request, 5x Sequential, & 5x Parallel Benchmarks**.
- 📊 **Real-time Metrics Dashboard**:
  - Live inspection of DNS lookup time, TCP/TLS connect time, response time, and total time saved.
  - Visual badges indicating `REUSED SOCKET` vs `NEW HANDSHAKE`.
- 💉 **Hilt Dependency Injection**: Singletons for `ConnectionPool`, `OkHttpClient`, `RetrofitApiService`, Ktor clients, repositories, and use cases.
- 🎨 **Modern Jetpack Compose UI**: Material 3 dashboard, latency metrics summary, request logs, and interactive architectural guide dialog.

---

## 💻 Code Highlights

### Hilt Singleton Network Module Configuration

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://jsonplaceholder.typicode.com/"

    @Provides
    @Singleton
    fun provideConnectionPool(): ConnectionPool {
        return ConnectionPool(
            maxIdleConnections = 10,     // Max idle connections retained in pool
            keepAliveDuration = 5,       // Time to keep idle sockets alive
            timeUnit = TimeUnit.MINUTES
        )
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(connectionPool: ConnectionPool): OkHttpClient {
        return OkHttpClient.Builder()
            .connectionPool(connectionPool) // Attach the shared Singleton pool
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofitApiService(okHttpClient: OkHttpClient): RetrofitApiService {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RetrofitApiService::class.java)
    }
}
```

---

## 📊 Benchmark Comparison Results

| Metric | Pooled Singleton (Recommended) | Unpooled New Client (Anti-pattern) |
| :--- | :--- | :--- |
| **Request #1 Latency** | ~250 - 380 ms *(DNS + TCP + TLS + HTTP)* | ~250 - 380 ms *(Full Handshake)* |
| **Requests #2..5 Latency** | **~40 - 75 ms** *(Reused Socket!)* ⚡ | ~250 - 380 ms *(Full Handshake Every Call)* 🐢 |
| **Sockets Reused** | **4 / 5 Connections** | 0 / 5 Connections |
| **Time Saved (5 reqs)** | **~800 - 1,200 ms Saved** | 0 ms |

---

## 🛠️ Project Structure

```text
com.example.pool
├── data
│   ├── network
│   │   ├── ConnectionPoolManager.kt    # Singleton pool & client configurations
│   │   ├── OkHttpEventListener.kt      # DNS/TCP/TLS & socket reuse listener
│   │   ├── ktor/KtorNetworkClient.kt   # Ktor request executor
│   │   └── retrofit/RetrofitApiService # Retrofit API interface
│   └── repository
│       └── NetworkRepositoryImpl.kt    # Benchmark & metrics calculations
├── di
│   ├── NetworkModule.kt                # Hilt Network bindings
│   ├── RepositoryModule.kt             # Hilt Repository bindings
│   └── UseCaseModule.kt                # Hilt UseCase bindings
├── domain
│   ├── model                           # Post, NetworkMetrics, ClientEngine, PoolingMode
│   ├── repository/NetworkRepository.kt # Repository Contract
│   └── usecase                         # FetchPostsUseCase, RunBenchmarkUseCase
├── presentation
│   ├── mvi                             # NetworkUiState, NetworkUiIntent, NetworkUiEffect
│   ├── ui/NetworkScreen.kt             # Jetpack Compose Dashboard UI
│   └── NetworkViewModel.kt             # HiltViewModel handling MVI flows
└── PoolApplication.kt                  # @HiltAndroidApp entry point
```

---

## 🛠️ Tech Stack & Libraries

- **Language**: Kotlin 2.2.10
- **UI Framework**: Jetpack Compose (Material 3, Material Icons Extended)
- **Dependency Injection**: Hilt 2.51.1 (`@HiltAndroidApp`, `@HiltViewModel`, `@AndroidEntryPoint`)
- **Networking**:
  - Retrofit 2.11.0 + Gson Converter
  - OkHttp 4.12.0 + Logging Interceptor
  - Ktor Client 3.1.1 (CIO & OkHttp Engines + ContentNegotiation + kotlinx.serialization)
- **Architecture**: Clean Architecture + MVI (StateFlow & Channel)
- **Testing**: JUnit 4 & `kotlinx-coroutines-test`

---

## 🚦 Getting Started & Testing

### Prerequisites
- Android Studio Ladybug or newer
- JDK 11 / JDK 17
- Android SDK 37 (Min SDK 24)

### Building the Project
```bash
./gradlew app:assembleDebug
```

### Running Unit Tests
```bash
./gradlew app:testDebugUnitTest
```

---

## 📚 References & Further Reading

- [Master HTTP Connection Pooling in Android](https://medium.com/@ahmed.aly) by Ahmed Aly
- [Square OkHttp Documentation: ConnectionPool](https://square.github.io/okhttp/4x/okhttp/okhttp3/-connection-pool/)
- [Ktor Client Engines Guide](https://ktor.io/docs/client-engines.html)
- [Dagger / Hilt Singleton Scoping](https://developer.android.com/training/dependency-injection/hilt-android)

---

## 📄 License
This project is open-source and available under the [MIT License](LICENSE).
