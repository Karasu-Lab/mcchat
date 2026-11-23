package com.karasu256.mcchatnative.ui.drawer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.karasu256.mcchatnative.data.ClientRepository
import com.karasu256.mcchatnative.data.ConnectionManager
import com.karasu256.mcchatnative.data.ThreadRepository
import com.karasu256.mcchatnative.model.ChatThread
import com.karasu256.mcchatnative.model.DrawerSection
import com.karasu256.mcchatnative.model.DrawerUser
import com.karasu256.mcchatnative.model.ThreadCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DrawerUiState(
    val sections: List<DrawerSection> = emptyList(),
    val user: DrawerUser? = null,
    val selectedThreadId: String? = null,
)

class DrawerViewModel(application: Application) : AndroidViewModel(application) {
    private val threadRepository = ThreadRepository(application)

    private val _uiState = MutableStateFlow(DrawerUiState())
    val uiState: StateFlow<DrawerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val user = ClientRepository.loadUser()
            _uiState.update { it.copy(user = user) }
        }
        viewModelScope.launch {
            ConnectionManager.clients.collect { threadRepository.syncWithClients(it) }
        }
        viewModelScope.launch {
            threadRepository.threads.collect { threads ->
                _uiState.update { it.copy(sections = threads.toSections()) }
            }
        }
    }

    fun onClientRemoved(clientId: String) {
        viewModelScope.launch { threadRepository.removeClient(clientId) }
    }

    fun onThreadSelected(threadId: String) = _uiState.update { it.copy(selectedThreadId = threadId) }

    fun onPinToggle(thread: ChatThread) {
        val category = if (thread.isPinned) ThreadCategory.Recent else ThreadCategory.Pinned
        viewModelScope.launch { threadRepository.setCategory(thread.id, category) }
    }
}

private fun List<ChatThread>.toSections(): List<DrawerSection> {
    val byCategory = groupBy { it.category }
    return ThreadCategory.entries.mapNotNull { category ->
        byCategory[category]?.let { DrawerSection(category, it.sortedByDescending(ChatThread::createdAtMillis)) }
    }
}
