package com.example.pool.presentation.mvi

sealed interface NetworkUiEffect {
    data class ShowToast(val message: String) : NetworkUiEffect
    data object ScrollToBottom : NetworkUiEffect
}
