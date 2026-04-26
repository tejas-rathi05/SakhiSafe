package com.heysafe.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heysafe.app.data.auth.AuthRepository
import com.heysafe.app.data.contacts.Contact
import com.heysafe.app.data.contacts.ContactGroup
import com.heysafe.app.data.contacts.ContactsRepository
import com.heysafe.app.domain.alert.AlertOrchestrator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val auth: AuthRepository,
    private val contacts: ContactsRepository,
    private val orchestrator: AlertOrchestrator,
) : ViewModel() {

    val displayName: String =
        auth.currentUser()?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "there"

    private val _selectedGroup = MutableStateFlow(ContactGroup.FAMILY)
    val selectedGroup: StateFlow<ContactGroup> = _selectedGroup
    fun setGroup(g: ContactGroup) { _selectedGroup.value = g }

    val groupedContacts: StateFlow<List<Contact>> =
        combine(
            (auth.currentUser()?.uid?.let { contacts.observe(it) } ?: flowOf(emptyList())),
            _selectedGroup,
        ) { all, g -> all.filter { it.group == g } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun triggerManualSos() {
        viewModelScope.launch { orchestrator.onManualAlert() }
    }
}
