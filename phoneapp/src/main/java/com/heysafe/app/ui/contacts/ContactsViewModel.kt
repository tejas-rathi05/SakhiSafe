package com.heysafe.app.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heysafe.app.data.auth.AuthRepository
import com.heysafe.app.data.contacts.Contact
import com.heysafe.app.data.contacts.ContactsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactsViewModel(
    private val auth: AuthRepository,
    private val contacts: ContactsRepository,
) : ViewModel() {
    private val uid: String? = auth.currentUser()?.uid

    val items: StateFlow<List<Contact>> =
        (uid?.let { contacts.observe(it) } ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun add(c: Contact): Result<String> {
        val u = uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return contacts.add(u, c)
    }

    fun delete(id: String) {
        val u = uid ?: return
        viewModelScope.launch { contacts.delete(u, id) }
    }
}
