package com.example.pool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pool.presentation.NetworkViewModel
import com.example.pool.presentation.ui.NetworkScreen
import com.example.pool.ui.theme.ConnectionPoolTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ConnectionPoolTheme {
                val networkViewModel: NetworkViewModel = hiltViewModel()
                NetworkScreen(viewModel = networkViewModel)
            }
        }
    }
}
