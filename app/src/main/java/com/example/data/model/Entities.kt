package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val avatar: String, // Resource identifier name or custom string
    val statusMessage: String,
    val isGroup: Boolean = false,
    val isAi: Boolean = false
)

@Entity(tableName = "chats")
data class Chat(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val contactId: Int, // Refers to contacts.id, or 0 if custom group
    val name: String,
    val avatar: String, // Cached avatar from Contact
    val lastMessage: String,
    val lastTimestamp: Long,
    val isGroup: Boolean = false,
    val unreadCount: Int = 0,
    val isAi: Boolean = false
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chatId: Int, // Refers to chats.id
    val senderId: Int, // Refers to contact ID, or -1 for user, 0 for system/AI
    val senderName: String,
    val text: String,
    val timestamp: Long,
    val isOutgoing: Boolean,
    val isRead: Boolean = false,
    val isAudio: Boolean = false,
    val audioDurationSec: Int = 0,
    val isImage: Boolean = false,
    val imageUrl: String? = null,
    val quotedMessageText: String? = null
)

@Entity(tableName = "status_updates")
data class StatusUpdate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val contactId: Int,
    val contactName: String,
    val contactAvatar: String,
    val timestamp: Long,
    val textContent: String? = null,
    val imageUrl: String? = null,
    val isViewed: Boolean = false
)

@Entity(tableName = "call_logs")
data class CallLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val contactId: Int,
    val contactName: String,
    val contactAvatar: String,
    val timestamp: Long,
    val isVideo: Boolean,
    val isIncoming: Boolean,
    val isMissed: Boolean
)
