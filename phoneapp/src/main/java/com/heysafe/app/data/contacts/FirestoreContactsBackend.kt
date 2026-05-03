package com.heysafe.app.data.contacts

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreContactsBackend(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : ContactsBackend {
    private fun col(uid: String) = db.collection("users").document(uid).collection("contacts")

    override suspend fun create(uid: String, contact: Contact): String {
        val data = mapOf(
            "name" to contact.name,
            "phone" to contact.phone,
            "relationship" to contact.relationship,
            "group" to contact.group.firestoreValue(),
            "createdAt" to FieldValue.serverTimestamp(),
        )
        val ref = col(uid).add(data).await()
        return ref.id
    }

    override suspend fun delete(uid: String, contactId: String) {
        col(uid).document(contactId).delete().await()
    }

    override fun observe(uid: String): Flow<List<Contact>> =
        col(uid).snapshots().map { snap ->
            snap.documents.map { d ->
                Contact(
                    id = d.id,
                    name = d.getString("name").orEmpty(),
                    phone = d.getString("phone").orEmpty(),
                    relationship = d.getString("relationship").orEmpty(),
                    group = ContactGroup.fromFirestore(d.getString("group")),
                )
            }
        }
}
