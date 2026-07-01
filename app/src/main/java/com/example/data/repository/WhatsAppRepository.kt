package com.example.data.repository

import com.example.data.db.WhatsAppDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class WhatsAppRepository(private val dao: WhatsAppDao) {

    val allContacts: Flow<List<Contact>> = dao.getAllContacts()
    val allChats: Flow<List<Chat>> = dao.getAllChats()
    val allStatusUpdates: Flow<List<StatusUpdate>> = dao.getAllStatusUpdates()
    val allCallLogs: Flow<List<CallLog>> = dao.getAllCallLogs()

    suspend fun getContactById(id: Int): Contact? = dao.getContactById(id)

    suspend fun insertContact(contact: Contact): Long = dao.insertContact(contact)

    suspend fun insertChat(chat: Chat): Long = dao.insertChat(chat)

    suspend fun updateChat(chat: Chat) = dao.updateChat(chat)

    suspend fun getChatByContactId(contactId: Int): Chat? = dao.getChatByContactId(contactId)

    suspend fun getChatById(id: Int): Chat? = dao.getChatById(id)

    suspend fun deleteChatById(chatId: Int) = dao.deleteChatById(chatId)

    fun getMessagesForChat(chatId: Int): Flow<List<Message>> = dao.getMessagesByChatId(chatId)

    suspend fun getMessagesForChatOneShot(chatId: Int): List<Message> = dao.getMessagesByChatIdOneShot(chatId)

    suspend fun insertMessage(message: Message): Long = dao.insertMessage(message)

    suspend fun updateMessage(message: Message) = dao.updateMessage(message)

    suspend fun deleteMessagesByChatId(chatId: Int) = dao.deleteMessagesByChatId(chatId)

    suspend fun insertStatusUpdate(status: StatusUpdate): Long = dao.insertStatusUpdate(status)

    suspend fun updateStatusUpdate(status: StatusUpdate) = dao.updateStatusUpdate(status)

    suspend fun insertCallLog(call: CallLog): Long = dao.insertCallLog(call)

    suspend fun deleteCallLogById(id: Int) = dao.deleteCallLogById(id)
    
    suspend fun clearCallLogs() = dao.clearCallLogs()
}
