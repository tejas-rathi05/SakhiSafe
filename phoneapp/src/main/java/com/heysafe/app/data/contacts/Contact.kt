package com.heysafe.app.data.contacts

data class Contact(
    val id: String = "",
    val name: String,
    val phone: String,           // E.164: "+91XXXXXXXXXX"
    val relationship: String,
    val group: ContactGroup,
)

enum class ContactGroup {
    FAMILY, FRIENDS;

    fun firestoreValue(): String = name.lowercase()

    companion object {
        fun fromFirestore(s: String?): ContactGroup =
            if (s.equals("friends", ignoreCase = true)) FRIENDS else FAMILY
    }
}
