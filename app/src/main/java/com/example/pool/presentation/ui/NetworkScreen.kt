package com.example.pool.presentation.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pool.domain.model.BenchmarkSummary
import com.example.pool.domain.model.ClientEngine
import com.example.pool.domain.model.NetworkMetrics
import com.example.pool.domain.model.PoolingMode
import com.example.pool.domain.model.Post
import com.example.pool.presentation.NetworkViewModel
import com.example.pool.presentation.mvi.NetworkUiEffect
import com.example.pool.presentation.mvi.NetworkUiIntent
import com.example.pool.presentation.mvi.NetworkUiState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(
    viewModel: NetworkViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                is NetworkUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is NetworkUiEffect.ScrollToBottom -> {
                    scope.launch {
                        if (state.metricsLog.isNotEmpty()) {
                            listState.animateScrollToItem(0)
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "HTTP Connection Pooling",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Clean Architecture + MVI • Retrofit & Ktor",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onIntent(NetworkUiIntent.ToggleExplanationDialog(true)) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Architecture & Article Info",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Concept Banner Card
            item {
                ConceptBannerCard(
                    onInfoClick = { viewModel.onIntent(NetworkUiIntent.ToggleExplanationDialog(true)) }
                )
            }

            // 2. Engine Selection (Retrofit vs Ktor)
            item {
                EngineSelectionSection(
                    selectedEngine = state.selectedEngine,
                    onEngineSelected = { viewModel.onIntent(NetworkUiIntent.SelectEngine(it)) }
                )
            }

            // 3. Pooling Mode Toggle (Singleton vs New Instance)
            item {
                PoolingModeSelectionSection(
                    selectedMode = state.selectedPoolingMode,
                    onModeSelected = { viewModel.onIntent(NetworkUiIntent.SelectPoolingMode(it)) }
                )
            }

            // 4. Action Buttons
            item {
                ActionButtonsSection(
                    isProgress = state.isRequestInProgress,
                    onSingleClick = { viewModel.onIntent(NetworkUiIntent.ExecuteSingleRequest) },
                    onBatchSequentialClick = { viewModel.onIntent(NetworkUiIntent.ExecuteBatchBenchmark(count = 5, parallel = false)) },
                    onBatchParallelClick = { viewModel.onIntent(NetworkUiIntent.ExecuteBatchBenchmark(count = 5, parallel = true)) },
                    onClearClick = { viewModel.onIntent(NetworkUiIntent.ClearMetricsLog) }
                )
            }

            // 5. Benchmark Dashboard
            state.benchmarkSummary?.let { summary ->
                item {
                    BenchmarkSummaryCard(summary = summary)
                }
            }

            // 6. Data Preview Section (if posts are loaded)
            if (state.posts.isNotEmpty()) {
                item {
                    FetchedDataCard(posts = state.posts)
                }
            }

            // 7. Request History Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Request History Log (${state.metricsLog.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (state.metricsLog.isNotEmpty()) {
                        TextButton(onClick = { viewModel.onIntent(NetworkUiIntent.ClearMetricsLog) }) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // 8. Metrics Log Stream
            if (state.metricsLog.isEmpty()) {
                item {
                    EmptyLogPlaceholder()
                }
            } else {
                items(state.metricsLog, key = { it.id }) { metrics ->
                    MetricsLogItemCard(metrics = metrics)
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (state.showExplanationDialog) {
        ExplanationDialog(
            onDismiss = { viewModel.onIntent(NetworkUiIntent.ToggleExplanationDialog(false)) }
        )
    }
}

@Composable
fun ConceptBannerCard(onInfoClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ElectricBolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Why Connection Pooling?",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Reusing active TCP/TLS connections skips DNS lookup & handshakes, saving hundreds of milliseconds per network request.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onInfoClick) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Read Article Summary",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun EngineSelectionSection(
    selectedEngine: ClientEngine,
    onEngineSelected: (ClientEngine) -> Unit
) {
    Column {
        Text(
            text = "Select Network Client Engine",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ClientEngine.entries.forEach { engine ->
                val isSelected = (engine == selectedEngine)
                FilterChip(
                    selected = isSelected,
                    onClick = { onEngineSelected(engine) },
                    label = {
                        Text(
                            text = when (engine) {
                                ClientEngine.RETROFIT_OKHTTP -> "Retrofit"
                                ClientEngine.KTOR_CIO -> "Ktor (CIO)"
                                ClientEngine.KTOR_OKHTTP -> "Ktor (OkHttp)"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun PoolingModeSelectionSection(
    selectedMode: PoolingMode,
    onModeSelected: (PoolingMode) -> Unit
) {
    Column {
        Text(
            text = "Connection Pooling Configuration",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Pooled Singleton Option
            ModeCard(
                title = "Pooled Singleton",
                subtitle = "Reuses Sockets (Fast)",
                badgeText = "RECOMMENDED",
                isSelected = (selectedMode == PoolingMode.POOLED_SINGLETON),
                badgeColor = Color(0xFF2E7D32),
                onClick = { onModeSelected(PoolingMode.POOLED_SINGLETON) },
                modifier = Modifier.weight(1f)
            )

            // Unpooled Option
            ModeCard(
                title = "New Client Each Req",
                subtitle = "Handshake Each Time",
                badgeText = "ANTI-PATTERN",
                isSelected = (selectedMode == PoolingMode.UNPOOLED_NEW_INSTANCE),
                badgeColor = Color(0xFFC62828),
                onClick = { onModeSelected(PoolingMode.UNPOOLED_NEW_INSTANCE) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ModeCard(
    title: String,
    subtitle: String,
    badgeText: String,
    isSelected: Boolean,
    badgeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface

    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(borderColor),
            width = if (isSelected) 2.dp else 1.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Surface(
                color = badgeColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 9.sp),
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ActionButtonsSection(
    isProgress: Boolean,
    onSingleClick: () -> Unit,
    onBatchSequentialClick: () -> Unit,
    onBatchParallelClick: () -> Unit,
    onClearClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSingleClick,
                enabled = !isProgress,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("Single Request", fontSize = 12.sp)
            }

            Button(
                onClick = onBatchSequentialClick,
                enabled = !isProgress,
                modifier = Modifier.weight(1.2f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Benchmark 5x", fontSize = 12.sp)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBatchParallelClick,
                enabled = !isProgress,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.ElectricBolt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Parallel 5x", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = onClearClick,
                enabled = !isProgress,
                modifier = Modifier.weight(0.8f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun BenchmarkSummaryCard(summary: BenchmarkSummary) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Benchmark Results Summary",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                Surface(
                    color = if (summary.reusedConnectionsCount > 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (summary.reusedConnectionsCount > 0) "SOCKET REUSE ACTIVE" else "NO POOLING",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricStatItem(
                    label = "Avg Latency",
                    value = "${summary.avgDurationMs} ms",
                    icon = Icons.Default.Timer,
                    modifier = Modifier.weight(1f)
                )

                MetricStatItem(
                    label = "1st Req Latency",
                    value = "${summary.firstRequestDurationMs} ms",
                    icon = Icons.Default.Speed,
                    modifier = Modifier.weight(1f)
                )

                MetricStatItem(
                    label = "Reused Sockets",
                    value = "${summary.reusedConnectionsCount} / ${summary.totalRequests}",
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
            }

            if (summary.estimatedTimeSavedMs > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Estimated time saved by connection pooling: ~${summary.estimatedTimeSavedMs} ms!",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFF1B5E20)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricStatItem(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun MetricsLogItemCard(metrics: NetworkMetrics) {
    val isReused = metrics.isConnectionReused
    val badgeColor = if (isReused) Color(0xFF2E7D32) else Color(0xFFE65100)
    val badgeText = if (isReused) "REUSED SOCKET" else "NEW HANDSHAKE"

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#${metrics.requestIndex}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = badgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${metrics.engine.title} • ${metrics.poolingMode.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (metrics.dnsDurationMs > 0 || metrics.connectDurationMs > 0) {
                    Text(
                        text = "DNS: ${metrics.dnsDurationMs}ms | Connect: ${metrics.connectDurationMs}ms",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${metrics.durationMs} ms",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isReused) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = if (metrics.statusCode == 200) "HTTP 200 OK" else "Err: ${metrics.errorMessage}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (metrics.statusCode == 200) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }
        }
    }
}

@Composable
fun FetchedDataCard(posts: List<Post>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Fetched API Sample Data (${posts.size} posts)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "JSON OK",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            posts.take(2).forEach { post ->
                Text(
                    text = "• Post #${post.id}: ${post.title}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = post.body,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
fun EmptyLogPlaceholder() {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No metrics recorded yet",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Tap 'Single Request' or 'Benchmark 5x' to measure latencies and connection pooling socket reuse.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun ExplanationDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Master HTTP Connection Pooling",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Key Takeaways from Ahmed Aly's Guide:",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "1. Overhead of Every Request:\nWithout pooling, every request performs:\n• DNS Lookup (~50ms)\n• TCP Handshake (~100ms)\n• TLS Handshake (~150ms)\nAdding up to 300ms+ overhead per call!",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "2. How Connection Pooling Works:\nActive TCP sockets are kept open in an 'idle' state in a shared ConnectionPool (e.g. 5 mins). Subsequent requests reuse the socket immediately.",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "3. The Golden Rule: Singleton Client\nCreating a new OkHttpClient or Ktor HttpClient per request creates a new connection pool every time, completely breaking socket reuse!",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFC62828)
                )

                Text(
                    text = "4. Solution: Dependency Injection\nUse Hilt, Dagger, or Singleton provider objects to inject single instances of OkHttpClient / Ktor HttpClient across your repositories.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got It!")
            }
        }
    )
}
