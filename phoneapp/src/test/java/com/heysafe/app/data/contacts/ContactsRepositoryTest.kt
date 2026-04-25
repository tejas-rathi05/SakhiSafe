package com.heysafe.app.data.contacts

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContactsRepositoryTest {
    @Test
    fun `add rejects non-E164 phone`() = runTest {
        val backend = mockk<ContactsBackend>(relaxed = true)
        val repo = ContactsRepository(backend)
        val r = repo.add(
            "uid1",
            Contact(name = "M", phone = "9876543210", relationship = "Mom", group = ContactGroup.FAMILY)
        )
        assertEquals(false, r.isSuccess)
    }

    @Test
    fun `add accepts E164 phone and forwards to backend`() = runTest {
        val backend = mockk<ContactsBackend>(relaxed = true)
        coEvery { backend.create(any(), any()) } returns "newId"
        val repo = ContactsRepository(backend)
        val c = Contact(name = "M", phone = "+919876543210", relationship = "Mom", group = ContactGroup.FAMILY)
        val r = repo.add("uid1", c)
        assertEquals(true, r.isSuccess)
        assertEquals("newId", r.getOrNull())
        coVerify { backend.create("uid1", c) }
    }

    @Test
    fun `add rejects empty name`() = runTest {
        val backend = mockk<ContactsBackend>(relaxed = true)
        val repo = ContactsRepository(backend)
        val r = repo.add(
            "uid1",
            Contact(name = "  ", phone = "+919876543210", relationship = "Mom", group = ContactGroup.FAMILY)
        )
        assertEquals(false, r.isSuccess)
    }

    @Test
    fun `ContactGroup roundtrips through firestoreValue`() {
        assertEquals(ContactGroup.FAMILY, ContactGroup.fromFirestore("family"))
        assertEquals(ContactGroup.FRIENDS, ContactGroup.fromFirestore("friends"))
        assertEquals(ContactGroup.FAMILY, ContactGroup.fromFirestore(null))
        assertEquals(ContactGroup.FAMILY, ContactGroup.fromFirestore("garbage"))
        assertEquals("family", ContactGroup.FAMILY.firestoreValue())
        assertEquals("friends", ContactGroup.FRIENDS.firestoreValue())
    }
}
