package com.heysafe.app.data.contacts

import kotlinx.coroutines.flow.Flow

interface ContactsBackend {
    suspend fun create(uid: String, contact: Contact): String
    suspend fun delete(uid: String, contactId: String)
    fun observe(uid: String): Flow<List<Contact>>
}

private val E164 = Regex("^\\+[1-9]\\d{1,14}$")

class ContactsRepository(private val backend: ContactsBackend) {
    suspend fun add(uid: String, contact: Contact): Result<String> {
        if (contact.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Name is required"))
        }
        if (!E164.matches(contact.phone)) {
            return Result.failure(IllegalArgumentException("Phone must be E.164 (+countrycode...)"))
        }
        return runCatching { backend.create(uid, contact) }
    }

    suspend fun delete(uid: String, id: String): Result<Unit> = runCatching { backend.delete(uid, id) }

    fun observe(uid: String): Flow<List<Contact>> = backend.observe(uid)
}
