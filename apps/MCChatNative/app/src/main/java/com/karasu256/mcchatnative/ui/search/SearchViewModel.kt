package com.karasu256.mcchatnative.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karasu256.mcchatnative.data.ConnectionManager
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
            ConnectionManager.clients.collect { list ->
                clients = list
                _uiState.update { it.copy(isLoading = false, results = match(it.query)) }
            }
        }
    }

    suspend fun addConnection(input: String): Boolean {
        val trimmed = input.trim()
        val separator = trimmed.lastIndexOf(':')
        val port = if (separator > 0) trimmed.substring(separator + 1).toIntOrNull() else null
        val host = if (port != null) trimmed.substring(0, separator) else trimmed
        if (host.isEmpty() || !ConnectionManager.probe(host, port ?: DefaultPort)) return false
        ConnectionManager.add(host, port ?: DefaultPort)
        return true
    }

    fun removeConnection(clientId: String) {
        viewModelScope.launch { ConnectionManager.remove(clientId) }
    }

    fun search(q: String) {
        _uiState.update { it.copy(query = q, results = match(q)) }
    }

    private fun match(q: String): List<Client> {
        val terms = q.split(WhitespaceRegex).filter { it.isNotEmpty() }
        if (terms.isEmpty()) return clients
        return clients.filter { client ->
            terms.any { term ->
                client.name.contains(term, ignoreCase = true) || client.address.contains(
                    term,
                    ignoreCase = true
                )
            }
        }
    }

    private companion object {
        const val DefaultPort = 25570
        val WhitespaceRegex = Regex("\\s+")
    }
}
