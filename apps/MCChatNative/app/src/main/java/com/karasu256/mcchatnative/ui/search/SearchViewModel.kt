package com.karasu256.mcchatnative.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karasu256.mcchatnative.data.ClientRepository
import com.karasu256.mcchatnative.model.Client
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val results: List<Client> = emptyList(),
)

class SearchViewModel : ViewModel() {
    private var clients: List<Client> = emptyList()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            clients = ClientRepository.loadClients()
            _uiState.update { it.copy(isLoading = false, results = match(it.query)) }
        }
    }

    fun search(q: String) {
        _uiState.update { it.copy(query = q, results = match(q)) }
    }

    private fun match(q: String): List<Client> {
        val terms = q.split(WhitespaceRegex).filter { it.isNotEmpty() }
        if (terms.isEmpty()) return clients
        return clients.filter { client ->
            terms.any { term ->
                client.name.contains(term, ignoreCase = true) || client.address.contains(term, ignoreCase = true)
            }
        }
    }

    private companion object {
        val WhitespaceRegex = Regex("\\s+")
    }
}
