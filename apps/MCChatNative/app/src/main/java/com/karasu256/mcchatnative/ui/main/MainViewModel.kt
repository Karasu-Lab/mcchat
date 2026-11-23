package com.karasu256.mcchatnative.ui.main

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class BackLayer { Drawer, Search }

data class MainUiState(
    val isSearching: Boolean = false,
    val isDrawerOpen: Boolean = false,
) {
    val topBackLayer: BackLayer?
        get() = when {
            isDrawerOpen -> BackLayer.Drawer
            isSearching -> BackLayer.Search
            else -> null
        }
}

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun onSearchStart() = _uiState.update { it.copy(isSearching = true) }

    fun onSearchClose() = _uiState.update { it.copy(isSearching = false) }

    fun onDrawerOpen() = _uiState.update { it.copy(isDrawerOpen = true) }

    fun onDrawerClose() = _uiState.update { it.copy(isDrawerOpen = false) }

    fun onDrawerSettled(isOpen: Boolean) = _uiState.update { it.copy(isDrawerOpen = isOpen) }

    fun onBack() {
        when (_uiState.value.topBackLayer) {
            BackLayer.Drawer -> onDrawerClose()
            BackLayer.Search -> onSearchClose()
            null -> Unit
        }
    }
}
